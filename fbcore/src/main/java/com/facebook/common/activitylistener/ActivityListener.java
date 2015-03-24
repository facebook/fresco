/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.activitylistener;

import android.app.Activity;

/**
 * Listener interface for activity lifecycle events.
 * <p>
 * All methods take the Activity so it's possible to implement a singleton version of this
 * interface that has no-state.
 */
public interface ActivityListener {

  /**
   * Called by the Activity base class after the Activity's <code>onActivityCreate</code>
   * method has run.
   *
   * @param activity the activity
   */
  void onActivityCreate(Activity activity);

  /**
   * Called by the Activity base class from the {@link Activity#onStart} method.
   *
   * @param activity the activity
   */
  void onStart(Activity activity);

  /**
   * Called by the Activity base class from the {@link Activity#onResume} method.
   *
   * @param activity the activity
   */
  void onResume(Activity activity);

  /**
   * Called by the Activity base class from the {@link Activity#onPause} method.
   *
   * @param activity the activity
   */
  void onPause(Activity activity);

  /**
   * Called by the Activity base class from the {@link Activity#onStop} method.
   *
   * @param activity the activity
   */
  void onStop(Activity activity);

  /**
   * Called by the Activity base class from the {@link Activity#onDestroy} method.
   *
   * @param activity the activity
   */
  void onDestroy(Activity activity);
}
