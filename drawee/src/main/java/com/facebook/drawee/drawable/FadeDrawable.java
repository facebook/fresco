/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import java.util.Arrays;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;

/**
 * A drawable that fades to the specific layer.
 *
 * <p> Arbitrary number of layers is supported. 5 Different fade methods are supported.
 * Once the transition starts we will animate layers in or out based on used fade method.
 * fadeInLayer fades in specified layer to full opacity.
 * fadeOutLayer fades out specified layer to zero opacity.
 * fadeOutAllLayers fades out all layers to zero opacity.
 * fadeToLayer fades in specified layer to full opacity, fades out all other layers to zero opacity.
 * fadeUpToLayer fades in all layers up to specified layer to full opacity and
 * fades out all other layers to zero opacity.
 *
 */
public class FadeDrawable extends ArrayDrawable {

  /**
   * A transition is about to start.
   */
  @VisibleForTesting
  public static final int TRANSITION_STARTING = 0;

  /**
   * The transition has started and the animation is in progress.
   */
  @VisibleForTesting
  public static final int TRANSITION_RUNNING = 1;

  /**
   * No transition will be applied.
   */
  @VisibleForTesting
  public static final int TRANSITION_NONE = 2;

  /**
   * Layers.
   */
  private final Drawable[] mLayers;

  /**
   * The current state.
   */
  @VisibleForTesting int mTransitionState;
  @VisibleForTesting int mDurationMs;
  @VisibleForTesting long mStartTimeMs;
  @VisibleForTesting int[] mStartAlphas;
  @VisibleForTesting int[] mAlphas;
  @VisibleForTesting int mAlpha;

  /**
   * Determines whether to fade-out a layer to zero opacity (false) or to fade-in to
   * the full opacity (true)
   */
  @VisibleForTesting boolean[] mIsLayerOn;

  /**
   * When in batch mode, drawable won't invalidate self until batch mode finishes.
   */
  @VisibleForTesting int mPreventInvalidateCount;

  /**
   * Creates a new fade drawable.
   * The first layer is displayed with full opacity whereas all other layers are invisible.
   * @param layers layers to fade between
   */
  public FadeDrawable(Drawable[] layers) {
    super(layers);
    Preconditions.checkState(layers.length >= 1, "At least one layer required!");
    mLayers = layers;
    mStartAlphas = new int[layers.length];
    mAlphas = new int[layers.length];
    mAlpha = 255;
    mIsLayerOn = new boolean[layers.length];
    mPreventInvalidateCount = 0;
    resetInternal();
  }

  @Override
  public void invalidateSelf() {
    if (mPreventInvalidateCount == 0) {
      super.invalidateSelf();
    }
  }

  /**
   * Begins the batch mode so that it doesn't invalidate self on every operation.
   */
  public void beginBatchMode() {
    mPreventInvalidateCount++;
  }

  /**
   * Ends the batch mode and invalidates.
   */
  public void endBatchMode() {
    mPreventInvalidateCount--;
    invalidateSelf();
  }

  /**
   * Sets the duration of the current transition in milliseconds.
   */
  public void setTransitionDuration(int durationMs) {
    mDurationMs = durationMs;
    // re-initialize transition if it's running
    if (mTransitionState == TRANSITION_RUNNING) {
      mTransitionState = TRANSITION_STARTING;
    }
  }

  /**
   * Gets the transition duration.
   * @return transition duration in milliseconds.
   */
  public int getTransitionDuration() {
    return mDurationMs;
  }

  /**
   * Resets internal state to the initial state.
   */
  private void resetInternal() {
    mTransitionState = TRANSITION_NONE;
    Arrays.fill(mStartAlphas, 0);
    mStartAlphas[0] = 255;
    Arrays.fill(mAlphas, 0);
    mAlphas[0] = 255;
    Arrays.fill(mIsLayerOn, false);
    mIsLayerOn[0] = true;
  }

  /**
   * Resets to the initial state.
   */
  public void reset() {
    resetInternal();
    invalidateSelf();
  }

  /**
   * Starts fading in the specified layer.
   * @param index the index of the layer to fade in.
   */
  public void fadeInLayer(int index) {
    mTransitionState = TRANSITION_STARTING;
    mIsLayerOn[index] = true;
    invalidateSelf();
  }

  /**
   * Starts fading out the specified layer.
   * @param index the index of the layer to fade out.
   */
  public void fadeOutLayer(int index) {
    mTransitionState = TRANSITION_STARTING;
    mIsLayerOn[index] = false;
    invalidateSelf();
  }

  /**
   * Starts fading in all layers.
   */
  public void fadeInAllLayers() {
    mTransitionState = TRANSITION_STARTING;
    Arrays.fill(mIsLayerOn, true);
    invalidateSelf();
  }

