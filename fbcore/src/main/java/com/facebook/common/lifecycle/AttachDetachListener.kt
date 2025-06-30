/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.lifecycle

import android.view.View

/** Attach detach listener. */
interface AttachDetachListener {

  fun onAttachToView(view: View)

  fun onDetachFromView(view: View)
}
