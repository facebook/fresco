/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.disk;

import com.facebook.infer.annotation.Nullsafe;

/** Any class that uses a lot of disk space and should implement this interface. */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface DiskTrimmable {
  /** Called when there is very little disk space left. */
  void trimToMinimum();

  /** Called when there is almost no disk space left and the app is likely to crash soon */
  void trimToNothing();
}
