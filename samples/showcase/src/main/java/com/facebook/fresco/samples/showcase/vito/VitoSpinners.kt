/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import androidx.core.content.ContextCompat
import com.facebook.drawee.drawable.AutoRotateDrawable
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.common.CustomScaleTypes
import com.facebook.fresco.samples.showcase.postprocessor.CachedWatermarkPostprocessor
import com.facebook.fresco.samples.showcase.postprocessor.FasterGreyScalePostprocessor
import com.facebook.fresco.samples.showcase.postprocessor.ScalingBlurPostprocessor
import com.facebook.fresco.samples.showcase.postprocessor.SlowGreyScalePostprocessor
import com.facebook.fresco.samples.showcase.postprocessor.WatermarkPostprocessor
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.postprocessors.IterativeBoxBlurPostProcessor
import com.facebook.imagepipeline.postprocessors.RoundAsCirclePostprocessor
import com.facebook.imagepipeline.postprocessors.RoundPostprocessor

object VitoSpinners {

    val roundingOptions = Pair(listOf(
            "none" to null,
            "as circle" to RoundingOptions.asCircle(),
            "corner radius" to RoundingOptions.forCornerRadiusPx(20f),
            "different radii" to RoundingOptions.forCornerRadii(0f, 20f, 30f, 40f)
    ), "Rounding")

    val borderOptions = Pair(listOf(
            "none" to null,
            "red border" to BorderOptions.create(Color.RED, 20f),
            "blue border" to BorderOptions.create(Color.BLUE, 40f),
            "border with no padding" to BorderOptions.create(Color.GREEN, 20f, 0f),
            "border with small padding" to BorderOptions.create(Color.GREEN, 20f, 10f),
            "border with same padding" to BorderOptions.create(Color.GREEN, 20f, 20f),
            "border with more padding" to BorderOptions.create(Color.GREEN, 20f, 40f)
    ), "Border")

    val scaleTypes = Pair(listOf(
            "center" to Pair(ScalingUtils.ScaleType.CENTER, null),
            "center_crop" to Pair(ScalingUtils.ScaleType.CENTER_CROP, null),
            "center_inside" to Pair(ScalingUtils.ScaleType.CENTER_INSIDE, null),
            "fit_center" to Pair(ScalingUtils.ScaleType.FIT_CENTER, null),
            "fit_start" to Pair(ScalingUtils.ScaleType.FIT_START, null),
            "fit_end" to Pair(ScalingUtils.ScaleType.FIT_END, null),
            "fit_xy" to Pair(ScalingUtils.ScaleType.FIT_XY, null),
            "focus_crop (0, 0)" to Pair(ScalingUtils.ScaleType.FOCUS_CROP, PointF(0f, 0f)),
            "focus_crop (1, 0.5)" to Pair(ScalingUtils.ScaleType.FOCUS_CROP, PointF(1f, 0.5f)),
            "custom: fit_x" to Pair(CustomScaleTypes.FIT_X, null),
            "custom: fit_y" to Pair(CustomScaleTypes.FIT_Y, null)
    ), "Scale type")

