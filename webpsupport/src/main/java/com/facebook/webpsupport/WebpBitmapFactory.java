/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.webpsupport;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.common.internal.DoNotStrip;

import static com.facebook.imagepipeline.webp.WebpSupportStatus.isWebpPlatformSupported;
import static com.facebook.imagepipeline.webp.WebpSupportStatus.isWebpHeader;

@DoNotStrip
public class WebpBitmapFactory {
  private static final String TAG = "WebpBitmapFactory";

  private static final int HEADER_SIZE = 20;

  private static final int IN_TEMP_BUFFER_SIZE = 8*1024;

  public static final boolean IN_BITMAP_SUPPORTED =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

  static {
    SoLoaderShim.loadLibrary("webp");
    SoLoaderShim.loadLibrary("webpsupport");
  }

  private static InputStream wrapToMarkSupportedStream(InputStream inputStream) {
    if (!inputStream.markSupported()) {
      inputStream = new BufferedInputStream(inputStream, HEADER_SIZE);
    }
    return inputStream;
  }

  private static byte[] getWebpHeader(InputStream inputStream, BitmapFactory.Options opts) {
    inputStream.mark(HEADER_SIZE);

    byte[] header;

    if (opts != null && opts.inTempStorage != null && opts.inTempStorage.length >= HEADER_SIZE) {
      header = opts.inTempStorage;
    } else {
      header = new byte[HEADER_SIZE];
    }
    try {
      inputStream.read(header, 0, HEADER_SIZE);
      inputStream.reset();
    } catch (IOException exp) {
      return null;
    }
    return header;
  }

  private static void setDensityFromOptions(Bitmap outputBitmap, BitmapFactory.Options opts) {
    if (outputBitmap == null || opts == null) {
      return;
    }

    final int density = opts.inDensity;
    if (density != 0) {
      outputBitmap.setDensity(density);
      final int targetDensity = opts.inTargetDensity;
      if (targetDensity == 0 || density == targetDensity || density == opts.inScreenDensity) {
        return;
      }

      if (opts.inScaled) {
        outputBitmap.setDensity(targetDensity);
      }
    } else if (IN_BITMAP_SUPPORTED && opts.inBitmap != null) {
      // bitmap was reused, ensure density is reset
      outputBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
    }
  }

  @DoNotStrip
  public static Bitmap hookDecodeByteArray(
      byte[] array,
      int offset,
      int length,
      BitmapFactory.Options opts) {
    Bitmap bitmap;
    if (isWebpHeader(array, offset, length) && !isWebpPlatformSupported(array, offset, length)) {
      bitmap = nativeDecodeByteArray(
          array,
          offset,
          length,
          opts,
          getScaleFromOptions(opts),
          getInBitmapFromOptions(opts),
          getInTempStorageFromOptions(opts));
      setWebpBitmapOptions(bitmap, opts);
    } else {
      bitmap = originalDecodeByteArray(array, offset, length, opts);
    }
    return bitmap;
  }

  @DoNotStrip
  private static Bitmap originalDecodeByteArray(
      byte[] array,
      int offset,
      int length,
      BitmapFactory.Options opts) {
    return BitmapFactory.decodeByteArray(array, offset, length);
  }

  @DoNotStrip
  public static Bitmap hookDecodeStream(
      InputStream inputStream,
      Rect outPadding,
      BitmapFactory.Options opts) {
    inputStream = wrapToMarkSupportedStream(inputStream);

    Bitmap bitmap;

    byte[] header = getWebpHeader(inputStream, opts);
    if (isWebpHeader(header, 0, HEADER_SIZE) && !isWebpPlatformSupported(header, 0, HEADER_SIZE)) {
      bitmap = nativeDecodeStream(
          inputStream,
          opts,
          getScaleFromOptions(opts),
          getInBitmapFromOptions(opts),
          getInTempStorageFromOptions(opts));
      setWebpBitmapOptions(bitmap, opts);
      setPaddingDefaultValues(outPadding);
    } else {
      bitmap = originalDecodeStream(inputStream, outPadding, opts);
    }

    return bitmap;
  }

  @DoNotStrip
  private static Bitmap originalDecodeStream(
      InputStream inputStream,
      Rect outPadding,
      BitmapFactory.Options opts) {
    return BitmapFactory.decodeStream(inputStream, outPadding, opts);
  }

  @DoNotStrip
  public static Bitmap hookDecodeFile(
      String pathName,
      BitmapFactory.Options opts) {
    Bitmap bitmap = null;
    try (InputStream stream = new FileInputStream(pathName)) {
      bitmap = hookDecodeStream(stream, null, opts);
    } catch (Exception e) {
      // Ignore, will just return null
    }
    return bitmap;
  }

