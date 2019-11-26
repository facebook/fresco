/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.debug;

import android.graphics.Color;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import java.util.Map;

public class DebugOverlayImageOriginColor {

  private static final Map<Integer, Integer> IMAGE_ORIGIN_COLOR_MAP =
      ImmutableMap.of(
          ImageOrigin.UNKNOWN, Color.GRAY,
          ImageOrigin.NETWORK, Color.RED,
          ImageOrigin.DISK, Color.YELLOW,
          ImageOrigin.MEMORY_ENCODED, Color.YELLOW,
          ImageOrigin.MEMORY_BITMAP, Color.GREEN,
          ImageOrigin.LOCAL, Color.GREEN);

  public static int getImageOriginColor(int imageOrigin) {
    // in place of getOrDefault() that would need API at least 24
    Integer colorFromMap = IMAGE_ORIGIN_COLOR_MAP.get(imageOrigin);
    return colorFromMap == null ? Color.WHITE : colorFromMap;
  }
}