  /**
   * Starts fading out all layers.
   */
  public void fadeOutAllLayers() {
    mTransitionState = TRANSITION_STARTING;
    Arrays.fill(mIsLayerOn, false);
    invalidateSelf();
  }

  /**
   * Starts fading to the specified layer.
   * @param index the index of the layer to fade to
   */
  public void fadeToLayer(int index) {
    mTransitionState = TRANSITION_STARTING;
    Arrays.fill(mIsLayerOn, false);
    mIsLayerOn[index] = true;
    invalidateSelf();
  }

  /**
   * Starts fading up to the specified layer.
   * <p>
   * Layers up to the specified layer inclusive will fade in, other layers will fade out.
   * @param index the index of the layer to fade up to.
   */
  public void fadeUpToLayer(int index) {
    mTransitionState = TRANSITION_STARTING;
    Arrays.fill(mIsLayerOn, 0, index + 1, true);
    Arrays.fill(mIsLayerOn, index + 1, mLayers.length, false);
    invalidateSelf();
  }

  /**
   * Finishes transition immediately.
   */
  public void finishTransitionImmediately() {
    mTransitionState = TRANSITION_NONE;
    for (int i = 0; i < mLayers.length; i++) {
      mAlphas[i] = mIsLayerOn[i] ? 255 : 0;
    }
    invalidateSelf();
  }

  /**
   * Updates the current alphas based on the ratio of the elapsed time and duration.
   * @param ratio
   * @return whether the all layers have reached their target opacity
   */
  private boolean updateAlphas(float ratio) {
    boolean done = true;
    for (int i = 0; i < mLayers.length; i++) {
      int dir = mIsLayerOn[i] ? +1 : -1;
      // determines alpha value and clamps it to [0, 255]
      mAlphas[i] = (int) (mStartAlphas[i] + dir * 255 * ratio);
      if (mAlphas[i] < 0) {
        mAlphas[i] = 0;
      }
      if (mAlphas[i] > 255) {
        mAlphas[i] = 255;
      }
      // determines whether the layer has reached its target opacity
      if (mIsLayerOn[i] && mAlphas[i] < 255) {
        done = false;
      }
      if (!mIsLayerOn[i] && mAlphas[i] > 0) {
        done = false;
      }
    }
    return done;
  }

  @Override
  public void draw(Canvas canvas) {
    boolean done = true;
    float ratio;

    switch (mTransitionState) {
      case TRANSITION_STARTING:
        // initialize start alphas and start time
        System.arraycopy(mAlphas, 0, mStartAlphas, 0, mLayers.length);
        mStartTimeMs = getCurrentTimeMs();
        // if the duration is 0, update alphas to the target opacities immediately
        ratio = (mDurationMs == 0) ? 1.0f : 0.0f;
        // if all the layers have reached their target opacity, transition is done
        done = updateAlphas(ratio);
        mTransitionState = done ? TRANSITION_NONE : TRANSITION_RUNNING;
        break;

      case TRANSITION_RUNNING:
        Preconditions.checkState(mDurationMs > 0);
        // determine ratio based on the elapsed time
        ratio = (float) (getCurrentTimeMs() - mStartTimeMs) / mDurationMs;
        // if all the layers have reached their target opacity, transition is done
        done = updateAlphas(ratio);
        mTransitionState = done ? TRANSITION_NONE : TRANSITION_RUNNING;
        break;

      case TRANSITION_NONE:
        // there is no transition in progress and mAlphas should be left as is.
        done = true;
        break;
    }

    for (int i = 0; i < mLayers.length; i++) {
      drawDrawableWithAlpha(canvas, mLayers[i], mAlphas[i] * mAlpha / 255);
    }

    if (!done) {
      invalidateSelf();
    }
  }

  private void drawDrawableWithAlpha(Canvas canvas, Drawable drawable, int alpha) {
    if (alpha > 0) {
      mPreventInvalidateCount++;
      drawable.mutate().setAlpha(alpha);
      mPreventInvalidateCount--;
      drawable.draw(canvas);
    }
  }

  @Override
  public void setAlpha(int alpha) {
    if (mAlpha != alpha) {
      mAlpha = alpha;
      invalidateSelf();
    }
  }

  public int getAlpha() {
    return mAlpha;
  }

  /**
   * Returns current time. Absolute reference is not important as only time deltas are used.
   * Extracting this to a separate method allows better testing.
   * @return current time in milliseconds
   */
  protected long getCurrentTimeMs() {
    return SystemClock.uptimeMillis();
  }

  /**
   * Gets the transition state (STARTING, RUNNING, NONE).
   * Useful for testing purposes.
   * @return transition state
   */
  @VisibleForTesting
  public int getTransitionState() {
    return mTransitionState;
  }

  public boolean isLayerOn(int index) {
    return mIsLayerOn[index];
  }

}
