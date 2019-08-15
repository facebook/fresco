/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.fresco.samples.showcase.vito

import android.graphics.Color
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.common.CustomScaleTypes
import com.facebook.fresco.samples.showcase.imageformat.keyframes.KeyframesDecoderExample
import com.facebook.fresco.samples.showcase.postprocessor.CachedWatermarkPostprocessor
import com.facebook.fresco.samples.showcase.postprocessor.FasterGreyScalePostprocessor
import com.facebook.fresco.samples.showcase.postprocessor.ScalingBlurPostprocessor
import com.facebook.fresco.samples.showcase.postprocessor.SlowGreyScalePostprocessor
import com.facebook.fresco.samples.showcase.postprocessor.WatermarkPostprocessor
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.postprocessors.IterativeBoxBlurPostProcessor
import com.facebook.imagepipeline.postprocessors.RoundAsCirclePostprocessor
import com.facebook.imagepipeline.postprocessors.RoundPostprocessor

object VitoSpinners {

    val roundingOptions = listOf(
            "no rounding" to null,
            "as circle" to RoundingOptions.asCircle(),
            "corner radius" to RoundingOptions.forCornerRadiusPx(20f),
            "different radii" to RoundingOptions.forCornerRadii(0f, 20f, 30f, 40f))

    val borderOptions = listOf(
            "no border" to null,
            "red border" to BorderOptions.create(Color.RED, 20f),
            "blue border" to BorderOptions.create(Color.BLUE, 40f))

    val scaleTypes = listOf(
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
            "custom: fit_y" to Pair(CustomScaleTypes.FIT_Y, null))

    val imageFormats = listOf(
            "JPEG" to DefaultImageFormats.JPEG,
            "PNG" to DefaultImageFormats.PNG,
            "Animated GIF" to DefaultImageFormats.GIF,
            "WebP simple" to DefaultImageFormats.WEBP_SIMPLE,
            "WebP with alpha" to DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA,
            "Animated WebP" to DefaultImageFormats.WEBP_ANIMATED,
            "Keyframes" to KeyframesDecoderExample.IMAGE_FORMAT_KEYFRAMES
    )

    val colorFilters = listOf(
            "none" to PorterDuffColorFilter(Color.TRANSPARENT, PorterDuff.Mode.ADD),
            "red" to PorterDuffColorFilter(Color.RED, PorterDuff.Mode.DARKEN),
            "green" to PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.DARKEN),
            "blue" to PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.DARKEN)
    )

    val placeholderOptions = listOf(
            "none" to {builder: ImageOptions.Builder -> builder.placeholder(null)},
            "image" to {builder: ImageOptions.Builder -> builder.placeholderRes(R.drawable.logo, ScalingUtils.ScaleType.FIT_CENTER)},
            "block color" to {builder: ImageOptions.Builder -> builder.placeholder(ColorDrawable(Color.RED))}
    )

    val postprocessorOptions = listOf(
            "Grey Scale Post-Processor(Slow)" to {builder: ImageOptions.Builder -> builder.postprocess(SlowGreyScalePostprocessor())},
            "Grey Scale Post-Processor" to {builder: ImageOptions.Builder -> builder.postprocess(FasterGreyScalePostprocessor())},
            "Watermark Post-Processor" to {builder: ImageOptions.Builder -> builder.postprocess(WatermarkPostprocessor(10, "FRESCO"))},
            "Watermark Post-Processor(Cached)" to {builder: ImageOptions.Builder -> builder.postprocess(CachedWatermarkPostprocessor(10, "FRESCO"))},
            "Native Blur Post-Processor" to {builder: ImageOptions.Builder -> builder.postprocess(IterativeBoxBlurPostProcessor(25, 3))},
            "Scaling Blur Post-Processor" to {builder: ImageOptions.Builder -> builder.postprocess(ScalingBlurPostprocessor(25, 3, 4))},
            "Native Round As Circle Postprocessor" to {builder: ImageOptions.Builder -> builder.postprocess(RoundAsCirclePostprocessor(false))},
            "Antialiased As Circle Postprocessor" to {builder: ImageOptions.Builder -> builder.postprocess(RoundAsCirclePostprocessor(true))},
            "Round As Circle Postprocessor" to {builder: ImageOptions.Builder -> builder.postprocess(RoundPostprocessor())}
    )

    val rotationOptions = listOf(
      "disable rotation" to RotationOptions.disableRotation(),
      "auto rotate" to RotationOptions.autoRotate(),
      "auto rotate at render time" to RotationOptions.autoRotateAtRenderTime(),
      "no rotation" to RotationOptions.forceRotation(RotationOptions.NO_ROTATION),
      "rotate 90" to RotationOptions.forceRotation(RotationOptions.ROTATE_90),
      "rotate 180" to RotationOptions.forceRotation(RotationOptions.ROTATE_180),
      "rotate 270" to RotationOptions.forceRotation(RotationOptions.ROTATE_270)
    )
}
