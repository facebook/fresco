/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import com.facebook.common.activitylistener.ActivityListener;
import com.facebook.common.activitylistener.BaseActivityListener;
import com.facebook.common.activitylistener.ListenableActivity;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.components.DraweeEventTracker;
import com.facebook.drawee.drawable.VisibilityAwareDrawable;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;

import javax.annotation.Nullable;

import static com.facebook.drawee.components.DraweeEventTracker.Event;

/**
 * A holder class for Drawee controller and hierarchy.
 *
 * <p>Drawee users, should, as a rule, use {@link DraweeView} or its subclasses. There are
 * situations where custom views are required, however, and this class is for those circumstances.
 *
 * <p>Each {@link DraweeHierarchy} object should be contained in a single instance of this
 * class.
 *
 * <p>Users of this class must call {@link Drawable#setBounds} on the top-level drawable
 * of the DraweeHierarchy. Otherwise the drawable will not be drawn.
 *
 * <p>The containing view must also call {@link #onDetach()} from its
 * {@link View#onStartTemporaryDetach()} and {@link View#onDetachedFromWindow()} methods. It must
 * call {@link #onAttach} from its {@link View#onFinishTemporaryDetach()} and
 * {@link View#onAttachedToWindow()} methods.
 */
public class DraweeHolder<DH extends DraweeHierarchy> implements VisibilityCallback {

  private boolean mIsControllerAttached = false;
  private boolean mIsHolderAttached = false;
  private boolean mIsVisible = true;
  private boolean mIsActivityStarted = true;
  private DH mHierarchy;
  private DraweeController mController = null;
  private final ActivityListener mActivityListener;
  private final DraweeEventTracker mEventTracker = new DraweeEventTracker();

  /**
   * Creates a new instance of DraweeHolder that detaches / attaches controller whenever context
   * notifies it about activity's onStop and onStart callbacks.
   *
   * <p>It is strongly recommended to pass a {@link ListenableActivity} as context. The holder will
   * then also be able to respond to onStop and onStart events from that activity, making sure the
   * image does not waste memory when the activity is stopped.
   */
  public static <DH extends DraweeHierarchy> DraweeHolder<DH> create(
      @Nullable DH hierarchy,
      Context context) {
    DraweeHolder<DH> holder = new DraweeHolder<DH>(hierarchy);
    holder.registerWithContext(context);
    return holder;
  }

  /**
   * If the given context is an instance of FbListenableActivity, then listener for its onStop and
   * onStart methods is registered that changes visibility of the holder.
   */
  public void registerWithContext(Context context) {
    // TODO(T6181423): this is not working reliably and we cannot afford photos-not-loading issues.
    //ActivityListenerManager.register(mActivityListener, context);
  }

  /**
   * Creates a new instance of DraweeHolder.
   * @param hierarchy
   */
  public DraweeHolder(@Nullable DH hierarchy) {
    if (hierarchy != null) {
      setHierarchy(hierarchy);
    }
    mActivityListener = new BaseActivityListener() {
      @Override
      public void onStart(Activity activity) {
        setActivityStarted(true);
      }

      @Override
      public void onStop(Activity activity) {
        setActivityStarted(false);
      }
    };
  }

  /**
   * Gets the controller ready to display the image.
   *
   * <p>The containing view must call this method from both {@link View#onFinishTemporaryDetach()}
   * and {@link View#onAttachedToWindow()}.
   */
  public void onAttach() {
    mEventTracker.recordEvent(Event.ON_HOLDER_ATTACH);
    mIsHolderAttached = true;
    attachOrDetachController();
  }

  /**
   * Releases resources used to display the image.
   *
   * <p>The containing view must call this method from both {@link View#onStartTemporaryDetach()}
   * and {@link View#onDetachedFromWindow()}.
   */
  public void onDetach() {
    mEventTracker.recordEvent(Event.ON_HOLDER_DETACH);
    mIsHolderAttached = false;
    attachOrDetachController();
  }

  /**
   * Forwards the touch event to the controller.
   * @param event touch event to handle
   * @return whether the event was handled or not
   */
  public boolean onTouchEvent(MotionEvent event) {
    if (mController == null) {
      return false;
    }
    return mController.onTouchEvent(event);
  }

  /**
   * Callback used to notify about top-level-drawable's visibility changes.
   */
  @Override
  public void onVisibilityChange(boolean isVisible) {
    if (mIsVisible == isVisible) {
      return;
    }
    mEventTracker.recordEvent(isVisible ? Event.ON_DRAWABLE_SHOW : Event.ON_DRAWABLE_HIDE);
    mIsVisible = isVisible;
    attachOrDetachController();
  }

