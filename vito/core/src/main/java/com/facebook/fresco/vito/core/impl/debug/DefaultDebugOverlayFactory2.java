/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import android.graphics.Color;
import android.graphics.Rect;
import com.facebook.common.internal.Supplier;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoUtils;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class DefaultDebugOverlayFactory2 extends BaseDebugOverlayFactory2 {

  public DefaultDebugOverlayFactory2(Supplier<Boolean> debugOverlayEnabled) {
    super(debugOverlayEnabled);
  }

  @Override
  protected void setData(
      DebugOverlayDrawable overlay,
      FrescoDrawable2 drawable,
      @Nullable ControllerListener2.Extras extras) {
    setBasicData(overlay, drawable);
    setImageRequestData(overlay, drawable.getImageRequest());
    setImageOriginData(overlay, extras);
  }

  private static void setBasicData(DebugOverlayDrawable overlay, FrescoDrawable2 drawable) {
    overlay.addDebugData("ID", VitoUtils.getStringId(drawable.getImageId()));
    Rect bounds = drawable.getBounds();
    overlay.addDebugData("D", formatDimensions(bounds.width(), bounds.height()));
    overlay.addDebugData(
        "I", formatDimensions(drawable.getActualImageWidthPx(), drawable.getActualImageHeightPx()));
  }

  private static void setImageOriginData(
      DebugOverlayDrawable overlay, @Nullable ControllerListener2.Extras extras) {
    String origin = "unknown";
    String originSubcategory = "unknown";
    if (extras != null) {
      Map<String, Object> originExtras = extras.datasourceExtras;
      if (originExtras == null) {
        // We did not receive data source extras, so the image did not come from the image pipeline
        // but from the bitmap memory cache shortcut
        originExtras = extras.shortcutExtras;
      }
      if (originExtras != null) {
        origin = String.valueOf(originExtras.get("origin"));
        originSubcategory = String.valueOf(originExtras.get("origin_sub"));
      }
    }
    overlay.addDebugData(
        "origin", origin, DebugOverlayImageOriginColor.getImageOriginColor(origin));
    overlay.addDebugData("origin_sub", originSubcategory, Color.GRAY);
  }

  private static void setImageRequestData(
      DebugOverlayDrawable overlay, @Nullable VitoImageRequest imageRequest) {
    if (imageRequest == null) {
      return;
    }
    overlay.addDebugData(
        "scale", String.valueOf(imageRequest.imageOptions.getActualImageScaleType()));
  }

  protected static String formatDimensions(int width, int height) {
    return String.format(Locale.US, "%dx%d", width, height);
  }
}
