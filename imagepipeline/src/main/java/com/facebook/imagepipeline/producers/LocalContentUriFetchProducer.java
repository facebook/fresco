/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imageutils.JfifUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

/**
 * Represents a local content Uri fetch producer.
 */
public class LocalContentUriFetchProducer extends LocalFetchProducer {

  private static final Class<?> TAG = LocalContentUriFetchProducer.class;

  @VisibleForTesting static final String PRODUCER_NAME = "LocalContentUriFetchProducer";
  private static final String DISPLAY_PHOTO_PATH =
      Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "display_photo").getPath();
  private static final String[] PROJECTION = new String[] {
      MediaStore.Images.Media._ID,
      MediaStore.Images.ImageColumns.DATA
  };
  private static final String[] THUMBNAIL_PROJECTION = new String[] {
      MediaStore.Images.Thumbnails.DATA
  };

  private static final Rect MINI_THUMBNAIL_DIMENSIONS = new Rect(0, 0, 512, 384);
  private static final Rect MICRO_THUMBNAIL_DIMENSIONS = new Rect(0, 0, 96, 96);
  private static final float ACCEPTABLE_REQUESTED_TO_ACTUAL_SIZE_RATIO = 4.0f/3;

  private static final int NO_THUMBNAIL = 0;

  private final ContentResolver mContentResolver;

  public LocalContentUriFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      boolean downsampleEnabled,
      ContentResolver contentResolver) {
    super(executor, pooledByteBufferFactory, downsampleEnabled);
    mContentResolver = contentResolver;
  }

  @Override
  protected EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException {
    Uri uri = imageRequest.getSourceUri();
    if (isContactUri(uri)) {
      // If a Contact URI is provided, use the special helper to open that contact's photo.
      return getByteBufferBackedEncodedImage(
          ContactsContract.Contacts.openContactPhotoInputStream(mContentResolver, uri),
          EncodedImage.UNKNOWN_STREAM_SIZE);
    }

    if (isCameraUri(uri)) {
      EncodedImage cameraImage = getCameraImage(uri, imageRequest.getResizeOptions());
      if (cameraImage != null) {
        return cameraImage;
      }
    }

    return getByteBufferBackedEncodedImage(
        mContentResolver.openInputStream(uri),
        EncodedImage.UNKNOWN_STREAM_SIZE);
  }

  /**
   * Checks if the given URI is a general Contact URI, and not a specific display photo.
   * @param uri the URI to check
   * @return true if the uri is a a Contact URI, and is not already specifying a display photo.
   */
  private static boolean isContactUri(Uri uri) {
    return ContactsContract.AUTHORITY.equals(uri.getAuthority()) &&
        !uri.getPath().startsWith(DISPLAY_PHOTO_PATH);
  }

  private static boolean isCameraUri(Uri uri) {
    String uriString = uri.toString();
    return uriString.startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()) ||
        uriString.startsWith(MediaStore.Images.Media.INTERNAL_CONTENT_URI.toString());
  }

  private @Nullable EncodedImage getCameraImage(Uri uri, ResizeOptions resizeOptions) {
    Cursor cursor = mContentResolver.query(uri, PROJECTION, null, null, null);
    if (cursor == null) {
      return null;
    }
    try {
      if (cursor.getCount() == 0) {
        return null;
      }
      cursor.moveToFirst();
      final String pathname =
          cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
      if (resizeOptions != null) {
        int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        EncodedImage thumbnail = getThumbnail(resizeOptions, cursor.getInt(imageIdColumnIndex));
        if (thumbnail != null) {
          thumbnail.setRotationAngle(getRotationAngle(pathname));
          return thumbnail;
        }
      }
      if (pathname != null) {
        return getFileBackedEncodedImage(pathname, getLength(pathname));
      }
    } finally {
      cursor.close();
    }
    return null;
  }

  // Gets the smallest possible thumbnail that is bigger than the requested size in the resize
  // options or null if either the thumbnails are smaller than the requested size or there are no
  // stored thumbnails.
  private EncodedImage getThumbnail(ResizeOptions resizeOptions, int imageId) {
    int thumbnailKind = getThumbnailKind(resizeOptions);
    if (thumbnailKind == NO_THUMBNAIL) {
      return null;
    }
    Cursor thumbnailCursor = null;
    try {
      thumbnailCursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
          mContentResolver,
          imageId,
          thumbnailKind,
          THUMBNAIL_PROJECTION);
      if (thumbnailCursor == null) {
         return null;
      }
      thumbnailCursor.moveToFirst();
      if (thumbnailCursor.getCount() > 0) {
        final String thumbnailUri = thumbnailCursor.getString(
            thumbnailCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
        if (new File(thumbnailUri).exists()) {
          return getFileBackedEncodedImage(thumbnailUri, getLength(thumbnailUri));
        }
      }
    } finally {
      if (thumbnailCursor != null) {
        thumbnailCursor.close();
      }
    }
    return null;
  }

  // Returns the smallest possible thumbnail kind that has an acceptable size (meaning the resize
  // options requested size is smaller than 4/3 its size).
  // We can add a small interval of acceptance over the size of the thumbnail since the quality lost
  // when scaling it to fit a view will not be significant.
  private static int getThumbnailKind(ResizeOptions resizeOptions) {
    if (isThumbnailBigEnough(resizeOptions, MICRO_THUMBNAIL_DIMENSIONS)) {
      return MediaStore.Images.Thumbnails.MICRO_KIND;
    } else if (isThumbnailBigEnough(resizeOptions, MINI_THUMBNAIL_DIMENSIONS)) {
      return MediaStore.Images.Thumbnails.MINI_KIND;
    }
    return NO_THUMBNAIL;
  }

  @VisibleForTesting
  static boolean isThumbnailBigEnough(ResizeOptions resizeOptions, Rect thumbnailDimensions) {
    return resizeOptions.width <=
        thumbnailDimensions.width() * ACCEPTABLE_REQUESTED_TO_ACTUAL_SIZE_RATIO
        && resizeOptions.height <=
        thumbnailDimensions.height() * ACCEPTABLE_REQUESTED_TO_ACTUAL_SIZE_RATIO;
  }

  private static int getLength(String pathname) {
    return pathname == null ? -1 : (int) new File(pathname).length();
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }

  private static int getRotationAngle(String pathname) {
    if (pathname != null) {
      try {
        ExifInterface exif = new ExifInterface(pathname);
        return JfifUtil.getAutoRotateAngleFromOrientation(exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
      } catch (IOException ioe) {
        FLog.e(TAG, ioe, "Unable to retrieve thumbnail rotation for %s", pathname);
      }
    }
    return 0;
  }
}
