// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.fresco.ui.common;

import javax.annotation.Nullable;

/* Experimental */
@Deprecated
public class BaseControllerListener2<INFO> implements ControllerListener2<INFO> {

  private static final ControllerListener2 NO_OP_LISTENER = new BaseControllerListener2();

  public static ControllerListener2 getNoOpListener() {
    return NO_OP_LISTENER;
  }

  @Override
  public void onSubmit(String id, Object callerContext) {}

  @Override
  public void onFinalImageSet(String id, @Nullable INFO imageInfo, Object extraData) {}

  @Override
  public void onIntermediateImageSet(String id, @Nullable INFO imageInfo) {}

  @Override
  public void onIntermediateImageFailed(String id) {}

  @Override
  public void onFailure(String id, Throwable throwable) {}

  @Override
  public void onRelease(String id) {}
}
