/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.debug;

import android.graphics.Color;
import android.util.SparseIntArray;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public class DebugOverlayImageOriginColor {

  private static final SparseIntArray IMAGE_ORIGIN_COLOR_MAP = new SparseIntArray(7);

  static {
    IMAGE_ORIGIN_COLOR_MAP.append(ImageOrigin.UNKNOWN, Color.GRAY);
    IMAGE_ORIGIN_COLOR_MAP.append(ImageOrigin.NETWORK, Color.RED);
    IMAGE_ORIGIN_COLOR_MAP.append(ImageOrigin.DISK, Color.YELLOW);
    IMAGE_ORIGIN_COLOR_MAP.append(ImageOrigin.MEMORY_ENCODED, Color.YELLOW);
    IMAGE_ORIGIN_COLOR_MAP.append(ImageOrigin.MEMORY_BITMAP, Color.GREEN);
    IMAGE_ORIGIN_COLOR_MAP.append(ImageOrigin.MEMORY_BITMAP_SHORTCUT, Color.GREEN);
    IMAGE_ORIGIN_COLOR_MAP.append(ImageOrigin.LOCAL, Color.GREEN);
  }

  public static int getImageOriginColor(int imageOrigin) {
    return IMAGE_ORIGIN_COLOR_MAP.get(imageOrigin, Color.WHITE);
  }
}