  @DoNotStrip
  private static boolean setOutDimensions(
      BitmapFactory.Options options,
      int imageWidth,
      int imageHeight) {
    if (options != null && options.inJustDecodeBounds) {
      options.outWidth = imageWidth;
      options.outHeight = imageHeight;
      return true;
    }
    return false;
  }

  @DoNotStrip
  private static void setPaddingDefaultValues(@Nullable Rect padding) {
    if (padding != null) {
      padding.top = -1;
      padding.left = -1;
      padding.bottom = -1;
      padding.right = -1;
    }
  }

  @DoNotStrip
  private static void setBitmapSize(
      @Nullable BitmapFactory.Options options,
      int width,
      int height) {
    if (options != null) {
      options.outWidth = width;
      options.outHeight = height;
    }
  }

  @DoNotStrip
  private static Bitmap originalDecodeFile(
      String pathName,
      BitmapFactory.Options opts) {
    return BitmapFactory.decodeFile(pathName, opts);
  }

  @DoNotStrip
  public static Bitmap hookDecodeFileDescriptor(
      FileDescriptor fd,
      Rect outPadding,
      BitmapFactory.Options opts) {
    Bitmap bitmap;

    long originalSeekPosition = nativeSeek(fd, 0, false);
    if (originalSeekPosition != -1) {
      InputStream inputStream = wrapToMarkSupportedStream(new FileInputStream(fd));

      try {
        byte[] header = getWebpHeader(inputStream, opts);
        if (isWebpHeader(header, 0, HEADER_SIZE)
            && !isWebpPlatformSupported(header, 0, HEADER_SIZE)) {
          bitmap = nativeDecodeStream(
              inputStream,
              opts,
              getScaleFromOptions(opts),
              getInBitmapFromOptions(opts),
              getInTempStorageFromOptions(opts));
          setPaddingDefaultValues(outPadding);
          setWebpBitmapOptions(bitmap, opts);
        } else {
          nativeSeek(fd, originalSeekPosition, true);
          bitmap = originalDecodeFileDescriptor(fd, outPadding, opts);
        }
      } finally {
        try {
          inputStream.close();
        } catch (Throwable t) {
          /* ignore */
        }
      }
    } else {
      bitmap = hookDecodeStream(new FileInputStream(fd), outPadding, opts);
      setPaddingDefaultValues(outPadding);
    }
    return bitmap;
  }

  @DoNotStrip
  private static Bitmap originalDecodeFileDescriptor(
      FileDescriptor fd,
      Rect outPadding,
      BitmapFactory.Options opts) {
    return BitmapFactory.decodeFileDescriptor(fd, outPadding, opts);
  }

  private static void setWebpBitmapOptions(Bitmap bitmap, BitmapFactory.Options opts) {
    setDensityFromOptions(bitmap, opts);
    if (opts != null) {
      opts.outMimeType = "image/webp";
    }
  }

  @DoNotStrip
  @SuppressLint("NewApi")
  private static boolean shouldPremultiply(BitmapFactory.Options options) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && options != null) {
      return options.inPremultiplied;
    }
    return true;
  }

  @DoNotStrip
  private static Bitmap createBitmap(int width, int height, BitmapFactory.Options options) {
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
  }

  @DoNotStrip
  private static native Bitmap nativeDecodeStream(
      InputStream is,
      BitmapFactory.Options options,
      float scale,
      Bitmap inBitmap,
      byte[] inTempStorage);

  @DoNotStrip
  private static native Bitmap nativeDecodeByteArray(
      byte[] data,
      int offset,
      int length,
      BitmapFactory.Options opts,
      float scale,
      Bitmap inBitmap,
      byte[] inTempStorage);


  @DoNotStrip
  private static native long nativeSeek(FileDescriptor fd, long offset, boolean absolute);

  @DoNotStrip
  private static Bitmap getInBitmapFromOptions(final BitmapFactory.Options options) {
    if (IN_BITMAP_SUPPORTED && options != null) {
      return options.inBitmap;
    } else {
      return null;
    }
  }

  @DoNotStrip
  private static byte[] getInTempStorageFromOptions(@Nullable final BitmapFactory.Options options) {
    if (options != null && options.inTempStorage != null) {
      return options.inTempStorage;
    } else {
      return new byte[IN_TEMP_BUFFER_SIZE];
    }
  }

  @DoNotStrip
  private static float getScaleFromOptions(BitmapFactory.Options options) {
    float scale = 1.0f;
    if (options != null) {
      int sampleSize = options.inSampleSize;
      if (sampleSize > 1) {
        scale = 1.0f / (float) sampleSize;
      }
      if (options.inScaled) {
        int density = options.inDensity;
        int targetDensity = options.inTargetDensity;
        int screenDensity = options.inScreenDensity;
        if (density != 0 && targetDensity != 0 && density != screenDensity) {
          scale = targetDensity / (float) density;
        }
      }
    }
    return scale;
  }
}