    val colorFilters = Pair(listOf(
            "none" to PorterDuffColorFilter(Color.TRANSPARENT, PorterDuff.Mode.ADD),
            "red" to PorterDuffColorFilter(Color.RED, PorterDuff.Mode.DARKEN),
            "green" to PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.DARKEN),
            "blue" to PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.DARKEN)
    ), "Color filter")

    val fadingOptions = Pair(listOf(
            "none" to 0,
            "50 ms" to 50,
            "100 ms" to 100,
            "200 ms" to 200,
            "500 ms" to 500,
            "1000 ms" to 1000,
            "2000 ms" to 2000,
            "5000 ms" to 5000,
            "10000 ms" to 10000
    ), "Fading")

    val placeholderOptions = Pair(listOf(
            "none" to {builder: ImageOptions.Builder -> builder.placeholder(null)},
            "image" to {builder: ImageOptions.Builder -> builder.placeholderRes(R.drawable.logo, ScalingUtils.ScaleType.FIT_CENTER)},
            "block color" to {builder: ImageOptions.Builder -> builder.placeholder(ColorDrawable(Color.RED))},
            "color res" to {builder: ImageOptions.Builder -> builder.placeholderRes(R.color.placeholder_color)}
    ), "Placeholder")

    val progressOptions = Pair(listOf(
            "none" to {_, builder: ImageOptions.Builder -> builder.progress(null)},
            "image" to {_, builder: ImageOptions.Builder -> builder.progressRes(R.drawable.logo)},
            "color drawable" to {_, builder: ImageOptions.Builder -> builder.progress(ColorDrawable(Color.YELLOW))},
            "color res" to {_, builder: ImageOptions.Builder -> builder.progressRes(R.color.progress_bar_color)},
            "indeterminate" to {_, builder: ImageOptions.Builder -> builder.progress(AutoRotateDrawable(InsetDrawable(ColorDrawable(Color.BLUE), 50), 1000))},
            "progress bar" to { c: Context, builder: ImageOptions.Builder ->
                builder.progress(ProgressBarDrawable().apply {
                    color = ContextCompat.getColor(c, R.color.progress_bar_color)
                    backgroundColor = ContextCompat.getColor(c, R.color.progress_bar_background)
                })
            }
    ), "Progress")

    val overlayOptions = Pair(listOf(
            "none" to {builder: ImageOptions.Builder -> builder.overlayRes(0)},
            "logo" to {builder: ImageOptions.Builder -> builder.overlayRes(R.drawable.logo)},
            "orange color res" to {builder: ImageOptions.Builder -> builder.overlayRes(R.color.overlay_color)},
            "9-patch" to {builder: ImageOptions.Builder -> builder.overlayRes(R.drawable.ninepatch)}
    ), "Overlay")

    val postprocessorOptions = Pair(listOf(
            "none" to {builder: ImageOptions.Builder -> builder.postprocess(null)},
            "Grey Scale Post-Processor(Slow)" to {builder: ImageOptions.Builder -> builder.postprocess(SlowGreyScalePostprocessor())},
            "Grey Scale Post-Processor" to {builder: ImageOptions.Builder -> builder.postprocess(FasterGreyScalePostprocessor())},
            "Watermark Post-Processor" to {builder: ImageOptions.Builder -> builder.postprocess(WatermarkPostprocessor(10, "FRESCO"))},
            "Watermark Post-Processor(Cached)" to {builder: ImageOptions.Builder -> builder.postprocess(CachedWatermarkPostprocessor(10, "FRESCO"))},
            "Native Blur Post-Processor" to {builder: ImageOptions.Builder -> builder.postprocess(IterativeBoxBlurPostProcessor(25, 3))},
            "Scaling Blur Post-Processor" to {builder: ImageOptions.Builder -> builder.postprocess(ScalingBlurPostprocessor(25, 3, 4))},
            "Native Round As Circle Postprocessor" to {builder: ImageOptions.Builder -> builder.postprocess(RoundAsCirclePostprocessor(false))},
            "Antialiased As Circle Postprocessor" to {builder: ImageOptions.Builder -> builder.postprocess(RoundAsCirclePostprocessor(true))},
            "Round As Circle Postprocessor" to {builder: ImageOptions.Builder -> builder.postprocess(RoundPostprocessor())}
    ), "Postprocessor")

    val rotationOptions = Pair(listOf(
      "disabled" to RotationOptions.disableRotation(),
      "auto rotate" to RotationOptions.autoRotate(),
      "auto rotate at render time" to RotationOptions.autoRotateAtRenderTime(),
      "no rotation" to RotationOptions.forceRotation(RotationOptions.NO_ROTATION),
      "rotate 90" to RotationOptions.forceRotation(RotationOptions.ROTATE_90),
      "rotate 180" to RotationOptions.forceRotation(RotationOptions.ROTATE_180),
      "rotate 270" to RotationOptions.forceRotation(RotationOptions.ROTATE_270)
    ), "Rotation")
}
