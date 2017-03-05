/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
