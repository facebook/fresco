/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase;

import androidx.annotation.Nullable;

/** This is the abstraction for each Fragment we display into the showcase application */
public interface ShowcaseFragment {

  /** @return If any the tag to use when this is into a backstack */
  @Nullable
  String getBackstackTag();

  /** @return The resourceId for the title */
  int getTitleId();
}
