/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Implementation of {@link MemoryTrimmableRegistry} that does not do anything. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class NoOpMemoryTrimmableRegistry implements MemoryTrimmableRegistry {
  @Nullable private static NoOpMemoryTrimmableRegistry sInstance = null;

  public NoOpMemoryTrimmableRegistry() {}

  public static synchronized NoOpMemoryTrimmableRegistry getInstance() {
    if (sInstance == null) {
      sInstance = new NoOpMemoryTrimmableRegistry();
    }
    return sInstance;
  }

  /** Register an object. */
  public void registerMemoryTrimmable(MemoryTrimmable trimmable) {}

  /** Unregister an object. */
  public void unregisterMemoryTrimmable(MemoryTrimmable trimmable) {}
}
