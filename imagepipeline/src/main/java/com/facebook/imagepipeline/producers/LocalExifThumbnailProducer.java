/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

import android.graphics.Rect;
import android.media.ExifInterface;
import android.util.Pair;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageutils.JfifUtil;

/**
 * A producer that retrieves exif thumbnails.
 *
 * <p>At present, these thumbnails are retrieved on the java heap before being put into native
 * memory.
 */
public class LocalExifThumbnailProducer implements
    Producer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> {

  @VisibleForTesting static final String PRODUCER_NAME = "LocalExifThumbnailProducer";
  @VisibleForTesting static final String CREATED_THUMBNAIL = "createdThumbnail";

  private final Executor mExecutor;
  private final PooledByteBufferFactory mPooledByteBufferFactory;

  public LocalExifThumbnailProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory) {
    mExecutor = executor;
    mPooledByteBufferFactory = pooledByteBufferFactory;
  }

  @Override
  public void produceResults(
      final Consumer<Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>> consumer,
      final ProducerContext producerContext) {

    final ProducerListener listener = producerContext.getListener();
    final String requestId = producerContext.getId();
    final ImageRequest imageRequest = producerContext.getImageRequest();

    final StatefulProducerRunnable cancellableProducerRunnable =
        new StatefulProducerRunnable<
            Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData>>(
            consumer,
            listener,
            PRODUCER_NAME,
            requestId) {
          @Override
          protected Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> getResult()
              throws Exception {
            final ExifInterface exifInterface =
                getExifInterface(imageRequest.getSourceFile().getPath());
            if (!exifInterface.hasThumbnail()) {
              return null;
            }

            byte[] bytes = exifInterface.getThumbnail();
            PooledByteBuffer pooledByteBuffer = mPooledByteBufferFactory.newByteBuffer(bytes);
            ImageTransformMetaData imageTransformMetaData =
                getImageTransformMetaData(pooledByteBuffer, exifInterface);
            return Pair.create(CloseableReference.of(pooledByteBuffer), imageTransformMetaData);
          }

          @Override
          protected void disposeResult(
              Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> result) {
            if (result != null) {
              CloseableReference.closeSafely(result.first);
            }
          }

          @Override
          protected Map<String, String> getExtraMapOnSuccess(
              final Pair<CloseableReference<PooledByteBuffer>, ImageTransformMetaData> result) {
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

  @VisibleForTesting ExifInterface getExifInterface(String path) throws IOException {
    return new ExifInterface(path);
  }

  private ImageTransformMetaData getImageTransformMetaData(
      PooledByteBuffer imageBytes,
      ExifInterface exifInterface) {
    ImageTransformMetaData.Builder builder = ImageTransformMetaData.newBuilder()
        .setImageFormat(ImageFormat.JPEG);
    builder.setRotationAngle(getRotationAngle(exifInterface));
    Rect dimensions = JfifUtil.getDimensions(new PooledByteBufferInputStream(imageBytes));
    if (dimensions != null) {
      builder.setWidth(dimensions.width());
      builder.setHeight(dimensions.height());
    }
    return builder.build();
  }

  // Gets the correction angle based on the image's orientation
  private int getRotationAngle(final ExifInterface exifInterface) {
    return JfifUtil.getAutoRotateAngleFromOrientation(
        Integer.parseInt(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)));
  }
}
