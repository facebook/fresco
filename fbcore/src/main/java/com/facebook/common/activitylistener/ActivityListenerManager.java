/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.activitylistener;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

import com.facebook.common.internal.Preconditions;

/**
 * Registers ActivityListener with ActivityListener.
 *
 * <p> A WeakReference is used to wrap an ActivityVisibilityListener. When it is nullified
 * ActivityListener is automatically removed from the listened ListenableActivity.
 */
public class ActivityListenerManager {

  /**
   * If given context is an instance of ListenableActivity then creates new instance of
   * WeakReferenceActivityListenerAdapter and adds it to activity's listeners
   */
  public static void register(
      ActivityListener activityListener,
      Context context) {
    if (!(context instanceof ListenableActivity) && context instanceof ContextWrapper) {
      context = ((ContextWrapper) context).getBaseContext();
    }
    if (context instanceof ListenableActivity) {
      ListenableActivity listenableActivity = (ListenableActivity) context;
      Listener listener = new Listener(activityListener);
      listenableActivity.addActivityListener(listener);
    }
  }

  private static class Listener extends BaseActivityListener {
    private final WeakReference<ActivityListener> mActivityListenerRef;

    public Listener(ActivityListener activityListener) {
      mActivityListenerRef = new WeakReference<ActivityListener>(activityListener);
    }

    @Override
    public void onActivityCreate(Activity activity) {
      ActivityListener activityVisibilityListener = getListenerOrCleanUp(activity);
      if (activityVisibilityListener != null) {
        activityVisibilityListener.onActivityCreate(activity);
      }
    }

    @Override
    public void onDestroy(Activity activity) {
      ActivityListener activityVisibilityListener = getListenerOrCleanUp(activity);
      if (activityVisibilityListener != null) {
        activityVisibilityListener.onDestroy(activity);
      }
    }

    @Override
    public void onStart(Activity activity) {
      ActivityListener activityVisibilityListener = getListenerOrCleanUp(activity);
      if (activityVisibilityListener != null) {
        activityVisibilityListener.onStart(activity);
      }
    }

    @Override
    public void onStop(Activity activity) {
      ActivityListener activityVisibilityListener = getListenerOrCleanUp(activity);
      if (activityVisibilityListener != null) {
        activityVisibilityListener.onStop(activity);
      }
    }

    @Override
    public void onResume(Activity activity) {
      ActivityListener activityVisibilityListener = getListenerOrCleanUp(activity);
      if (activityVisibilityListener != null) {
        activityVisibilityListener.onResume(activity);
      }
    }

    @Override
    public void onPause(Activity activity) {
      ActivityListener activityVisibilityListener = getListenerOrCleanUp(activity);
      if (activityVisibilityListener != null) {
        activityVisibilityListener.onPause(activity);
      }
    }

    private ActivityListener getListenerOrCleanUp(Activity activity) {
      ActivityListener activityVisibilityListener = mActivityListenerRef.get();
      if (activityVisibilityListener == null) {
        Preconditions.checkArgument(activity instanceof ListenableActivity);
        ListenableActivity listenableActivity = (ListenableActivity) activity;
        listenableActivity.removeActivityListener(this);
      }
      return activityVisibilityListener;
    }
  }
}
