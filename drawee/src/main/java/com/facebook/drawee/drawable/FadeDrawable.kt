/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.facebook.fresco.ui.common.OnFadeListener
import java.util.Arrays
import kotlin.math.ceil

/**
 * A drawable that fades to the specific layer.
 *
 * Arbitrary number of layers is supported. 5 Different fade methods are supported. Once the
 * transition starts we will animate layers in or out based on used fade method. fadeInLayer fades
 * in specified layer to full opacity. fadeOutLayer fades out specified layer to zero opacity.
 * fadeOutAllLayers fades out all layers to zero opacity. fadeToLayer fades in specified layer to
 * full opacity, fades out all other layers to zero opacity. fadeUpToLayer fades in all layers up to
 * specified layer to full opacity and fades out all other layers to zero opacity.
 */
open class FadeDrawable
@JvmOverloads
constructor(
    layers: Array<out Drawable?>,
    allLayersVisible: Boolean = false,
    actualImageLayer: Int = -1,
) : ArrayDrawable(layers) {

  /** Layers. */
  private val layers: Array<out Drawable?>

  val isDefaultLayerIsOn: Boolean
  private val defaultLayerAlpha: Int

  /* The index of the layer that contains the actual image */
  private val actualImageLayer: Int

  /**
   * Gets the transition state (STARTING, RUNNING, NONE). Useful for testing purposes.
   *
   * @return transition state
   */
  /** The current state. */
  @JvmField @VisibleForTesting var mTransitionState: Int = 0

  @JvmField @VisibleForTesting var mDurationMs: Int = 0

  @JvmField @VisibleForTesting var mStartTimeMs: Long = 0

  @JvmField @VisibleForTesting var mStartAlphas: IntArray

  @JvmField @VisibleForTesting var mAlphas: IntArray

  @VisibleForTesting var mAlpha: Int

  /**
   * Determines whether to fade-out a layer to zero opacity (false) or to fade-in to the full
   * opacity (true)
   */
  @JvmField @VisibleForTesting var mIsLayerOn: BooleanArray

  /** When in batch mode, drawable won't invalidate self until batch mode finishes. */
  @JvmField @VisibleForTesting var mPreventInvalidateCount: Int

  private var onFadeListener: OnFadeListener? = null
  private var isFadingActualImage = false
  private var onFadeListenerShowImmediately = false
  private var mutateDrawables = true

  /**
   * Creates a new fade drawable. The first layer is displayed with full opacity whereas all other
   * layers are invisible if allLayersVisible is false. Otherwise, all layers will be displayed with
   * full opacity.
   *
   * @param layers layers to fade between
   * @param allLayersVisible true if all layers should be visible per default
   * @param actualImageLayer The index of the layer that contains the actual image
   */
  override fun invalidateSelf() {
    if (mPreventInvalidateCount == 0) {
      super.invalidateSelf()
    }
  }

  /** Begins the batch mode so that it doesn't invalidate self on every operation. */
  fun beginBatchMode() {
    mPreventInvalidateCount++
  }

  /** Ends the batch mode and invalidates. */
  fun endBatchMode() {
    mPreventInvalidateCount--
    invalidateSelf()
  }

  var transitionDuration: Int
    /**
     * Gets the transition duration.
     *
     * @return transition duration in milliseconds.
     */
    get() = mDurationMs
    /** Sets the duration of the current transition in milliseconds. */
    set(durationMs) {
      this.mDurationMs = durationMs
      // re-initialize transition if it's running
      if (mTransitionState == TRANSITION_RUNNING) {
        mTransitionState = FadeDrawable.TRANSITION_STARTING
      }
    }

  /** Resets internal state to the initial state. */
  private fun resetInternal() {
    mTransitionState = FadeDrawable.TRANSITION_NONE
    Arrays.fill(mStartAlphas, defaultLayerAlpha)
    mStartAlphas[0] = 255
    Arrays.fill(mAlphas, defaultLayerAlpha)
    mAlphas[0] = 255
    Arrays.fill(mIsLayerOn, isDefaultLayerIsOn)
    mIsLayerOn[0] = true
  }

  /** Resets to the initial state. */
  open fun reset() {
    resetInternal()
    invalidateSelf()
  }

  /**
   * Starts fading in the specified layer.
   *
   * @param index the index of the layer to fade in.
   */
  fun fadeInLayer(index: Int) {
    mTransitionState = FadeDrawable.TRANSITION_STARTING
    mIsLayerOn[index] = true
    invalidateSelf()
  }

  /**
   * Starts fading out the specified layer.
   *
   * @param index the index of the layer to fade out.
   */
  fun fadeOutLayer(index: Int) {
    mTransitionState = FadeDrawable.TRANSITION_STARTING
    mIsLayerOn[index] = false
    invalidateSelf()
  }

  /** Starts fading in all layers. */
  fun fadeInAllLayers() {
    mTransitionState = FadeDrawable.TRANSITION_STARTING
    Arrays.fill(mIsLayerOn, true)
    invalidateSelf()
  }

  /** Starts fading out all layers. */
  fun fadeOutAllLayers() {
    mTransitionState = FadeDrawable.TRANSITION_STARTING
    Arrays.fill(mIsLayerOn, false)
    invalidateSelf()
  }

  /**
   * Starts fading to the specified layer.
   *
   * @param index the index of the layer to fade to
   */
  fun fadeToLayer(index: Int) {
    mTransitionState = FadeDrawable.TRANSITION_STARTING
    Arrays.fill(mIsLayerOn, false)
    mIsLayerOn[index] = true
    invalidateSelf()
  }

  /**
   * Starts fading up to the specified layer.
   *
   * Layers up to the specified layer inclusive will fade in, other layers will fade out.
   *
   * @param index the index of the layer to fade up to.
   */
  fun fadeUpToLayer(index: Int) {
    mTransitionState = FadeDrawable.TRANSITION_STARTING
    Arrays.fill(mIsLayerOn, 0, index + 1, true)
    Arrays.fill(mIsLayerOn, index + 1, layers.size, false)
    invalidateSelf()
  }

  /**
   * Makes the specified layer fully opaque
   *
   * @param index the index of the layer to be shown
   */
  fun showLayerImmediately(index: Int) {
    mIsLayerOn[index] = true
    mAlphas[index] = 255
    if (index == actualImageLayer) {
      onFadeListenerShowImmediately = true
    }
    invalidateSelf()
  }

  /**
   * Makes the specified layer fully transparent
   *
   * @param index the index of the layer to be hidden
   */
  fun hideLayerImmediately(index: Int) {
    mIsLayerOn[index] = false
    mAlphas[index] = 0
    invalidateSelf()
  }

  /** Finishes transition immediately. */
  fun finishTransitionImmediately() {
    mTransitionState = FadeDrawable.TRANSITION_NONE
    for (i in layers.indices) {
      mAlphas[i] = if (mIsLayerOn[i]) 255 else 0
    }
    invalidateSelf()
  }

  /**
   * Updates the current alphas based on the ratio of the elapsed time and duration.
   *
   * @param ratio
   * @return whether the all layers have reached their target opacity
   */
  private fun updateAlphas(ratio: Float): Boolean {
    var done = true
    for (i in layers.indices) {
      val dir = if (mIsLayerOn[i]) +1 else -1
      // determines alpha value and clamps it to [0, 255]
      mAlphas[i] = (mStartAlphas[i] + dir * 255 * ratio).toInt()
      if (mAlphas[i] < 0) {
        mAlphas[i] = 0
      }
      if (mAlphas[i] > 255) {
        mAlphas[i] = 255
      }
      // determines whether the layer has reached its target opacity
      if (mIsLayerOn[i] && mAlphas[i] < 255) {
        done = false
      }
      if (!mIsLayerOn[i] && mAlphas[i] > 0) {
        done = false
      }
    }
    return done
  }

  override fun draw(canvas: Canvas) {
    var done = true
    val ratio: Float

    when (mTransitionState) {
      TRANSITION_STARTING -> {
        // initialize start alphas and start time
        System.arraycopy(mAlphas, 0, mStartAlphas, 0, layers.size)
        mStartTimeMs = currentTimeMs
        // if the duration is 0, update alphas to the target opacities immediately
        ratio = if (mDurationMs == 0) 1.0f else 0.0f
        // if all the layers have reached their target opacity, transition is done
        done = updateAlphas(ratio)
        maybeOnFadeStarted()
        mTransitionState = if (done) TRANSITION_NONE else TRANSITION_RUNNING
      }

      TRANSITION_RUNNING -> {
        check(mDurationMs > 0)
        // determine ratio based on the elapsed time
        ratio = (currentTimeMs - mStartTimeMs).toFloat() / mDurationMs
        // if all the layers have reached their target opacity, transition is done
        done = updateAlphas(ratio)
        mTransitionState = if (done) TRANSITION_NONE else TRANSITION_RUNNING
      }

      TRANSITION_NONE -> // there is no transition in progress and mAlphas should be left as is.
      done = true
      else -> {}
    }

    for (i in layers.indices) {
      drawDrawableWithAlpha(
          canvas,
          layers[i],
          ceil(mAlphas[i] * mAlpha / 255.0).toInt(),
      )
    }

    if (done) {
      maybeOnFadeFinished()
      maybeOnImageShownImmediately()
    } else {
      invalidateSelf()
    }
  }

  private fun maybeOnImageShownImmediately() {
    if (!onFadeListenerShowImmediately) {
      return
    }

    if (mTransitionState == TRANSITION_NONE && mIsLayerOn[actualImageLayer]) {
      onFadeListener?.onShownImmediately()
      onFadeListenerShowImmediately = false
    }
  }

  private fun drawDrawableWithAlpha(canvas: Canvas, drawable: Drawable?, alpha: Int) {
    if (drawable != null && alpha > 0) {
      mPreventInvalidateCount++
      if (mutateDrawables) {
        drawable.mutate()
      }
      drawable.alpha = alpha
      mPreventInvalidateCount--
      drawable.draw(canvas)
    }
  }

  override fun setAlpha(alpha: Int) {
    if (this.mAlpha != alpha) {
      this.mAlpha = alpha
      invalidateSelf()
    }
  }

  override fun getAlpha(): Int = mAlpha

  protected open val currentTimeMs: Long
    /**
     * Returns current time. Absolute reference is not important as only time deltas are used.
     * Extracting this to a separate method allows better testing.
     *
     * @return current time in milliseconds
     */
    get() = SystemClock.uptimeMillis()

  /**
   * Creates a new fade drawable. The first layer is displayed with full opacity whereas all other
   * layers are invisible.
   *
   * @param layers layers to fade between
   */
  init {
    check(layers.size >= 1) { "At least one layer required!" }
    this.layers = layers
    mStartAlphas = IntArray(layers.size)
    mAlphas = IntArray(layers.size)
    mAlpha = 255
    mIsLayerOn = BooleanArray(layers.size)
    mPreventInvalidateCount = 0
    isDefaultLayerIsOn = allLayersVisible
    defaultLayerAlpha = if (isDefaultLayerIsOn) 255 else 0
    this.actualImageLayer = actualImageLayer
    resetInternal()
  }

  @VisibleForTesting fun getTransitionState(): Int = mTransitionState

  fun isLayerOn(index: Int): Boolean = mIsLayerOn[index]

  fun setOnFadeListener(onFadeListener: OnFadeListener?) {
    this.onFadeListener = onFadeListener
  }

  fun setMutateDrawables(mutateDrawables: Boolean) {
    this.mutateDrawables = mutateDrawables
  }

  private fun maybeOnFadeStarted() {
    if (isFadingActualImage) {
      return
    }

    if (actualImageLayer < 0 || actualImageLayer >= mIsLayerOn.size) {
      return
    }
    if (!mIsLayerOn[actualImageLayer]) {
      return
    }

    isFadingActualImage = true

    onFadeListener?.onFadeStarted()
  }

  private fun maybeOnFadeFinished() {
    if (!isFadingActualImage) {
      return
    }
    isFadingActualImage = false

    onFadeListener?.onFadeFinished()
  }

  companion object {
    /** A transition is about to start. */
    @VisibleForTesting const val TRANSITION_STARTING: Int = 0

    /** The transition has started and the animation is in progress. */
    @VisibleForTesting const val TRANSITION_RUNNING: Int = 1

    /** No transition will be applied. */
    @VisibleForTesting const val TRANSITION_NONE: Int = 2
  }
}
