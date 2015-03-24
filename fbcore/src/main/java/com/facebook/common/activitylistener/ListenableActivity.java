/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
  public void addActivityListener(ActivityListener listener);

  /**
   * Removes ActivityListener from the activity
   *
   * @param listener
   */
  public void removeActivityListener(ActivityListener listener);
}
