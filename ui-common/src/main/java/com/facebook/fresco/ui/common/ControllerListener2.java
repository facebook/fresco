// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.fresco.ui.common;

import java.util.Map;
import javax.annotation.Nullable;

/* Experimental */
@Deprecated
public interface ControllerListener2<INFO> {

  class Extras {
    public @Nullable Map<String, Object> pipe;
    public @Nullable Map<String, Object> view;
  }

  /**
   * Called before the image request is submitted.
   *
   * <p>IMPORTANT: It is not safe to reuse the controller from within this callback!
   *
   * @param id controller id
   * @param callerContext caller context
   */
  void onSubmit(String id, Object callerContext);

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
   */
  void onFailure(String id, Throwable throwable);

  /**
   * Called after the controller released the fetched image.
   *
   * <p>IMPORTANT: It is not safe to reuse the controller from within this callback!
   *
   * @param id controller id
   */
  void onRelease(String id);
}