  /**
   * Callback used to notify about top-level-drawable being drawn.
   */
  @Override
  public void onDraw() {
    // draw is only expected if the controller is attached
    if (mIsControllerAttached) {
      return;
    }
    // something went wrong here; controller is not attached, yet the hierarchy has to be drawn
    // log error and attach the controller
    FLog.wtf(
        DraweeEventTracker.class,
        "%x: Draw requested for a non-attached controller %x. %s",
        System.identityHashCode(this),
        System.identityHashCode(mController),
        toString());
    mIsHolderAttached = true;
    mIsVisible = true;
    mIsActivityStarted = true;
    attachOrDetachController();
  }

  /**
   * Sets the visibility callback to the current top-level-drawable.
   */
  private void setVisibilityCallback(@Nullable VisibilityCallback visibilityCallback) {
    Drawable drawable = getTopLevelDrawable();
    if (drawable instanceof VisibilityAwareDrawable) {
      ((VisibilityAwareDrawable) drawable).setVisibilityCallback(visibilityCallback);
    }
  }

  /**
   * Notifies the holder of activity's visibility change
   */
  private void setActivityStarted(boolean isStarted) {
    mEventTracker.recordEvent(isStarted ? Event.ON_ACTIVITY_START : Event.ON_ACTIVITY_STOP);
    mIsActivityStarted = isStarted;
    attachOrDetachController();
  }

  /**
   * Sets a new controller.
   */
  public void setController(@Nullable DraweeController draweeController) {
    boolean wasAttached = mIsControllerAttached;
    if (wasAttached) {
      detachController();
    }

    // Clear the old controller
    if (mController != null) {
      mEventTracker.recordEvent(Event.ON_CLEAR_OLD_CONTROLLER);
      mController.setHierarchy(null);
    }
    mController = draweeController;
    if (mController != null) {
      mEventTracker.recordEvent(Event.ON_SET_CONTROLLER);
      mController.setHierarchy(mHierarchy);
    } else {
      mEventTracker.recordEvent(Event.ON_CLEAR_CONTROLLER);
    }

    if (wasAttached) {
      attachController();
    }
  }

  /**
   * Gets the controller if set, null otherwise.
   */
  @Nullable public DraweeController getController() {
    return mController;
  }

  /**
   * Sets the drawee hierarchy.
   */
  public void setHierarchy(DH hierarchy) {
    mEventTracker.recordEvent(Event.ON_SET_HIERARCHY);
    setVisibilityCallback(null);
    mHierarchy = Preconditions.checkNotNull(hierarchy);
    onVisibilityChange(mHierarchy.getTopLevelDrawable().isVisible());
    setVisibilityCallback(this);
    if (mController != null) {
      mController.setHierarchy(hierarchy);
    }
  }

  /**
   * Gets the drawee hierarchy if set, throws NPE otherwise.
   */
  public DH getHierarchy() {
    return Preconditions.checkNotNull(mHierarchy);
  }

  /**
   * Returns whether the hierarchy is set or not.
   */
  public boolean hasHierarchy() {
    return mHierarchy != null;
  }

  /**
   * Gets the top-level drawable if hierarchy is set, null otherwise.
   */
  public Drawable getTopLevelDrawable() {
    return mHierarchy == null ? null : mHierarchy.getTopLevelDrawable();
  }

  private void attachController() {
    if (mIsControllerAttached) {
      return;
    }
    mEventTracker.recordEvent(Event.ON_ATTACH_CONTROLLER);
    mIsControllerAttached = true;
    if (mController != null &&
        mController.getHierarchy() != null) {
      mController.onAttach();
    }
  }

  private void detachController() {
    if (!mIsControllerAttached) {
      return;
    }
    mEventTracker.recordEvent(Event.ON_DETACH_CONTROLLER);
    mIsControllerAttached = false;
    if (mController != null) {
      mController.onDetach();
    }
  }

  private void attachOrDetachController() {
    if (mIsHolderAttached && mIsVisible && mIsActivityStarted) {
      attachController();
    } else {
      detachController();
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("controllerAttached", mIsControllerAttached)
        .add("holderAttached", mIsHolderAttached)
        .add("drawableVisible", mIsVisible)
        .add("activityStarted", mIsActivityStarted)
        .add("events", mEventTracker.toString())
        .toString();
  }
}
