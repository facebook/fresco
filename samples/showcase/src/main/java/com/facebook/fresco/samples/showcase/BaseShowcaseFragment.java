/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase;

import androidx.fragment.app.Fragment;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;

/** A base classe for ShowcaseFragment */
public abstract class BaseShowcaseFragment extends Fragment {

  public ImageUriProvider sampleUris() {
    return ShowcaseApplication.Companion.getImageUriProvider();
  }
}
