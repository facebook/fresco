/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import com.facebook.fresco.vito.core.FrescoDrawableInterface;
import com.facebook.fresco.vito.core.VitoImagePerfListener;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public class NoOpVitoImagePerfListener implements VitoImagePerfListener {

  @Override
  public void onImageMount(FrescoDrawableInterface drawable) {}

  @Override
  public void onImageUnmount(FrescoDrawableInterface drawable) {}

  @Override
  public void onImageBind(FrescoDrawableInterface drawable) {}

  @Override
  public void onImageUnbind(FrescoDrawableInterface drawable) {}

  @Override
  public void onImageFetch(FrescoDrawableInterface drawable) {}

  @Override
  public void onImageSuccess(FrescoDrawableInterface drawable, boolean wasImmediate) {}

  @Override
  public void onImageError(FrescoDrawableInterface drawable) {}

  @Override
  public void onImageRelease(FrescoDrawableInterface drawable) {}

  @Override
  public void onScheduleReleaseDelayed(FrescoDrawableInterface drawable) {}

  @Override
  public void onScheduleReleaseNextFrame(FrescoDrawableInterface drawable) {}

  @Override
  public void onReleaseImmediately(FrescoDrawableInterface drawable) {}

  @Override
  public void onDrawableReconfigured(FrescoDrawableInterface drawable) {}

  @Override
  public void onIgnoreResult(FrescoDrawableInterface drawable) {}

  @Override
  public void onIgnoreFailure(FrescoDrawableInterface drawable) {}
}
