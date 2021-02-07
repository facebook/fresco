/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.atomic.AtomicLong;

@Nullsafe(Nullsafe.Mode.STRICT)
public final class VitoUtils {

  private static final AtomicLong sIdCounter = new AtomicLong();

  /**
   * Create a unique Fresco Vito image ID.
   *
   * @return the new image ID to use
   */
  public static long generateIdentifier() {
    return sIdCounter.incrementAndGet();
  }

  /** Create a string version for the given Vito ID. */
  public static String getStringId(long id) {
    // Vito IDs and Drawee IDs overlap. We add a prefix to distinguish between them.
    return "v" + id;
  }

  private VitoUtils() {}
}
