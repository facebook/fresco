/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.Pair;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferInputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.imageutils.JfifUtil;
import com.facebook.soloader.DoNotOptimize;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * A producer that retrieves exif thumbnails.
 *
 * <p>At present, these thumbnails are retrieved on the java heap before being put into native
 * memory.
 */
public class LocalExifThumbnailProducer implements ThumbnailProducer<EncodedImage> {

  private static final int COMMON_EXIF_THUMBNAIL_MAX_DIMENSION = 512;

  public static final String PRODUCER_NAME = "LocalExifThumbnailProducer";
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

  /**
   * Checks whether the producer may be able to produce images of the specified size. This makes no
   * promise about being able to produce images for a particular source, only generally being able
   * to produce output of the desired resolution.
   *
   * <p>In this case, assumptions are made about the common size of EXIF thumbnails which is that
   * they may be up to 512 pixels in each dimension.
   *
   * @param resizeOptions the resize options from the current request
   * @return true if the producer can meet these needs
   */
  @Override
  public boolean canProvideImageForSize(ResizeOptions resizeOptions) {
    return ThumbnailSizeChecker.isImageBigEnough(
        COMMON_EXIF_THUMBNAIL_MAX_DIMENSION, COMMON_EXIF_THUMBNAIL_MAX_DIMENSION, resizeOptions);
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer, final ProducerContext producerContext) {

    final ProducerListener2 listener = producerContext.getProducerListener();
    final ImageRequest imageRequest = producerContext.getImageRequest();

    producerContext.putOriginExtra("local", "exif");
    final StatefulProducerRunnable cancellableProducerRunnable =
        new StatefulProducerRunnable<EncodedImage>(
            consumer, listener, producerContext, PRODUCER_NAME) {
          @Override
          protected @Nullable EncodedImage getResult() throws Exception {
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

  @VisibleForTesting
  @Nullable
  ExifInterface getExifInterface(Uri uri) {
    final String realPath = UriUtil.getRealPathFromUri(mContentResolver, uri);
    try {
      if (canReadAsFile(realPath)) {
        return new ExifInterface(realPath);
      } else {
        AssetFileDescriptor assetFileDescriptor =
            UriUtil.getAssetFileDescriptor(mContentResolver, uri);
        if (assetFileDescriptor != null && Build.VERSION.SDK_INT >= 24) {
          final ExifInterface exifInterface =
              (new Api24Utils()).getExifInterface(assetFileDescriptor.getFileDescriptor());
          assetFileDescriptor.close();
          return exifInterface;
        }
      }
    } catch (IOException e) {
      // If we cannot get the exif interface, return null as there is no thumbnail available
    } catch (StackOverflowError e) {
      // Device-specific issue when loading huge images
      FLog.e(LocalExifThumbnailProducer.class, "StackOverflowError in ExifInterface constructor");
    }
    return null;
  }

  private EncodedImage buildEncodedImage(PooledByteBuffer imageBytes, ExifInterface exifInterface) {
    Pair<Integer, Integer> dimensions =
        BitmapUtil.decodeDimensions(new PooledByteBufferInputStream(imageBytes));
    int rotationAngle = getRotationAngle(exifInterface);
    int width = dimensions != null ? dimensions.first : EncodedImage.UNKNOWN_WIDTH;
    int height = dimensions != null ? dimensions.second : EncodedImage.UNKNOWN_HEIGHT;
    EncodedImage encodedImage;
    CloseableReference<PooledByteBuffer> closeableByteBuffer = CloseableReference.of(imageBytes);
    try {
      encodedImage = new EncodedImage(closeableByteBuffer);
    } finally {
      CloseableReference.closeSafely(closeableByteBuffer);
    }
    encodedImage.setImageFormat(DefaultImageFormats.JPEG);
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

  @VisibleForTesting
  boolean canReadAsFile(String realPath) throws IOException {
    if (realPath == null) {
      return false;
    }
    final File file = new File(realPath);
    return file.exists() && file.canRead();
  }

  @DoNotOptimize
  private class Api24Utils {
    ExifInterface getExifInterface(FileDescriptor fileDescriptor) throws IOException {
      return Build.VERSION.SDK_INT >= 24 ? new ExifInterface(fileDescriptor) : null;
    }
  }
}
