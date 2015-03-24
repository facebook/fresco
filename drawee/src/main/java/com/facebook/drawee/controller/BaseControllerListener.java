/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.controller;

import javax.annotation.Nullable;

import android.graphics.drawable.Animatable;

/**
 * Convenience class that has empty implementation of {@link ControllerListener}.
 */
public class BaseControllerListener<INFO> implements ControllerListener<INFO> {

  private static final ControllerListener<Object> NO_OP_LISTENER =
      new BaseControllerListener<Object>();

  public static <INFO> ControllerListener<INFO> getNoOpListener() {
    // Listener only receives <INFO>, it never produces one.
    // That means if it can accept Object, it can very well accept <INFO>.
    return (ControllerListener<INFO>) NO_OP_LISTENER;
  }

  @Override
  public void onSubmit(String id, Object callerContext) {
  }

  @Override
  public void onFinalImageSet(
      String id,
      @Nullable INFO imageInfo,
      @Nullable Animatable animatable) {
  }

  @Override
  public void onIntermediateImageSet(String id, @Nullable INFO imageInfo) {
  }

  @Override
  public void onIntermediateImageFailed(String id, Throwable throwable) {
  }

  @Override
  public void onFailure(String id, Throwable throwable) {
  }

  @Override
  public void onRelease(String id) {
  }
}
