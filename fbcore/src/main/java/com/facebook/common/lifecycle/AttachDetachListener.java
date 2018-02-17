/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */


package com.facebook.common.lifecycle;

import android.view.View;

/**
 * Attach detach listener.
 */
public interface AttachDetachListener {

  void onAttachToView(View view);

  void onDetachFromView(View view);
}
