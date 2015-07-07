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
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

/**
 * Represents a local content Uri fetch producer.
 */
public class LocalContentUriFetchProducer extends LocalFetchProducer {
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

  private static final int NO_THUMBNAIL = 0;

  private final ContentResolver mContentResolver;

  public LocalContentUriFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      ContentResolver contentResolver) {
    super(executor, pooledByteBufferFactory);
    mContentResolver = contentResolver;
  }

  @Override
  protected EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException {
    Uri uri = imageRequest.getSourceUri();
    if (isContactUri(uri)) {
      // If a Contact URI is provided, use the special helper to open that contact's photo.
      return getByteBufferBackedEncodedImage(
          ContactsContract.Contacts.openContactPhotoInputStream(mContentResolver, uri),
          getLength(imageRequest));
    }

    if (isCameraUri(uri)) {
      final String pathname = getCameraPath(uri, imageRequest.getResizeOptions());
      if (pathname != null) {
        Supplier<FileInputStream> sup = new Supplier<FileInputStream>() {
          @Override
          public FileInputStream get() {
            try {
              return new FileInputStream(pathname);
            } catch (IOException ioe) {
              throw new RuntimeException(ioe);
            }
          }
        };
        return new EncodedImage(sup, getLength(imageRequest));
      }
    }

    return getByteBufferBackedEncodedImage(
        mContentResolver.openInputStream(uri),
        getLength(imageRequest));
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

  private @Nullable String getCameraPath(Uri uri, ResizeOptions resizeOptions) {
    Cursor cursor = mContentResolver.query(uri, PROJECTION, null, null, null);
    if (cursor == null) {
      return null;
    }
    try {
      if (cursor.getCount() == 0) {
        return null;
      }
      cursor.moveToFirst();
      if (resizeOptions != null) {
        int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        String thumbnailUri = getThumbnailPath(resizeOptions, cursor.getInt(imageIdColumnIndex));
        if (thumbnailUri != null) {
          return thumbnailUri;
        }
      }
      return cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
    } finally {
      cursor.close();
    }
  }

  // Gets the path of the smallest possible thumbnail that is bigger than the requested size in the
  // resize options or null if either the thumbnails are smaller than the requested size or there
  // are no stored thumbnails.
  private String getThumbnailPath(ResizeOptions resizeOptions, int imageId) {
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
        String thumbnailUri = thumbnailCursor.getString(
            thumbnailCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
        if (new File(thumbnailUri).exists()) {
          return thumbnailUri;
        }
      }
    } finally {
      if (thumbnailCursor != null) {
        thumbnailCursor.close();
      }
    }
    return null;
  }

  // Returns the smallest possible thumbnail kind bigger than the requested size in the resize
  // options.
  private static int getThumbnailKind(ResizeOptions resizeOptions) {
    if (isSmallerThanThumbnail(resizeOptions, MICRO_THUMBNAIL_DIMENSIONS)) {
      return MediaStore.Images.Thumbnails.MICRO_KIND;
    } else if (isSmallerThanThumbnail(resizeOptions, MINI_THUMBNAIL_DIMENSIONS)) {
      return MediaStore.Images.Thumbnails.MINI_KIND;
    }
    return NO_THUMBNAIL;
  }

  @VisibleForTesting
  static boolean isSmallerThanThumbnail(ResizeOptions resizeOptions, Rect thumbnailDimensions) {
    return resizeOptions.width <= thumbnailDimensions.width()
        && resizeOptions.height <= thumbnailDimensions.height();
  }

  private int getLength(ImageRequest imageRequest) {
    Uri uri = imageRequest.getSourceUri();
    if (isCameraUri(uri)) {
      String pathname = getCameraPath(uri, imageRequest.getResizeOptions());
      return pathname == null ? -1 : (int) new File(pathname).length();
    } else {
      return -1;
    }
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
