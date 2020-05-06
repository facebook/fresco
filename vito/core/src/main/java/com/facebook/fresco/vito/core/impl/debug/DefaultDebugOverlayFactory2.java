/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.debug.DebugOverlayImageOriginColor;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginUtils;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoUtils;
import java.util.Locale;
import javax.annotation.Nullable;

public class DefaultDebugOverlayFactory2 extends BaseDebugOverlayFactory2 {

  public DefaultDebugOverlayFactory2(Supplier<Boolean> debugOverlayEnabled) {
    super(debugOverlayEnabled);
  }

  @Override
  protected void setData(DebugOverlayDrawable overlay, FrescoDrawable2 drawable) {
    setBasicData(overlay, drawable);
    setImageRequestData(overlay, drawable.getImageRequest());
    setImageOriginData(overlay, drawable.getImageOrigin());
  }

  private void setBasicData(DebugOverlayDrawable overlay, FrescoDrawable2 drawable) {
    overlay.addDebugData("ID", VitoUtils.getStringId(drawable.getImageId()));
    overlay.addDebugData(
        "D",
        String.format(
            Locale.US, "%dx%d", drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight()));
  }

  private void setImageOriginData(DebugOverlayDrawable overlay, @ImageOrigin int imageOrigin) {
    overlay.addDebugData(
        "origin",
        ImageOriginUtils.toString(imageOrigin),
        DebugOverlayImageOriginColor.getImageOriginColor(imageOrigin));
  }

  private void setImageRequestData(
      DebugOverlayDrawable overlay, @Nullable VitoImageRequest imageRequest) {
    if (imageRequest == null) {
      return;
    }
    if (imageRequest.imageOptions.getActualImageScaleType() != null) {
      overlay.addDebugData(
          "scale", String.valueOf(imageRequest.imageOptions.getActualImageScaleType()));
    }
  }
}
