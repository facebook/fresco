/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.webpsupport;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.webp.BitmapCreator;
import com.facebook.common.webp.WebpBitmapFactory;
import com.facebook.imagepipeline.nativecode.StaticWebpNativeLoader;
import com.facebook.infer.annotation.Nullsafe;
import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
@DoNotStrip
public class WebpBitmapFactoryImpl implements WebpBitmapFactory {

  private static final int HEADER_SIZE = 20;

  private static final int IN_TEMP_BUFFER_SIZE = 8 * 1024;

  private static WebpErrorLogger mWebpErrorLogger;

  private static BitmapCreator mBitmapCreator;

  @Override
  public void setBitmapCreator(final BitmapCreator bitmapCreator) {
    mBitmapCreator = bitmapCreator;
  }

  private static InputStream wrapToMarkSupportedStream(InputStream inputStream) {
    if (!inputStream.markSupported()) {
      inputStream = new BufferedInputStream(inputStream, HEADER_SIZE);
    }
    return inputStream;
  }

  private static @Nullable byte[] getWebpHeader(
      InputStream inputStream, @Nullable BitmapFactory.Options opts) {
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

  private static void setDensityFromOptions(
      @Nullable Bitmap outputBitmap, @Nullable BitmapFactory.Options opts) {
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
    } else if (opts.inBitmap != null) {
      // bitmap was reused, ensure density is reset
      outputBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
    }
  }

  @Override
  public void setWebpErrorLogger(WebpBitmapFactory.WebpErrorLogger webpErrorLogger) {
    mWebpErrorLogger = webpErrorLogger;
  }

  @Override
  @Nullable
  public Bitmap decodeFileDescriptor(
      FileDescriptor fd, @Nullable Rect outPadding, @Nullable BitmapFactory.Options opts) {
    return hookDecodeFileDescriptor(fd, outPadding, opts);
  }

  @Override
  @Nullable
  public Bitmap decodeStream(
      InputStream inputStream, @Nullable Rect outPadding, @Nullable BitmapFactory.Options opts) {
    return hookDecodeStream(inputStream, outPadding, opts);
  }

  @Override
  @Nullable
  public Bitmap decodeFile(String pathName, @Nullable BitmapFactory.Options opts) {
    return hookDecodeFile(pathName, opts);
  }

  @Override
  @Nullable
  public Bitmap decodeByteArray(
      byte[] array, int offset, int length, @Nullable BitmapFactory.Options opts) {
    return hookDecodeByteArray(array, offset, length, opts);
  }

  @DoNotStrip
  @Nullable
  public static Bitmap hookDecodeByteArray(
      byte[] array, int offset, int length, @Nullable BitmapFactory.Options opts) {
    StaticWebpNativeLoader.ensure();
    Bitmap bitmap = originalDecodeByteArray(array, offset, length, opts);
    if (bitmap == null) {
      sendWebpErrorLog("webp_direct_decode_array_failed_on_no_webp");
    }
    return bitmap;
  }

  @DoNotStrip
  @Nullable
  private static Bitmap originalDecodeByteArray(
      byte[] array, int offset, int length, @Nullable BitmapFactory.Options opts) {
    return BitmapFactory.decodeByteArray(array, offset, length, opts);
  }

  @DoNotStrip
  @Nullable
  public static Bitmap hookDecodeByteArray(byte[] array, int offset, int length) {
    return hookDecodeByteArray(array, offset, length, null);
  }

  @DoNotStrip
  @Nullable
  private static Bitmap originalDecodeByteArray(byte[] array, int offset, int length) {
    return BitmapFactory.decodeByteArray(array, offset, length);
  }

  @DoNotStrip
  @Nullable
  public static Bitmap hookDecodeStream(
      InputStream inputStream, @Nullable Rect outPadding, @Nullable BitmapFactory.Options opts) {
    StaticWebpNativeLoader.ensure();
    inputStream = wrapToMarkSupportedStream(inputStream);

    Bitmap bitmap;

    bitmap = originalDecodeStream(inputStream, outPadding, opts);
    if (bitmap == null) {
      sendWebpErrorLog("webp_direct_decode_stream_failed_on_no_webp");
    }

    return bitmap;
  }

