/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.VitoImagePerfListener;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public class NoOpVitoImagePerfListener implements VitoImagePerfListener {

  @Override
  public void onImageMount(FrescoDrawable2 drawable) {}

  @Override
  public void onImageUnmount(FrescoDrawable2 drawable) {}

  @Override
  public void onImageBind(FrescoDrawable2 drawable) {}

  @Override
  public void onImageUnbind(FrescoDrawable2 drawable) {}

  @Override
  public void onImageFetch(FrescoDrawable2 drawable) {}

  @Override
  public void onImageSuccess(FrescoDrawable2 drawable, boolean wasImmediate) {}

  @Override
  public void onImageError(FrescoDrawable2 drawable) {}

  @Override
  public void onImageRelease(FrescoDrawable2 drawable) {}

  @Override
  public void onScheduleReleaseDelayed(FrescoDrawable2 drawable) {}

  @Override
  public void onScheduleReleaseNextFrame(FrescoDrawable2 drawable) {}

  @Override
  public void onReleaseImmediately(FrescoDrawable2 drawable) {}

  @Override
  public void onDrawableReconfigured(FrescoDrawable2 drawable) {}

  @Override
  public void onIgnoreResult(FrescoDrawable2 drawable) {}

  @Override
  public void onIgnoreFailure(FrescoDrawable2 drawable) {}
}
