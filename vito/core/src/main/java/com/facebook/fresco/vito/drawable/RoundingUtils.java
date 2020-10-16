/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import com.facebook.drawee.drawable.Rounded;
import com.facebook.drawee.drawable.RoundedBitmapDrawable;
import com.facebook.drawee.drawable.RoundedColorDrawable;
import com.facebook.drawee.drawable.RoundedNinePatchDrawable;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.infer.annotation.Nullsafe;

import javax.annotation.Nullable;

/**
 * A class that contains helper methods for rounding a bitmap or different kind of Drawables. It
 * handles the conversion to the specific types of drawables.
 *
 * <p>Different combinations are:<br>
 * - {@link Bitmap} -> already rounded -> no border -> circular -> {@link BitmapDrawable}<br>
 * - {@link Bitmap} -> already rounded -> border -> circular -> {@link CircularBorderBitmapDrawable}
 * <br>
 * - {@link Bitmap} -> already rounder or not -> rounded corners -> {@link RoundedBitmapDrawable}
 * <br>
 * - {@link BitmapDrawable} -> {@link RoundedBitmapDrawable}<br>
 * - {@link ColorDrawable} -> {@link RoundedColorDrawable}<br>
 * - {@link NinePatchDrawable} -> {@link RoundedNinePatchDrawable}<br>
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class RoundingUtils {

  private boolean mAlreadyRounded;

  public RoundingUtils() {
    this(false);
  }

  public RoundingUtils(boolean alreadyRounded) {
    mAlreadyRounded = alreadyRounded;
  }

  public void setAlreadyRounded(boolean alreadyRounded) {
    mAlreadyRounded = alreadyRounded;
  }

  /**
   * Creates a drawable with the {@link RoundingOptions} and {@link BorderOptions} applied to it.
   *
   * @param bitmap a bitmap to be wrapped in the final {@link BitmapDrawable}
   * @param borderOptions border options for the given image
   * @param roundingOptions rounding options for the given image
   * @return a drawable with the applied effect
   */
  public Drawable roundedDrawable(
      Resources resources,
      Bitmap bitmap,
      @Nullable BorderOptions borderOptions,
      @Nullable RoundingOptions roundingOptions) {
    if (borderOptions != null && borderOptions.width > 0) {
      return roundedDrawableWithBorder(resources, bitmap, borderOptions, roundingOptions);
    } else {
      return roundedDrawableWithoutBorder(resources, bitmap, roundingOptions);
    }
  }

  /**
   * Creates a drawable with the {@link RoundingOptions} and {@link BorderOptions} applied to it.
   *
   * @param drawable the image to transform
   * @param borderOptions border options for the given image
   * @param roundingOptions rounding options for the given image
   * @return a drawable with the applied effect
   */
  public Drawable roundedDrawable(
      Resources resources,
      Drawable drawable,
      @Nullable BorderOptions borderOptions,
      @Nullable RoundingOptions roundingOptions) {
    if (borderOptions != null && borderOptions.width > 0) {
      return roundedDrawableWithBorder(resources, drawable, borderOptions, roundingOptions);
    } else {
      return roundedDrawableWithoutBorder(resources, drawable, roundingOptions);
    }
  }

  private Drawable roundedDrawableWithoutBorder(
      Resources resources, Bitmap bitmap, @Nullable RoundingOptions roundingOptions) {
    if ((roundingOptions == null) || (mAlreadyRounded && roundingOptions.isCircular())) {
      return new BitmapDrawable(resources, bitmap);
    } else {
      return applyRounding(getRoundedDrawable(resources, bitmap), null, roundingOptions);
    }
  }

  private Drawable roundedDrawableWithBorder(
      Resources resources,
      Bitmap bitmap,
      BorderOptions borderOptions,
      @Nullable RoundingOptions roundingOptions) {
    if (roundingOptions == null) {
      return squareDrawableWithBorder(getRoundedDrawable(resources, bitmap), borderOptions);
    } else {
      if (mAlreadyRounded && roundingOptions.isCircular()) {
        // Circular rounding is performed on the bitmap, so we only need to draw a circular border
        return circularNativeDrawableWithBorder(resources, bitmap, borderOptions);
      } else {
        return applyRounding(getRoundedDrawable(resources, bitmap), borderOptions, roundingOptions);
      }
    }
  }

  private Drawable roundedDrawableWithoutBorder(
      Resources resources, Drawable drawable, @Nullable RoundingOptions roundingOptions) {
    if (roundingOptions != null) {
      return applyRounding(getRoundedDrawable(resources, drawable), null, roundingOptions);
    }
    return drawable;
  }

  private Drawable roundedDrawableWithBorder(
      Resources resources,
      Drawable drawable,
      BorderOptions borderOptions,
      @Nullable RoundingOptions roundingOptions) {
    if (roundingOptions == null) {
      return squareDrawableWithBorder(getRoundedDrawable(resources, drawable), borderOptions);
    } else {
      if (mAlreadyRounded && roundingOptions.isCircular() && drawable instanceof BitmapDrawable) {
        // Circular rounding is performed on the bitmap, so we only need to draw a circular border
        return circularNativeDrawableWithBorder(
            resources, ((BitmapDrawable) drawable).getBitmap(), borderOptions);
      } else {
        return applyRounding(
            getRoundedDrawable(resources, drawable), borderOptions, roundingOptions);
      }
    }
  }

  private static <T extends Drawable & Rounded> T getRoundedDrawable(
      Resources resources, @Nullable Bitmap bitmap) {
    return (T) new RoundedBitmapDrawable(resources, bitmap);
  }

  private <T extends Drawable & Rounded> T getRoundedDrawable(
      Resources resources, Drawable drawable) {
    T roundingDrawable;
    if (drawable instanceof BitmapDrawable) {
      roundingDrawable = getRoundedDrawable(resources, ((BitmapDrawable) drawable).getBitmap());
    } else if (drawable instanceof NinePatchDrawable) {
      roundingDrawable = (T) new RoundedNinePatchDrawable((NinePatchDrawable) drawable);
    } else if (drawable instanceof ColorDrawable) {
      roundingDrawable = (T) RoundedColorDrawable.fromColorDrawable((ColorDrawable) drawable);
    } else {
      throw new UnsupportedOperationException(
          "Rounding of the drawable type not supported: " + drawable);
    }
    return roundingDrawable;
  }

  private <T extends Drawable & Rounded> Drawable applyRounding(
      T drawable, @Nullable BorderOptions borderOptions, RoundingOptions roundingOptions) {
    if (!roundingOptions.isCircular()) {
      return roundedCornerDrawable(drawable, borderOptions, roundingOptions);
    } else {
      return circularDrawable(drawable, borderOptions);
    }
  }

  private <T extends Drawable & Rounded> Drawable squareDrawableWithBorder(
      T drawable, BorderOptions borderOptions) {
    // We use the same rounded corner drawable to draw the border without applying rounding
    return roundedCornerDrawable(drawable, borderOptions, null);
  }

  private static Drawable circularNativeDrawableWithBorder(
      Resources resources, @Nullable Bitmap bitmap, BorderOptions borderOptions) {
    CircularBorderBitmapDrawable drawable = new CircularBorderBitmapDrawable(resources, bitmap);
    drawable.setBorder(borderOptions);
    return drawable;
  }

  private static <T extends Drawable & Rounded> Drawable circularDrawable(
      T drawable, @Nullable BorderOptions borderOptions) {
    drawable.setCircle(true);
    if (borderOptions != null) {
      applyBorders(drawable, borderOptions);
    }
    return drawable;
  }

  private static <T extends Drawable & Rounded> Drawable roundedCornerDrawable(
      T drawable,
      @Nullable BorderOptions borderOptions,
      @Nullable RoundingOptions roundingOptions) {
    if (borderOptions != null) {
      applyBorders(drawable, borderOptions);
    }
    if (roundingOptions != null) {
      float[] radii = roundingOptions.getCornerRadii();
      if (radii != null) {
        drawable.setRadii(radii);
      } else {
        drawable.setRadius(roundingOptions.getCornerRadius());
      }
    }
    return drawable;
  }

  /**
   * Applies the border according to {@link BorderOptions}
   *
   * @param drawable the drawable where the borders are applied
   * @param borderOptions {@link BorderOptions}
   */
  private static <T extends Drawable & Rounded> void applyBorders(
      T drawable, BorderOptions borderOptions) {
    drawable.setBorder(borderOptions.color, borderOptions.width);
    drawable.setPadding(borderOptions.padding);
  }
}
