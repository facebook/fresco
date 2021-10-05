/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho.slideshow;

import android.graphics.drawable.Drawable;
import com.facebook.common.internal.Preconditions;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.infer.annotation.Nullsafe;
import java.util.TimerTask;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class FrescoVitoSlideshowDrawable extends FadeDrawable {

  private int mCurrentLayer = 0;
  private @Nullable TimerTask mTimerTask;

  public FrescoVitoSlideshowDrawable(
      FrescoDrawable2 drawable1, FrescoDrawable2 drawable2, FrescoDrawable2 drawable3) {
    super(new Drawable[] {drawable1, drawable2, drawable3});
  }

  public void setTimerTask(TimerTask timerTask) {
    mTimerTask = timerTask;
  }

  @Nullable
  public TimerTask getTimerTask() {
    return mTimerTask;
  }

  public FrescoDrawable2 getCurrent() {
    return Preconditions.checkNotNull((FrescoDrawable2) getDrawable(mCurrentLayer));
  }

  public FrescoDrawable2 getNext() {
    return Preconditions.checkNotNull((FrescoDrawable2) getDrawable(getNextLayerIndex()));
  }

  public FrescoDrawable2 getPrevious() {
    return Preconditions.checkNotNull((FrescoDrawable2) getDrawable(getPreviousLayerIndex()));
  }

  public void fadeToNext() {
    final int prev = getPreviousLayerIndex();
    final int next = getNextLayerIndex();

    beginBatchMode();
    fadeUpToLayer(next);
    hideLayerImmediately(prev);
    endBatchMode();
    mCurrentLayer = next;
  }

  @Override
  public void reset() {
    if (mTimerTask != null) {
      mTimerTask.cancel();
    }
    mTimerTask = null;
    super.reset();
    mCurrentLayer = 0;
  }

  private int getNextLayerIndex() {
    return (mCurrentLayer + 1) % getNumberOfLayers();
  }

  private int getPreviousLayerIndex() {
    return (mCurrentLayer - 1 + getNumberOfLayers()) % getNumberOfLayers();
  }
}
