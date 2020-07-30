// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.fresco.ui.common;

import android.net.Uri;
import java.util.Map;
import javax.annotation.Nullable;

/* Experimental */
@Deprecated
public interface ControllerListener2<INFO> {

  class Extras {
    public @Nullable Map<String, Object> componentExtras;
    public @Nullable Map<String, Object> shortcutExtras;
    public @Nullable Map<String, Object> datasourceExtras;
    public @Nullable Map<String, Object> imageExtras;

    public @Nullable Object callerContext;
    public @Nullable Uri mainUri;

    public int viewportWidth = -1;
    public int viewportHeight = -1;
    public Object scaleType;
    public float focusX = -1;
    public float focusY = -1;

    public static Extras of(@Nullable Map<String, Object> componentExtras) {
      Extras extras = new Extras();
      extras.componentExtras = componentExtras;
      return extras;
    }
  }

  /**
   * Called before the image request is submitted.
   *
   * <p>IMPORTANT: It is not safe to reuse the controller from within this callback!
   *
   * @param id controller id
   * @param callerContext caller context
   * @param extraData extra data
   */
  void onSubmit(String id, Object callerContext, @Nullable Extras extraData);

  /**
   * Called after the final image has been set.
   *
   * @param id controller id
   * @param imageInfo image info
   * @param extraData extra data
   */
  void onFinalImageSet(String id, @Nullable INFO imageInfo, Extras extraData);

  /**
   * Called after any intermediate image has been set.
   *
   * @param id controller id
   */
  void onIntermediateImageSet(String id, @Nullable INFO imageInfo);

  /**
   * Called after the fetch of the intermediate image failed.
   *
   * @param id controller id
   */
  void onIntermediateImageFailed(String id);

  /**
   * Called after the fetch of the final image failed.
   *
   * @param id controller id
   * @param throwable failure cause
   * @param extraData extra data
   */
  void onFailure(String id, Throwable throwable, @Nullable Extras extraData);

  /**
   * Called after the controller released the fetched image.
   *
   * <p>IMPORTANT: It is not safe to reuse the controller from within this callback!
   *
   * @param id controller id
   * @param extraData extra data
   */
  void onRelease(String id, @Nullable Extras extraData);
}
