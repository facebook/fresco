/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imageutils.JfifUtil;
import com.facebook.infer.annotation.Nullsafe;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/** Represents a local content Uri fetch producer. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LocalContentUriThumbnailFetchProducer extends LocalFetchProducer
    implements ThumbnailProducer<EncodedImage> {

  private static final Class<?> TAG = LocalContentUriThumbnailFetchProducer.class;

  public static final String PRODUCER_NAME = "LocalContentUriThumbnailFetchProducer";

  private static final String[] PROJECTION =
      new String[] {MediaStore.Images.Media._ID, MediaStore.Images.ImageColumns.DATA};
  private static final String[] THUMBNAIL_PROJECTION =
      new String[] {MediaStore.Images.Thumbnails.DATA};

  private static final Rect MINI_THUMBNAIL_DIMENSIONS = new Rect(0, 0, 512, 384);
  private static final Rect MICRO_THUMBNAIL_DIMENSIONS = new Rect(0, 0, 96, 96);

  private static final int NO_THUMBNAIL = 0;

  private final ContentResolver mContentResolver;

  public LocalContentUriThumbnailFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      ContentResolver contentResolver) {
    super(executor, pooledByteBufferFactory);
    mContentResolver = contentResolver;
  }

  @Override
  public boolean canProvideImageForSize(@Nullable ResizeOptions resizeOptions) {
    return ThumbnailSizeChecker.isImageBigEnough(
        MINI_THUMBNAIL_DIMENSIONS.width(), MINI_THUMBNAIL_DIMENSIONS.height(), resizeOptions);
  }

  @Override
  protected @Nullable EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException {
    Uri uri = imageRequest.getSourceUri();

    if (UriUtil.isLocalCameraUri(uri)) {
      return getCameraImage(uri, imageRequest.getResizeOptions());
    }

    return null;
  }

  private @Nullable EncodedImage getCameraImage(Uri uri, @Nullable ResizeOptions resizeOptions)
      throws IOException {
    if (resizeOptions == null) {
      return null;
    }
    @Nullable Cursor cursor = mContentResolver.query(uri, PROJECTION, null, null, null);
    if (cursor == null) {
      return null;
    }
    try {
      if (cursor.moveToFirst()) {
        final int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        final EncodedImage thumbnail =
            getThumbnail(resizeOptions, cursor.getLong(imageIdColumnIndex));
        if (thumbnail != null) {
          final String pathname =
              cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
          thumbnail.setRotationAngle(getRotationAngle(pathname));
          return thumbnail;
        }
      }
    } finally {
      cursor.close();
    }
    return null;
  }

  // Gets the smallest possible thumbnail that is bigger than the requested size in the resize
  // options or null if either the thumbnails are smaller than the requested size or there are no
  // stored thumbnails.
  private @Nullable EncodedImage getThumbnail(ResizeOptions resizeOptions, long imageId)
      throws IOException {
    int thumbnailKind = getThumbnailKind(resizeOptions);
    if (thumbnailKind == NO_THUMBNAIL) {
      return null;
    }
    @Nullable
    Cursor thumbnailCursor =
        MediaStore.Images.Thumbnails.queryMiniThumbnail(
            mContentResolver, imageId, thumbnailKind, THUMBNAIL_PROJECTION);
    if (thumbnailCursor == null) {
      return null;
    }
    try {
      if (thumbnailCursor.moveToFirst()) {
        final String thumbnailUri =
            Preconditions.checkNotNull(
                thumbnailCursor.getString(
                    thumbnailCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA)));
        if (new File(thumbnailUri).exists()) {
          return getEncodedImage(new FileInputStream(thumbnailUri), getLength(thumbnailUri));
        }
      }
    } finally {
      thumbnailCursor.close();
    }
    return null;
  }

  // Returns the smallest possible thumbnail kind that has an acceptable size (meaning the resize
  // options requested size is smaller than 4/3 its size).
  // We can add a small interval of acceptance over the size of the thumbnail since the quality lost
  // when scaling it to fit a view will not be significant.
  private static int getThumbnailKind(ResizeOptions resizeOptions) {
    if (ThumbnailSizeChecker.isImageBigEnough(
        MICRO_THUMBNAIL_DIMENSIONS.width(), MICRO_THUMBNAIL_DIMENSIONS.height(), resizeOptions)) {
      return MediaStore.Images.Thumbnails.MICRO_KIND;
    } else if (ThumbnailSizeChecker.isImageBigEnough(
        MINI_THUMBNAIL_DIMENSIONS.width(), MINI_THUMBNAIL_DIMENSIONS.height(), resizeOptions)) {
      return MediaStore.Images.Thumbnails.MINI_KIND;
    } else {
      return NO_THUMBNAIL;
    }
  }

  private static int getLength(String pathname) {
    return pathname == null ? -1 : (int) new File(pathname).length();
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }

  private static int getRotationAngle(@Nullable String pathname) {
    if (pathname != null) {
      try {
        ExifInterface exif = new ExifInterface(pathname);
        return JfifUtil.getAutoRotateAngleFromOrientation(
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
      } catch (IOException ioe) {
        FLog.e(TAG, ioe, "Unable to retrieve thumbnail rotation for %s", pathname);
      }
    }
    return 0;
  }
}
