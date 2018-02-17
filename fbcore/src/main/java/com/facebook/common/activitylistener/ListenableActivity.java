/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.activitylistener;

/**
 * Interface for activities that support dynamic addition of ActivityListeners
 */
public interface ListenableActivity {

  /**
   * Adds ActivityListener to the activity
   *
   * @param listener
   */
  void addActivityListener(ActivityListener listener);

  /**
   * Removes ActivityListener from the activity
   *
   * @param listener
   */
  void removeActivityListener(ActivityListener listener);
}
