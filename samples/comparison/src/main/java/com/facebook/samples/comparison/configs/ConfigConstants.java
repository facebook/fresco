/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.configs;

import com.facebook.common.util.ByteConstants;

public class ConfigConstants {
  private static final int MAX_HEAP_SIZE = (int) Runtime.getRuntime().maxMemory();

  public static final int MAX_DISK_CACHE_SIZE = 40 * ByteConstants.MB;
  public static final int MAX_MEMORY_CACHE_SIZE = MAX_HEAP_SIZE / 4;
}
