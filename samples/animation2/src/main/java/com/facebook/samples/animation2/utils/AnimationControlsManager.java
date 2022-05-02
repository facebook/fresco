/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.animation2.utils;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.ToggleButton;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.fresco.animation.drawable.AnimationListener;
import com.facebook.fresco.animation.drawable.BaseAnimationListener;
import javax.annotation.Nullable;

/** Helper class that manages animation controls: Play / pause, reset and a seekbar. */
public class AnimationControlsManager {

  private final AnimatedDrawable2 mAnimatedDrawable;
  @Nullable private final SeekBar mSeekBar;
  @Nullable private final ToggleButton mPlayPauseToggleButton;
  @Nullable private final View mResetButton;

  private AnimationListener mAnimationListener =
      new BaseAnimationListener() {
        @Override
        public void onAnimationStart(AnimatedDrawable2 drawable) {
          if (mPlayPauseToggleButton != null) {
            mPlayPauseToggleButton.setChecked(true);
          }
        }

        @Override
        public void onAnimationStop(AnimatedDrawable2 drawable) {
          if (mPlayPauseToggleButton != null) {
            mPlayPauseToggleButton.setChecked(false);
          }
        }

        @Override
        public void onAnimationFrame(AnimatedDrawable2 drawable, int frameNumber) {
          if (mSeekBar != null) {
            mSeekBar.setProgress(frameNumber);
          }
        }
      };

  public AnimationControlsManager(
      AnimatedDrawable2 animatedDrawable,
      @Nullable SeekBar seekBar,
      @Nullable ToggleButton playPauseToggleButton,
      @Nullable View resetButton) {
    mAnimatedDrawable = animatedDrawable;
    mSeekBar = seekBar;
    mPlayPauseToggleButton = playPauseToggleButton;
    mResetButton = resetButton;

    setupPlayPauseToggleButton();
    setupResetButton();
    setupSeekBar();

    mAnimatedDrawable.setAnimationListener(mAnimationListener);
    updateBackendData(mAnimatedDrawable.getAnimationBackend());
  }

  public void updateBackendData(@Nullable AnimationBackend newBackend) {
    if (mSeekBar == null) {
      return;
    }
    if (newBackend != null) {
      mSeekBar.setMax(newBackend.getFrameCount() - 1);
    } else {
      mSeekBar.setMax(0);
    }
  }

  private void setupResetButton() {
    if (mResetButton == null) {
      return;
    }
    mResetButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            mAnimatedDrawable.stop();
            mAnimatedDrawable.jumpToFrame(0);
          }
        });
  }

  private void setupPlayPauseToggleButton() {
    if (mPlayPauseToggleButton == null) {
      return;
    }
    mPlayPauseToggleButton.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
              mAnimatedDrawable.start();
            } else {
              mAnimatedDrawable.stop();
            }
          }
        });
  }

  private void setupSeekBar() {
    if (mSeekBar == null) {
      return;
    }
    mSeekBar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          @Override
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
              mAnimatedDrawable.jumpToFrame(progress);
            }
          }

          @Override
          public void onStartTrackingTouch(SeekBar seekBar) {}

          @Override
          public void onStopTrackingTouch(SeekBar seekBar) {}
        });
  }
}
