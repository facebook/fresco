/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.util;

import android.app.Activity;
import android.view.View;

/** Utility class to get View references using type inference */
public final class UI {

  /**
   * This method returns the reference of the View with the given Id in the layout of the Activity
   * passed as parameter
   *
   * @param act The Activity that is using the layout with the given View
   * @param viewId The id of the View we want to get a reference
   * @return The View with the given id and type
   */
  public static <T extends View> T findViewById(Activity act, int viewId) {
    View containerView = act.getWindow().getDecorView();
    return findViewById(containerView, viewId);
  }

  /**
   * This method returns the reference of the View with the given Id in the view passed as parameter
   *
   * @param containerView The container View
   * @param viewId The id of the View we want to get a reference
   * @return The View with the given id and type
   */
  @SuppressWarnings("unchecked")
  public static <T extends View> T findViewById(View containerView, int viewId) {
    View foundView = containerView.findViewById(viewId);
    return (T) foundView;
  }
}
