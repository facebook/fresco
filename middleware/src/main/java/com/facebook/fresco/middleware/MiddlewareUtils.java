package com.facebook.fresco.middleware;

import android.graphics.Rect;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class MiddlewareUtils {

  public static Extras obtainExtras(
      Map<String, Object> componentAttribution,
      Map<String, Object> shortcutAttribution,
      @Nullable DataSource<?> dataSource,
      @Nullable Rect viewportDimensions) {
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

    if (dataSource != null) {
      extras.pipe = dataSource.getExtras();
    } else {
      extras.view.putAll(shortcutAttribution);
    }

    return extras;
  }
}
