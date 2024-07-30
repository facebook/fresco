/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.lifecycle;

import android.view.View;
import com.facebook.infer.annotation.Nullsafe;

/** Attach detach listener. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface AttachDetachListener {

  void onAttachToView(View view);

  void onDetachFromView(View view);
}
