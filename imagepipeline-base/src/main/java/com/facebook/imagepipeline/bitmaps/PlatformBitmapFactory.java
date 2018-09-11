/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.DisplayMetrics;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import javax.annotation.Nullable;

/**
 * Bitmap factory optimized for the platform.
 */
public abstract class PlatformBitmapFactory {

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the Bitmap.Config used to create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    return createBitmap(width, height, bitmapConfig, null);
  }

  /**
   * Creates a bitmap of the specified width and height.
   * The bitmap will be created with the default ARGB_8888 configuration
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(int width, int height) {
    return createBitmap(width, height, Bitmap.Config.ARGB_8888);
  }

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the Bitmap.Config used to create the Bitmap
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig,
      @Nullable Object callerContext) {
    return createBitmapInternal(width, height, bitmapConfig);
  }

  /**
   * Creates a bitmap of the specified width and height.
   * The bitmap will be created with the default ARGB_8888 configuration
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      @Nullable Object callerContext) {
    return createBitmap(width, height, Bitmap.Config.ARGB_8888, callerContext);
  }

  /**
   * Creates a bitmap from the specified source bitmap.
   * It is initialized with the same density as the original bitmap.
   *
   * @param source   The bitmap we are copying
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the x, y, width, height values are
   *         outside of the dimensions of the source bitmap, or width is <= 0,
   *         or height is <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(Bitmap source) {
    return createBitmap(source, null);
  }

  /**
   * Creates a bitmap from the specified source bitmap.
   * It is initialized with the same density as the original bitmap.
   *
   * @param source   The bitmap we are copying
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the x, y, width, height values are
   *         outside of the dimensions of the source bitmap, or width is <= 0,
   *         or height is <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(Bitmap source, @Nullable Object callerContext) {
    return createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), callerContext);
  }

  /**
   * Creates a bitmap from the specified subset of the source
   * bitmap. It is initialized with the same density as the original bitmap.
   *
   * @param source   The bitmap we are subsetting
   * @param x        The x coordinate of the first pixel in source
   * @param y        The y coordinate of the first pixel in source
   * @param width    The number of pixels in each row
   * @param height   The number of rows
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the x, y, width, height values are
   *         outside of the dimensions of the source bitmap, or width is <= 0,
   *         or height is <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      Bitmap source,
      int x,
      int y,
      int width,
      int height) {
    return createBitmap(source, x, y, width, height, null);
  }

  /**
   * Creates a bitmap from the specified subset of the source
   * bitmap. It is initialized with the same density as the original bitmap.
   *
   * @param source   The bitmap we are subsetting
   * @param x        The x coordinate of the first pixel in source
   * @param y        The y coordinate of the first pixel in source
   * @param width    The number of pixels in each row
   * @param height   The number of rows
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the x, y, width, height values are
   *         outside of the dimensions of the source bitmap, or width is <= 0,
   *         or height is <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      Bitmap source,
      int x,
      int y,
      int width,
      int height,
      @Nullable Object callerContext) {
    return createBitmap(source, x, y, width, height, null, false, callerContext);
  }

  /**
   * Creates a bitmap from subset of the source bitmap,
   * transformed by the optional matrix. It is initialized with the same
   * density as the original bitmap.
   *
   * @param source   The bitmap we are subsetting
   * @param x        The x coordinate of the first pixel in source
   * @param y        The y coordinate of the first pixel in source
   * @param width    The number of pixels in each row
   * @param height   The number of rows
   * @param matrix   Optional matrix to be applied to the pixels
   * @param filter   true if the source should be filtered.
   *                   Only applies if the matrix contains more than just
   *                   translation.
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the x, y, width, height values are
   *         outside of the dimensions of the source bitmap, or width is <= 0,
   *         or height is <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      Bitmap source,
      int x,
      int y,
      int width,
      int height,
      @Nullable Matrix matrix,
      boolean filter) {
    return createBitmap(
        source,
        x,
        y,
        width,
        height,
        matrix,
        filter,
        null);
  }

  /**
   * Creates a bitmap from the specified source scaled to have the height and width
   * as specified. It is initialized with the same density as the original bitmap.
   *
   * @param source   The bitmap we are subsetting
   * @param destinationWidth    The number of pixels in each row of the final bitmap
   * @param destinationHeight   The number of rows in the final bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the destinationWidth is <= 0,
   *         or destinationHeight is <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createScaledBitmap(
      Bitmap source,
      int destinationWidth,
      int destinationHeight,
      boolean filter) {
    return createScaledBitmap(source, destinationWidth, destinationHeight, filter, null);
  }

  /**
   * Creates a bitmap from the specified source scaled to have the height and width
   * as specified. It is initialized with the same density as the original bitmap.
   *
   * @param source   The bitmap we are subsetting
   * @param destinationWidth    The number of pixels in each row of the final bitmap
   * @param destinationHeight   The number of rows in the final bitmap
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the destinationWidth is <= 0,
   *         or destinationHeight is <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createScaledBitmap(
      Bitmap source,
      int destinationWidth,
      int destinationHeight,
      boolean filter,
      @Nullable Object callerContext) {
    checkWidthHeight(destinationWidth, destinationHeight);

    Matrix matrix = new Matrix();
    final int width = source.getWidth();
    final int height = source.getHeight();
    final float sx = destinationWidth  / (float) width;
    final float sy = destinationHeight / (float) height;
    matrix.setScale(sx, sy);

    return createBitmap(source, 0, 0, width, height, matrix, filter, callerContext);
  }

  /**
   * Creates a bitmap from subset of the source bitmap,
   * transformed by the optional matrix. It is initialized with the same
   * density as the original bitmap.
   *
   * @param source   The bitmap we are subsetting
   * @param x        The x coordinate of the first pixel in source
   * @param y        The y coordinate of the first pixel in source
   * @param width    The number of pixels in each row
   * @param height   The number of rows
   * @param matrix   Optional matrix to be applied to the pixels
   * @param filter   true if the source should be filtered.
   *                   Only applies if the matrix contains more than just
   *                   translation.
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the x, y, width, height values are
   *         outside of the dimensions of the source bitmap, or width is <= 0,
   *         or height is <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      Bitmap source,
      int x,
      int y,
      int width,
      int height,
      @Nullable Matrix matrix,
      boolean filter,
      @Nullable Object callerContext) {
    Preconditions.checkNotNull(source, "Source bitmap cannot be null");
    checkXYSign(x, y);
    checkWidthHeight(width, height);
    checkFinalImageBounds(source, x, y, width, height);

    // assigned because matrix can modify the final width, height
    int newWidth = width;
    int newHeight = height;

    Canvas canvas;
    CloseableReference<Bitmap> bitmapRef;
    Paint paint;

    Rect srcRectangle = new Rect(x, y, x + width, y + height);
    RectF dstRectangle = new RectF(0, 0, width, height);
    Bitmap.Config newConfig = getSuitableBitmapConfig(source);

    if (matrix == null || matrix.isIdentity()) {
      bitmapRef = createBitmap(newWidth, newHeight, newConfig, source.hasAlpha(), callerContext);
      setPropertyFromSourceBitmap(source, bitmapRef.get());
      canvas = new Canvas(bitmapRef.get());
      paint = null;   // not needed
    } else {
      boolean transformed = !matrix.rectStaysRect();
      RectF deviceRectangle = new RectF();
      matrix.mapRect(deviceRectangle, dstRectangle);

      newWidth = Math.round(deviceRectangle.width());
      newHeight = Math.round(deviceRectangle.height());
      bitmapRef =
          createBitmap(
              newWidth,
              newHeight,
              transformed ? Bitmap.Config.ARGB_8888 : newConfig,
              transformed || source.hasAlpha(),
              callerContext);

      setPropertyFromSourceBitmap(source, bitmapRef.get());
      canvas = new Canvas(bitmapRef.get());
      canvas.translate(-deviceRectangle.left, -deviceRectangle.top);
      canvas.concat(matrix);

      paint = new Paint();
      paint.setFilterBitmap(filter);
      if (transformed) {
        paint.setAntiAlias(true);
      }
    }

    canvas.drawBitmap(source, srcRectangle, dstRectangle, paint);
    canvas.setBitmap(null);

    return bitmapRef;
  }

  /**
   * Creates a bitmap with the specified width and height.  Its
   * initial density is determined from the given DisplayMetrics.
   *
   * @param display  Display metrics for the display this bitmap will be
   *                 drawn on.
   * @param width    The width of the bitmap
   * @param height   The height of the bitmap
   * @param config   The bitmap config to create.
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      DisplayMetrics display,
      int width,
      int height,
      Bitmap.Config config) {
    return createBitmap(display, width, height, config, null);
  }

  /**
   * Creates a bitmap with the specified width and height.  Its
   * initial density is determined from the given DisplayMetrics.
   *
   * @param display  Display metrics for the display this bitmap will be
   *                 drawn on.
   * @param width    The width of the bitmap
   * @param height   The height of the bitmap
   * @param config   The bitmap config to create.
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      DisplayMetrics display,
      int width,
      int height,
      Bitmap.Config config,
      @Nullable Object callerContext) {
    return createBitmap(display, width, height, config, true, callerContext);
  }

  /**
   * Creates a bitmap with the specified width and height.
   *
   * @param width    The width of the bitmap
   * @param height   The height of the bitmap
   * @param config   The bitmap config to create.
   * @param hasAlpha If the bitmap is ARGB_8888 this flag can be used to mark the
   *                 bitmap as opaque. Doing so will clear the bitmap in black
   *                 instead of transparent.
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  private CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config config,
      boolean hasAlpha) {
    return createBitmap(width, height, config, hasAlpha, null);
  }

  /**
   * Creates a bitmap with the specified width and height.
   *
   * @param width    The width of the bitmap
   * @param height   The height of the bitmap
   * @param config   The bitmap config to create.
   * @param hasAlpha If the bitmap is ARGB_8888 this flag can be used to mark the
   *                 bitmap as opaque. Doing so will clear the bitmap in black
   *                 instead of transparent.
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  private CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config config,
      boolean hasAlpha,
      @Nullable Object callerContext) {
    return createBitmap(null, width, height, config, hasAlpha, callerContext);
  }

  /**
   * Creates a bitmap with the specified width and height.  Its initial density is
   * determined from the given DisplayMetrics.
   *
   * @param display Display metrics for the display this bitmap will be drawn on
   * @param width The width of the bitmap
   * @param height The height of the bitmap
   * @param config The bitmap config to create
   * @param hasAlpha If the bitmap is ARGB_8888 this flag can be used to mark the bitmap as opaque
   * Doing so will clear the bitmap in black instead of transparent
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  private CloseableReference<Bitmap> createBitmap(
      DisplayMetrics display,
      int width,
      int height,
      Bitmap.Config config,
      boolean hasAlpha) {
    return createBitmap(display, width, height, config, hasAlpha, null);
  }

  /**
   * Creates a bitmap with the specified width and height.  Its initial density is
   * determined from the given DisplayMetrics.
   *
   * @param display Display metrics for the display this bitmap will be drawn on
   * @param width The width of the bitmap
   * @param height The height of the bitmap
   * @param config The bitmap config to create
   * @param hasAlpha If the bitmap is ARGB_8888 this flag can be used to mark the bitmap as opaque
   * Doing so will clear the bitmap in black instead of transparent
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  private CloseableReference<Bitmap> createBitmap(
      DisplayMetrics display,
      int width,
      int height,
      Bitmap.Config config,
      boolean hasAlpha,
      @Nullable Object callerContext) {
    checkWidthHeight(width, height);
    CloseableReference<Bitmap> bitmapRef = createBitmapInternal(width, height, config);

    Bitmap bitmap = bitmapRef.get();
    if (display != null) {
      bitmap.setDensity(display.densityDpi);
    }

    if (Build.VERSION.SDK_INT >= 12) {
      bitmap.setHasAlpha(hasAlpha);
    }

    if (config == Bitmap.Config.ARGB_8888 && !hasAlpha) {
      bitmap.eraseColor(0xff000000);
    }

    return bitmapRef;
  }

  /**
   * Creates a bitmap with the specified width and height.  Its initial density is
   * determined from the given DisplayMetrics.
   *
   * @param colors The colors to write to the bitmap
   * @param width The width of the bitmap
   * @param height The height of the bitmap
   * @param config The bitmap config to create
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      int[] colors,
      int width,
      int height,
      Bitmap.Config config) {
    return createBitmap(colors, width, height, config, null);
  }

  /**
   * Creates a bitmap with the specified width and height.  Its initial density is
   * determined from the given DisplayMetrics.
   *
   * @param colors The colors to write to the bitmap
   * @param width The width of the bitmap
   * @param height The height of the bitmap
   * @param config The bitmap config to create
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      int[] colors,
      int width,
      int height,
      Bitmap.Config config,
      @Nullable Object callerContext) {
    CloseableReference<Bitmap> bitmapRef = createBitmapInternal(width, height, config);
    Bitmap bitmap = bitmapRef.get();
    bitmap.setPixels(colors, 0, width, 0, 0, width, height);
    return bitmapRef;
  }

  /**
   * Creates a bitmap with the specified width and height.  Its initial density is
   * determined from the given DisplayMetrics.
   *
   * @param display Display metrics for the display this bitmap will be drawn on
   * @param colors The colors to write to the bitmap
   * @param width The width of the bitmap
   * @param height The height of the bitmap
   * @param config The bitmap config to create
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      DisplayMetrics display,
      int[] colors,
      int width,
      int height,
      Bitmap.Config config) {
    return createBitmap(display, colors, width, height, config, null);
  }

  /**
   * Creates a bitmap with the specified width and height.  Its initial density is
   * determined from the given DisplayMetrics.
   *
   * @param display Display metrics for the display this bitmap will be drawn on
   * @param colors The colors to write to the bitmap
   * @param width The width of the bitmap
   * @param height The height of the bitmap
   * @param config The bitmap config to create
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      DisplayMetrics display,
      int[] colors,
      int width,
      int height,
      Bitmap.Config config,
      @Nullable Object callerContext) {
    // Set the stride as the width of the image
    return createBitmap(display, colors, 0, width, width, height, config, callerContext);
  }

  /**
   * Creates a bitmap with the specified width and height.  Its initial density is
   * determined from the given DisplayMetrics.
   *
   * @param display Display metrics for the display this bitmap will be drawn on
   * @param colors The colors to write to the bitmap
   * @param offset The index of the first color to read from colors[]
   * @param stride The number of colors in pixels[] to skip between rows.
   * @param width The width of the bitmap
   * @param height The height of the bitmap
   * @param config The bitmap config to create
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      DisplayMetrics display,
      int[] colors,
      int offset,
      int stride,
      int width,
      int height,
      Bitmap.Config config) {
    return createBitmap(display, colors, offset, stride, width, height, config, null);
  }

  /**
   * Creates a bitmap with the specified width and height.  Its initial density is
   * determined from the given DisplayMetrics.
   *
   * @param display Display metrics for the display this bitmap will be drawn on
   * @param colors The colors to write to the bitmap
   * @param offset The index of the first color to read from colors[]
   * @param stride The number of colors in pixels[] to skip between rows.
   * @param width The width of the bitmap
   * @param height The height of the bitmap
   * @param config The bitmap config to create
   * @param callerContext the Tag to track who create the Bitmap
   * @return a reference to the bitmap
   * @throws IllegalArgumentException if the width or height are <= 0
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public CloseableReference<Bitmap> createBitmap(
      DisplayMetrics display,
      int[] colors,
      int offset,
      int stride,
      int width,
      int height,
      Bitmap.Config config,
      @Nullable Object callerContext) {
    CloseableReference<Bitmap> bitmapRef = createBitmap(
        display,
        width,
        height,
        config,
        callerContext);
    Bitmap bitmap = bitmapRef.get();
    bitmap.setPixels(colors, offset, stride, 0, 0, width, height);
    return bitmapRef;
  }

  /**
   * Returns suitable Bitmap Config for the new Bitmap based on the source Bitmap configurations.
   *
   * @param source the source Bitmap
   * @return the Bitmap Config for the new Bitmap
   */
  private static Bitmap.Config getSuitableBitmapConfig(Bitmap source) {
    Bitmap.Config finalConfig = Bitmap.Config.ARGB_8888;
    final Bitmap.Config sourceConfig = source.getConfig();

    // GIF files generate null configs, assume ARGB_8888
    if (sourceConfig != null) {
      switch (sourceConfig) {
        case RGB_565:
          finalConfig = Bitmap.Config.RGB_565;
          break;
        case ALPHA_8:
          finalConfig = Bitmap.Config.ALPHA_8;
          break;
        case ARGB_4444:
        case ARGB_8888:
        default:
          finalConfig = Bitmap.Config.ARGB_8888;
          break;
      }
    }
    return finalConfig;
  }

  /**
   * Common code for checking that width and height are > 0
   *
   * @param width width to ensure is > 0
   * @param height height to ensure is > 0
   */
  private static void checkWidthHeight(int width, int height) {
    Preconditions.checkArgument(width > 0, "width must be > 0");
    Preconditions.checkArgument(height > 0, "height must be > 0");
  }

  /**
   * Common code for checking that x and y are >= 0
   *
   * @param x x coordinate to ensure is >= 0
   * @param y y coordinate to ensure is >= 0
   */
  private static void checkXYSign(int x, int y) {
    Preconditions.checkArgument(x >= 0, "x must be >= 0");
    Preconditions.checkArgument(y >= 0, "y must be >= 0");
  }

  /**
   * Common code for checking that x + width and y + height are within image bounds
   *
   * @param source the source Bitmap
   * @param x starting x coordinate of source image subset
   * @param y starting y coordinate of source image subset
   * @param width width of the source image subset
   * @param height height of the source image subset
   */
  private static void checkFinalImageBounds(Bitmap source, int x, int y, int width, int height) {
    Preconditions.checkArgument(
        x + width <= source.getWidth(),
        "x + width must be <= bitmap.width()");
    Preconditions.checkArgument(
        y + height <= source.getHeight(),
        "y + height must be <= bitmap.height()");
  }

  /**
   * Set some property of the source bitmap to the destination bitmap
   *
   * @param source the source bitmap
   * @param destination the destination bitmap
   */
  private static void setPropertyFromSourceBitmap(Bitmap source, Bitmap destination) {
    // The new bitmap was created from a known bitmap source so assume that
    // they use the same density
    destination.setDensity(source.getDensity());
    if (Build.VERSION.SDK_INT >= 12) {
      destination.setHasAlpha(source.hasAlpha());
    }

    if (Build.VERSION.SDK_INT >= 19) {
      destination.setPremultiplied(source.isPremultiplied());
    }
  }

  /**
   * Creates a bitmap of the specified width and height. This is intended for ImagePipeline's
   * internal use only.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the Bitmap.Config used to create the Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  public abstract CloseableReference<Bitmap> createBitmapInternal(
      int width,
      int height,
      Bitmap.Config bitmapConfig);

}
