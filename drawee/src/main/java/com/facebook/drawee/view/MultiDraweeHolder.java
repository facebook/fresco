/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.view;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.drawee.interfaces.DraweeHierarchy;

/**
 * Contains multiple Drawee holders.
 *
 * <p>Intended for use in custom views that are showing more than one hierarchy.
 *
 * Users of this class must< call {@link Drawable#setBounds} on the top-level drawable
 * of each DraweeHierarchy in this holder. Otherwise the drawables will not be drawn.

 * <p>The containing view must also call {@link #onDetach()} from its
 * {@link View#onStartTemporaryDetach()} and {@link View#onDetachedFromWindow()} methods. It must
 * call {@link #onAttach} from its  {@link View#onFinishTemporaryDetach()} and
 * {@link View#onAttachedToWindow()} methods.
 */
public class MultiDraweeHolder<DH extends DraweeHierarchy> {

  @VisibleForTesting boolean mIsAttached = false;
  @VisibleForTesting ArrayList<DraweeHolder<DH>> mHolders = new ArrayList<>();

  /**
   * Gets the controller ready to display the images.
   *
   * <p>The containing view must call this method from both {@link View#onFinishTemporaryDetach()}
   * and {@link View#onAttachedToWindow()}.
   */
  public void onAttach() {
    if (mIsAttached) {
      return;
    }
    mIsAttached = true;
    for (int i = 0; i < mHolders.size(); ++i) {
      mHolders.get(i).onAttach();
    }
  }

  /**
   * Releases resources used to display the image.
   *
   * <p>The containing view must call this method from both {@link View#onStartTemporaryDetach()}
   * and {@link View#onDetachedFromWindow()}.
   */
  public void onDetach() {
    if (!mIsAttached) {
      return;
    }
    mIsAttached = false;
    for (int i = 0; i < mHolders.size(); ++i) {
      mHolders.get(i).onDetach();
    }
  }

  public boolean onTouchEvent(MotionEvent event) {
    for (int i = 0; i < mHolders.size(); ++i) {
      if (mHolders.get(i).onTouchEvent(event)) {
        return true;
      }
    }
    return false;
  }

  public void clear() {
    if (mIsAttached) {
      for (int i = 0; i < mHolders.size(); ++i) {
        mHolders.get(i).onDetach();
      }
    }
    mHolders.clear();
  }

  public void add(DraweeHolder<DH> holder) {
    add(mHolders.size(), holder);
  }

  public void add(int index, DraweeHolder<DH> holder) {
    Preconditions.checkNotNull(holder);
    Preconditions.checkElementIndex(index, mHolders.size() + 1);
    mHolders.add(index, holder);
    if (mIsAttached) {
      holder.onAttach();
    }
  }

  public void remove(int index) {
    DraweeHolder<DH> holder = mHolders.get(index);
    if (mIsAttached) {
      holder.onDetach();
    }
    mHolders.remove(index);
  }

  public DraweeHolder<DH> get(int index) {
    return mHolders.get(index);
  }

  public int size() {
    return mHolders.size();
  }

  /** Convenience method to draw all the top-level drawables in this holder. */
  public void draw(Canvas canvas) {
    for (int i = 0; i < mHolders.size(); ++i) {
      Drawable drawable = get(i).getTopLevelDrawable();
      if (drawable != null) {
        drawable.draw(canvas);
      }
    }
  }

  /** Returns true if the argument is a top-level Drawable in this holder. */
  public boolean verifyDrawable(Drawable who) {
    for (int i = 0; i < mHolders.size(); ++i) {
      if (who == get(i).getTopLevelDrawable()) {
        return true;
      }
    }
    return false;
  }
}