  @DoNotStrip
  @Nullable
  private static Bitmap originalDecodeStream(
      InputStream inputStream, @Nullable Rect outPadding, @Nullable BitmapFactory.Options opts) {
    return BitmapFactory.decodeStream(inputStream, outPadding, opts);
  }

  @DoNotStrip
  @Nullable
  public static Bitmap hookDecodeStream(InputStream inputStream) {
    return hookDecodeStream(inputStream, null, null);
  }

  @Nullable
  @DoNotStrip
  private static Bitmap originalDecodeStream(InputStream inputStream) {
    return BitmapFactory.decodeStream(inputStream);
  }

  @DoNotStrip
  public static @Nullable Bitmap hookDecodeFile(
      String pathName, @Nullable BitmapFactory.Options opts) {
    Bitmap bitmap = null;
    try (InputStream stream = new FileInputStream(pathName)) {
      bitmap = hookDecodeStream(stream, null, opts);
    } catch (Exception e) {
      // Ignore, will just return null
    }
    return bitmap;
  }

  @DoNotStrip
  @Nullable
  public static Bitmap hookDecodeFile(String pathName) {
    return hookDecodeFile(pathName, null);
  }

  @DoNotStrip
  @Nullable
  public static Bitmap hookDecodeResourceStream(
      @Nullable Resources res,
      @Nullable TypedValue value,
      InputStream is,
      @Nullable Rect pad,
      @Nullable BitmapFactory.Options opts) {
    if (opts == null) {
      opts = new BitmapFactory.Options();
    }

    if (opts.inDensity == 0 && value != null) {
      final int density = value.density;
      if (density == TypedValue.DENSITY_DEFAULT) {
        opts.inDensity = DisplayMetrics.DENSITY_DEFAULT;
      } else if (density != TypedValue.DENSITY_NONE) {
        opts.inDensity = density;
      }
    }

    if (opts.inTargetDensity == 0 && res != null) {
      opts.inTargetDensity = res.getDisplayMetrics().densityDpi;
    }

    return hookDecodeStream(is, pad, opts);
  }

  @DoNotStrip
  @Nullable
  private static Bitmap originalDecodeResourceStream(
      Resources res, TypedValue value, InputStream is, Rect pad, BitmapFactory.Options opts) {
    return BitmapFactory.decodeResourceStream(res, value, is, pad, opts);
  }

  @DoNotStrip
  public static @Nullable Bitmap hookDecodeResource(
      Resources res, int id, @Nullable BitmapFactory.Options opts) {
    Bitmap bm = null;
    TypedValue value = new TypedValue();

    try (InputStream is = res.openRawResource(id, value)) {
      bm = hookDecodeResourceStream(res, value, is, null, opts);
    } catch (Exception e) {
      // Keep resulting bitmap as null
    }

    if (bm == null && opts != null && opts.inBitmap != null) {
      throw new IllegalArgumentException("Problem decoding into existing bitmap");
    }

    return bm;
  }

  @DoNotStrip
  @Nullable
  private static Bitmap originalDecodeResource(Resources res, int id, BitmapFactory.Options opts) {
    return BitmapFactory.decodeResource(res, id, opts);
  }

  @DoNotStrip
  @Nullable
  public static Bitmap hookDecodeResource(Resources res, int id) {
    return hookDecodeResource(res, id, null);
  }

  @DoNotStrip
  @Nullable
  private static Bitmap originalDecodeResource(Resources res, int id) {
    return BitmapFactory.decodeResource(res, id);
  }

  @DoNotStrip
  private static boolean setOutDimensions(
      @Nullable BitmapFactory.Options options, int imageWidth, int imageHeight) {
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
      @Nullable BitmapFactory.Options options, int width, int height) {
    if (options != null) {
      options.outWidth = width;
      options.outHeight = height;
    }
  }

