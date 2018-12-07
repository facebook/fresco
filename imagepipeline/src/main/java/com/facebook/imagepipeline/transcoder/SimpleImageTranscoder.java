/*
 * Copyright (c) 2018-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import com.facebook.common.logging.FLog;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import java.io.OutputStream;
import javax.annotation.Nullable;

/**
 * Image transcoder using only the Android API. Clients can use this if they don't want to use the
 * native implementation. This image transcoder requires more memory.
 */
public class SimpleImageTranscoder implements ImageTranscoder {
  private static final String TAG = "SimpleImageTranscoder";
  private final boolean mResizingEnabled;
  private final int mMaxBitmapSize;

  public SimpleImageTranscoder(final boolean resizingEnabled, final int maxBitmapSize) {
    mResizingEnabled = resizingEnabled;
    mMaxBitmapSize = maxBitmapSize;
  }

  @Override
  public ImageTranscodeResult transcode(
      EncodedImage encodedImage,
      OutputStream outputStream,
      @Nullable RotationOptions rotationOptions,
      @Nullable ResizeOptions resizeOptions,
      @Nullable ImageFormat outputFormat,
      @Nullable Integer quality) {
    if (quality == null) {
      quality = JpegTranscoderUtils.DEFAULT_JPEG_QUALITY;
    }
    if (rotationOptions == null) {
      rotationOptions = RotationOptions.autoRotate();
    }

    final int sampleSize = getSampleSize(encodedImage, rotationOptions, resizeOptions);
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = sampleSize;
    Bitmap resizedBitmap;
    try {
      resizedBitmap = BitmapFactory.decodeStream(encodedImage.getInputStream(), null, options);
    } catch (OutOfMemoryError oom) {
      FLog.e(TAG, "Out-Of-Memory during transcode", oom);
      return new ImageTranscodeResult(TranscodeStatus.TRANSCODING_ERROR);
    }

    if (resizedBitmap == null) {
      FLog.e(TAG, "Couldn't decode the EncodedImage InputStream ! ");
      return new ImageTranscodeResult(TranscodeStatus.TRANSCODING_ERROR);
    }

    Matrix transformationMatrix =
        JpegTranscoderUtils.getTransformationMatrix(encodedImage, rotationOptions);

    Bitmap srcBitmap = resizedBitmap;
    try {
      if (transformationMatrix != null) {
        srcBitmap =
            Bitmap.createBitmap(
                resizedBitmap,
                0,
                0,
                resizedBitmap.getWidth(),
                resizedBitmap.getHeight(),
                transformationMatrix,
                false);
      }
      srcBitmap.compress(getOutputFormat(outputFormat), quality, outputStream);
      return new ImageTranscodeResult(
          sampleSize > DownsampleUtil.DEFAULT_SAMPLE_SIZE
              ? TranscodeStatus.TRANSCODING_SUCCESS
              : TranscodeStatus.TRANSCODING_NO_RESIZING);
    } catch (OutOfMemoryError oom) {
      FLog.e(TAG, "Out-Of-Memory during transcode", oom);
      return new ImageTranscodeResult(TranscodeStatus.TRANSCODING_ERROR);
    } finally {
      srcBitmap.recycle();
      resizedBitmap.recycle();
    }
  }

  @Override
  public boolean canResize(
      EncodedImage encodedImage,
      @Nullable RotationOptions rotationOptions,
      @Nullable ResizeOptions resizeOptions) {
    if (rotationOptions == null) {
      rotationOptions = RotationOptions.autoRotate();
    }
    return mResizingEnabled
        && DownsampleUtil.determineSampleSize(
                rotationOptions, resizeOptions, encodedImage, mMaxBitmapSize)
            > DownsampleUtil.DEFAULT_SAMPLE_SIZE;
  }

  @Override
  public boolean canTranscode(ImageFormat imageFormat) {
    return imageFormat == DefaultImageFormats.HEIF || imageFormat == DefaultImageFormats.JPEG;
  }

  @Override
  public String getIdentifier() {
    return "SimpleImageTranscoder";
  }

  private int getSampleSize(
      final EncodedImage encodedImage,
      final RotationOptions rotationOptions,
      @Nullable final ResizeOptions resizeOptions) {
    int sampleSize;
    if (!mResizingEnabled) {
      sampleSize = DownsampleUtil.DEFAULT_SAMPLE_SIZE;
    } else {
      sampleSize =
          DownsampleUtil.determineSampleSize(
              rotationOptions, resizeOptions, encodedImage, mMaxBitmapSize);
    }
    return sampleSize;
  }

  /**
   * Determine the {@link Bitmap.CompressFormat} given the {@link ImageFormat}. If no match is
   * found, it returns {@link Bitmap.CompressFormat#JPEG}
   *
   * @param format The {@link ImageFormat} used as input
   * @return The corresponding {@link Bitmap.CompressFormat}
   */
  private static Bitmap.CompressFormat getOutputFormat(@Nullable final ImageFormat format) {
    if (format == null) {
      return Bitmap.CompressFormat.JPEG;
    }

    if (format == DefaultImageFormats.JPEG) {
      return Bitmap.CompressFormat.JPEG;
    } else if (format == DefaultImageFormats.PNG) {
      return Bitmap.CompressFormat.PNG;
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
          && DefaultImageFormats.isStaticWebpFormat(format)) {
        return Bitmap.CompressFormat.WEBP;
      }

      return Bitmap.CompressFormat.JPEG;
    }
  }
}
