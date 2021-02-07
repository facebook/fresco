/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashMap;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.STRICT)
public class DebugOverlayImageOriginColor {

  private static final Map<String, Integer> IMAGE_ORIGIN_COLOR_MAP = new HashMap<>(6);

  static {
    IMAGE_ORIGIN_COLOR_MAP.put("unknown", Color.GRAY);
    IMAGE_ORIGIN_COLOR_MAP.put("network", Color.RED);
    IMAGE_ORIGIN_COLOR_MAP.put("disk", Color.YELLOW);
    IMAGE_ORIGIN_COLOR_MAP.put("memory_encoded", Color.YELLOW);
    IMAGE_ORIGIN_COLOR_MAP.put("memory_bitmap", Color.GREEN);
    IMAGE_ORIGIN_COLOR_MAP.put("local", Color.GREEN);
  }

  public static @ColorInt int getImageOriginColor(String imageOrigin) {
    Integer val = IMAGE_ORIGIN_COLOR_MAP.get(imageOrigin);
    return val != null ? val : Color.WHITE;
  }
}