  @DoNotStrip
  @Nullable
  private static Bitmap originalDecodeFile(String pathName, @Nullable BitmapFactory.Options opts) {
    return BitmapFactory.decodeFile(pathName, opts);
  }

  @DoNotStrip
  @Nullable
  private static Bitmap originalDecodeFile(String pathName) {
    return BitmapFactory.decodeFile(pathName);
  }

  @DoNotStrip
  @Nullable
  public static Bitmap hookDecodeFileDescriptor(
      FileDescriptor fd, @Nullable Rect outPadding, @Nullable BitmapFactory.Options opts) {
    StaticWebpNativeLoader.ensure();
    Bitmap bitmap;

    long originalSeekPosition = nativeSeek(fd, 0, false);
    if (originalSeekPosition != -1) {
      InputStream inputStream = wrapToMarkSupportedStream(new FileInputStream(fd));
      try {
        byte[] header = getWebpHeader(inputStream, opts);
        nativeSeek(fd, originalSeekPosition, true);
        bitmap = originalDecodeFileDescriptor(fd, outPadding, opts);
        if (bitmap == null) {
          sendWebpErrorLog("webp_direct_decode_fd_failed_on_no_webp");
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
  @Nullable
  private static Bitmap originalDecodeFileDescriptor(
      FileDescriptor fd, @Nullable Rect outPadding, @Nullable BitmapFactory.Options opts) {
    return BitmapFactory.decodeFileDescriptor(fd, outPadding, opts);
  }

  @DoNotStrip
  @Nullable
  public static Bitmap hookDecodeFileDescriptor(FileDescriptor fd) {
    return hookDecodeFileDescriptor(fd, null, null);
  }

  @DoNotStrip
  @Nullable
  private static Bitmap originalDecodeFileDescriptor(FileDescriptor fd) {
    return BitmapFactory.decodeFileDescriptor(fd);
  }

  private static void setWebpBitmapOptions(
      @Nullable Bitmap bitmap, @Nullable BitmapFactory.Options opts) {
    setDensityFromOptions(bitmap, opts);
    if (opts != null) {
      opts.outMimeType = "image/webp";
    }
  }

  @DoNotStrip
  @SuppressLint("NewApi")
  private static boolean shouldPremultiply(@Nullable BitmapFactory.Options options) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && options != null) {
      return options.inPremultiplied;
    }
    return true;
  }

  @DoNotStrip
  @Nullable
  private static Bitmap createBitmap(
      int width, int height, @Nullable BitmapFactory.Options options) {
    if (options != null && options.inBitmap != null && options.inBitmap.isMutable()) {
      return options.inBitmap;
    }
    return mBitmapCreator.createNakedBitmap(width, height, Bitmap.Config.ARGB_8888);
  }

  @DoNotStrip
  @Nullable
  private static native Bitmap nativeDecodeStream(
      InputStream is, @Nullable BitmapFactory.Options options, float scale, byte[] inTempStorage);

  @DoNotStrip
  @Nullable
  private static native Bitmap nativeDecodeByteArray(
      byte[] data,
      int offset,
      int length,
      @Nullable BitmapFactory.Options opts,
      float scale,
      byte[] inTempStorage);

  @DoNotStrip
  private static native long nativeSeek(FileDescriptor fd, long offset, boolean absolute);

  @DoNotStrip
  private static byte[] getInTempStorageFromOptions(@Nullable final BitmapFactory.Options options) {
    if (options != null && options.inTempStorage != null) {
      return options.inTempStorage;
    } else {
      return new byte[IN_TEMP_BUFFER_SIZE];
    }
  }

  @DoNotStrip
  private static float getScaleFromOptions(@Nullable BitmapFactory.Options options) {
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

  private static void sendWebpErrorLog(String message) {
    // We want to track only when bitmap is null after native decoding
    if (mWebpErrorLogger != null) {
      mWebpErrorLogger.onWebpErrorLog(message, "decoding_failure");
    }
  }
}
