// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.fresco.ui.common;

import javax.annotation.Nullable;

/* Experimental */
@Deprecated
public class BaseControllerListener2<INFO> implements ControllerListener2<INFO> {

  private static final ControllerListener2 NO_OP_LISTENER = new BaseControllerListener2();

  public static <I> ControllerListener2<I> getNoOpListener() {
    //noinspection unchecked
    return (ControllerListener2<I>) NO_OP_LISTENER;
  }

  @Override
  public void onSubmit(String id, Object callerContext, @Nullable Extras extras) {}

  @Override
  public void onFinalImageSet(String id, @Nullable INFO imageInfo, Extras extraData) {}

  @Override
  public void onIntermediateImageSet(String id, @Nullable INFO imageInfo) {}

  @Override
  public void onIntermediateImageFailed(String id) {}

  @Override
  public void onFailure(String id, Throwable throwable, Extras extras) {}

  @Override
  public void onRelease(String id, Extras extras) {}
}
