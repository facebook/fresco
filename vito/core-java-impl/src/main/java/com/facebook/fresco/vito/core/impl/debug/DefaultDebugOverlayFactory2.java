/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import com.facebook.common.internal.Supplier;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.ui.common.VitoUtils;
import com.facebook.fresco.vito.core.FrescoDrawableInterface;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.impl.FrescoDrawable2;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class DefaultDebugOverlayFactory2 extends BaseDebugOverlayFactory2 {

  private boolean mShowExtendedInformation;
  private boolean mShowExtendedImageSourceExtraInformation;

  public DefaultDebugOverlayFactory2(Supplier<Boolean> debugOverlayEnabled) {
    this(true, false, debugOverlayEnabled);
  }

  public DefaultDebugOverlayFactory2(
      boolean showExtendedInformation,
      boolean showExtendedImageSourceExtraInformation,
      Supplier<Boolean> debugOverlayEnabled) {
    super(debugOverlayEnabled);
    mShowExtendedInformation = showExtendedInformation;
    mShowExtendedImageSourceExtraInformation = showExtendedImageSourceExtraInformation;
  }

  public void setShowExtendedInformation(boolean showExtendedInformation) {
    mShowExtendedInformation = showExtendedInformation;
  }

  public boolean getShowExtendedInformation() {
    return mShowExtendedInformation;
  }

  public void setShowExtendedImageSourceExtraInformation(
      boolean showExtendedImageSourceExtraInformation) {
    mShowExtendedImageSourceExtraInformation = showExtendedImageSourceExtraInformation;
  }

  @Override
  protected void setData(
      DebugOverlayDrawable overlay,
      FrescoDrawableInterface drawable,
      @Nullable ControllerListener2.Extras extras) {
    setBasicData(overlay, drawable);
    setImageRequestData(overlay, drawable.getImageRequest());
    setImageOriginData(overlay, extras);
    setImageSourceExtra(overlay, extras);
  }

  private void setBasicData(DebugOverlayDrawable overlay, FrescoDrawableInterface drawable) {
    overlay.setDrawIdentifier(mShowExtendedInformation);
    String tag = mShowExtendedInformation ? "ID" : overlay.getIdentifier();
    overlay.addDebugData(tag, VitoUtils.getStringId(drawable.getImageId()));
    if (drawable instanceof FrescoDrawable2) {
      FrescoDrawable2 abstractDrawable = (FrescoDrawable2) drawable;
      Rect bounds = abstractDrawable.getBounds();
      overlay.addDebugData("D", formatDimensions(bounds.width(), bounds.height()));
      if (mShowExtendedInformation) {
        overlay.addDebugData("DAR", String.valueOf(bounds.width() / (float) bounds.height()));
      }
      overlay.addDebugData(
          "I",
          formatDimensions(
              abstractDrawable.getActualImageWidthPx(), abstractDrawable.getActualImageHeightPx()));
      if (mShowExtendedInformation && abstractDrawable.getActualImageHeightPx() > 0) {
        overlay.addDebugData(
            "IAR",
            String.valueOf(
                abstractDrawable.getActualImageWidthPx()
                    / (float) abstractDrawable.getActualImageHeightPx()));
      }
      @Nullable PointF focusPoint = abstractDrawable.getActualImageFocusPoint();
      if (focusPoint != null) {
        overlay.addDebugData("FocusPointX", String.valueOf(focusPoint.x));
        overlay.addDebugData("FocusPointY", String.valueOf(focusPoint.y));
      }
    }
  }

  private void setImageOriginData(
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
    if (mShowExtendedInformation) {
      overlay.addDebugData(
          "origin", origin, DebugOverlayImageOriginColor.getImageOriginColor(origin));
      overlay.addDebugData("origin_sub", originSubcategory, Color.GRAY);
    } else {
      overlay.addDebugData(
          "o",
          origin + " | " + originSubcategory,
          DebugOverlayImageOriginColor.getImageOriginColor(origin));
    }
  }

  private void setImageSourceExtra(
      DebugOverlayDrawable overlay, @Nullable ControllerListener2.Extras extras) {
    if (mShowExtendedImageSourceExtraInformation && extras != null) {
      Map<String, Object> sourceExtras = extras.imageSourceExtras;
      if (sourceExtras != null) {
        for (Map.Entry<String, Object> entry : sourceExtras.entrySet()) {
          overlay.addDebugData(entry.getKey(), entry.getValue().toString());
        }
      }
    }
  }

  private void setImageRequestData(
      DebugOverlayDrawable overlay, @Nullable VitoImageRequest imageRequest) {
    if (imageRequest == null || !mShowExtendedInformation) {
      return;
    }
    overlay.addDebugData(
        "scale", String.valueOf(imageRequest.imageOptions.getActualImageScaleType()));
  }

  protected static String formatDimensions(int width, int height) {
    return String.format(Locale.US, "%dx%d", width, height);
  }
}
