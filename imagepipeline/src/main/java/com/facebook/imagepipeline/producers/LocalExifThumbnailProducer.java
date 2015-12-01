/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Pair;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.imageutils.JfifUtil;

/**
 * A producer that retrieves exif thumbnails.
 *
 * <p>At present, these thumbnails are retrieved on the java heap before being put into native
 * memory.
 */
public class LocalExifThumbnailProducer implements Producer<EncodedImage> {

  @VisibleForTesting static final String PRODUCER_NAME = "LocalExifThumbnailProducer";
  @VisibleForTesting static final String CREATED_THUMBNAIL = "createdThumbnail";

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final ContentResolver mContentResolver;

  public LocalExifThumbnailProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      ContentResolver contentResolver) {
    mExecutor = executor;
    mPooledByteBufferFactory = pooledByteBufferFactory;
    mContentResolver = contentResolver;
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {

    final ProducerListener listener = producerContext.getListener();
    final String requestId = producerContext.getId();
    final ImageRequest imageRequest = producerContext.getImageRequest();

    final StatefulProducerRunnable cancellableProducerRunnable =
        new StatefulProducerRunnable<EncodedImage>(
            consumer,
            listener,
            PRODUCER_NAME,
            requestId) {
          @Override
          protected EncodedImage getResult()
              throws Exception {
            final Uri sourceUri = imageRequest.getSourceUri();

            final ExifInterface exifInterface = getExifInterface(sourceUri);
            if (exifInterface == null || !exifInterface.hasThumbnail()) {
              return null;
            }

            byte[] bytes = exifInterface.getThumbnail();
            PooledByteBuffer pooledByteBuffer = mPooledByteBufferFactory.newByteBuffer(bytes);
            return buildEncodedImage(pooledByteBuffer, exifInterface);
          }

          @Override
          protected void disposeResult(EncodedImage result) {
            EncodedImage.closeSafely(result);
          }

          @Override
          protected Map<String, String> getExtraMapOnSuccess(final EncodedImage result) {
            return ImmutableMap.of(CREATED_THUMBNAIL, Boolean.toString(result != null));
          }
        };
    producerContext.addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            cancellableProducerRunnable.cancel();
          }
        });
    mExecutor.execute(cancellableProducerRunnable);
  }

  @VisibleForTesting ExifInterface getExifInterface(Uri uri) throws IOException {
    final String realPath = getRealPathFromUri(uri);
    if (canReadAsFile(realPath)) {
        return new ExifInterface(realPath);
    }
    return null;
  }

  private EncodedImage buildEncodedImage(
      PooledByteBuffer imageBytes,
      ExifInterface exifInterface) {
    Pair<Integer, Integer> dimensions =
        BitmapUtil.decodeDimensions(new PooledByteBufferInputStream(imageBytes));
    int rotationAngle = getRotationAngle(exifInterface);
    int width = dimensions != null ? dimensions.first : EncodedImage.UNKNOWN_WIDTH;
    int height = dimensions != null ? dimensions.second : EncodedImage.UNKNOWN_HEIGHT;
    EncodedImage encodedImage = new EncodedImage(CloseableReference.of(imageBytes));
    encodedImage.setImageFormat(ImageFormat.JPEG);
    encodedImage.setRotationAngle(rotationAngle);
    encodedImage.setWidth(width);
    encodedImage.setHeight(height);
    return encodedImage;
  }

  // Gets the correction angle based on the image's orientation
  private int getRotationAngle(final ExifInterface exifInterface) {
    return JfifUtil.getAutoRotateAngleFromOrientation(
        Integer.parseInt(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)));
  }

  /**
   * Get the path of a file from the Uri
   * @param srcUri The source uri
   * @return The Path for the file or null if doesn't exists
   */
  private String getRealPathFromUri(final Uri srcUri) {
    String result = null;
    if (UriUtil.isLocalContentUri(srcUri)) {
      Cursor cursor = mContentResolver.query(srcUri, null, null, null, null);
      if (cursor != null) {
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        result = cursor.getString(idx);
        cursor.close();
      }
    } else if (UriUtil.isLocalFileUri(srcUri)) {
      result = srcUri.getPath();
    }
    return result;
  }

  @VisibleForTesting boolean canReadAsFile(String realPath) throws IOException {
    if (realPath == null) {
      return false;
    }
    final File file = new File(realPath);
    return file.exists() && file.canRead();
  }

}
