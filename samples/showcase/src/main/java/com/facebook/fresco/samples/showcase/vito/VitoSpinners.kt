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
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.fresco.samples.showcase.common.CustomScaleTypes
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.options.RoundingOptions

object VitoSpinners {
    val roundingOptions = listOf(
            "no rounding" to null,
            "as circle" to RoundingOptions.asCircle(),
            "corner radius" to RoundingOptions.forCornerRadius(20f),
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
}
