/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.vito.view.transition

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionSet
import android.transition.TransitionValues
import android.view.ViewGroup
import android.widget.ImageView
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.drawable.ScalingUtils.InterpolatingScaleType
import com.facebook.fresco.vito.view.VitoView
import com.facebook.infer.annotation.Nullsafe

/**
 * This Transition animates changes of an [ImageView] using Vito between two ScaleTypes
 *
 * In combination with ChangeBounds, VitoTransition allows ImageViews that change size, shape, or
 * [ScalingUtils.ScaleType] to animate contents smoothly.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@TargetApi(Build.VERSION_CODES.KITKAT)
class VitoTransition
@JvmOverloads
constructor(
    private val callerContext: Any,
    private val fromScale: ScalingUtils.ScaleType,
    private val toScale: ScalingUtils.ScaleType,
    private val fromFocusPoint: PointF? = null,
    private val toFocusPoint: PointF? = null
) : Transition() {
  override fun captureStartValues(transitionValues: TransitionValues) {
    captureValues(transitionValues)
  }

  override fun captureEndValues(transitionValues: TransitionValues) {
    captureValues(transitionValues)
  }

  override fun createAnimator(
      sceneRoot: ViewGroup,
      startValues: TransitionValues?,
      endValues: TransitionValues?,
  ): Animator? {
    startValues ?: return null
    endValues ?: return null
    val startBounds = startValues.values[PROPNAME_BOUNDS] as Rect?
    val endBounds = endValues.values[PROPNAME_BOUNDS] as Rect?
    if (startBounds == null || endBounds == null) {
      return null
    }
    if (fromScale === toScale && fromFocusPoint === toFocusPoint) {
      return null
    }
    val imageView = startValues.view
    val scaleType =
        InterpolatingScaleType(
            fromScale, toScale, startBounds, endBounds, fromFocusPoint, toFocusPoint)
    val vitoDrawable = VitoView.getDrawable(imageView) ?: return null
    val originalVitoImageRequest = vitoDrawable.imageRequest ?: return null
    VitoView.show(
        originalVitoImageRequest.imageSource,
        originalVitoImageRequest.imageOptions.extend().scale(scaleType).build(),
        callerContext,
        imageView)

    val animator = ValueAnimator.ofFloat(0f, 1f)
    animator.addUpdateListener { animation ->
      val fraction = animation.animatedValue as Float
      scaleType.value = fraction
    }
    animator.addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            VitoView.show(
                originalVitoImageRequest.imageSource,
                originalVitoImageRequest.imageOptions
                    .extend()
                    .scale(toScale)
                    .focusPoint(toFocusPoint)
                    .build(),
                callerContext,
                imageView)
          }
        })

    return animator
  }

  private fun captureValues(transitionValues: TransitionValues) {
    if (transitionValues.view is ImageView) {
      transitionValues.values[PROPNAME_BOUNDS] =
          Rect(0, 0, transitionValues.view.width, transitionValues.view.height)
    }
  }

  companion object {
    private const val PROPNAME_BOUNDS = "vitoTransition:bounds"

    @JvmOverloads
    @JvmStatic
    fun createTransitionSet(
        callerContext: Any,
        fromScale: ScalingUtils.ScaleType,
        toScale: ScalingUtils.ScaleType,
        fromFocusPoint: PointF? = null,
        toFocusPoint: PointF? = null
    ): TransitionSet {
      val transitionSet = TransitionSet()
      transitionSet.addTransition(ChangeBounds())
      transitionSet.addTransition(
          VitoTransition(callerContext, fromScale, toScale, fromFocusPoint, toFocusPoint))
      return transitionSet
    }
  }
}
