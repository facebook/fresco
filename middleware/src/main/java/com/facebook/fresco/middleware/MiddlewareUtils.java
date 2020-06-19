package com.facebook.fresco.middleware;

import android.graphics.Rect;
import android.net.Uri;
import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class MiddlewareUtils {

  public static Extras obtainExtras(
      Map<String, Object> componentAttribution,
      Map<String, Object> shortcutAttribution,
      @Nullable Map<String, Object> dataSourceExtras,
      @Nullable Rect viewportDimensions,
      @Nullable Map<String, Object> imageExtras,
      @Nullable Object callerContext,
      @Nullable Uri mainUri) {
    final Extras extras = new Extras();
    extras.view = new HashMap<>();

    extras.view.putAll(componentAttribution);

    if (viewportDimensions != null) {
      extras.view.put("viewport_width", viewportDimensions.width());
      extras.view.put("viewport_height", viewportDimensions.height());
    } else {
      extras.view.put("viewport_width", -1);
      extras.view.put("viewport_height", -1);
    }

    extras.view.put("caller_context", callerContext);
    if (mainUri != null) extras.view.put("uri_main", mainUri);

    if (dataSourceExtras != null) {
      extras.pipe = dataSourceExtras;
    } else {
      extras.pipe = imageExtras;
      extras.view.putAll(shortcutAttribution);
    }

    return extras;
  }
}
