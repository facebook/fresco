/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.memory;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Used to make available a list of all UI objects in the system that can
 * be trimmed on background. Currently stores DraweeHolder objects, both
 * inside and outside views.
 */
public class MemoryUiTrimmableRegistry {

  private static final Set<MemoryUiTrimmable> sUiTrimmables =
      Collections.newSetFromMap(new WeakHashMap<MemoryUiTrimmable, Boolean>());

  public static void registerUiTrimmable(MemoryUiTrimmable uiTrimmable) {
    sUiTrimmables.add(uiTrimmable);
  }

  public static Iterable<MemoryUiTrimmable> iterable() {
    return sUiTrimmables;
  }

  // There is no unregister! The trimmables are stored in a weak-hash set,
  // so the GC will take care of that.

}
