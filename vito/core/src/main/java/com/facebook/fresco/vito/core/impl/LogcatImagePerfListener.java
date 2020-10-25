/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.util.Log;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.VitoImagePerfListener;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class LogcatImagePerfListener implements VitoImagePerfListener {

  private static final String TAG = "LogcatImagePerfListener";

  @Override
  public void onImageMount(FrescoDrawable2 drawable) {
    log("onImageMount", drawable);
  }

  @Override
  public void onImageUnmount(FrescoDrawable2 drawable) {
    log("onImageUnmount", drawable);
  }

  @Override
  public void onImageBind(FrescoDrawable2 drawable) {
    log("onImageBind", drawable);
  }

  @Override
  public void onImageUnbind(FrescoDrawable2 drawable) {
    log("onImageUnbind", drawable);
  }

  @Override
  public void onImageFetch(FrescoDrawable2 drawable) {
    log("onImageRequested", drawable);
  }

  @Override
  public void onImageSuccess(FrescoDrawable2 drawable, boolean wasImmediate) {
    log("onImageSuccess - immediate: " + wasImmediate, drawable);
  }

  @Override
  public void onImageError(FrescoDrawable2 drawable) {
    log("onImageError", drawable);
  }

  @Override
  public void onImageRelease(FrescoDrawable2 drawable) {
    log("onImageRelease", drawable);
  }

  @Override
  public void onScheduleReleaseDelayed(FrescoDrawable2 drawable) {
    log("onImageReleaseDelayed", drawable);
  }

  @Override
  public void onScheduleReleaseNextFrame(FrescoDrawable2 drawable) {
    log("onScheduleReleaseNextFrame", drawable);
  }

  @Override
  public void onReleaseImmediately(FrescoDrawable2 drawable) {
    log("onReleaseImmediately", drawable);
  }

  @Override
  public void onDrawableReconfigured(FrescoDrawable2 drawable) {
    log("onReconfigured", drawable);
  }

  @Override
  public void onIgnoreResult(FrescoDrawable2 drawable) {
    log("onIgnoreResult", drawable);
  }

  @Override
  public void onIgnoreFailure(FrescoDrawable2 drawable) {
    log("onIgnoreFailure", drawable);
  }

  private static void log(String name, FrescoDrawable2 drawable) {
    log(name, drawable, null);
  }

  private static void log(String name, FrescoDrawable2 drawable, @Nullable String text) {
    Log.d(TAG, name + ": " + drawable.getImageId() + (text != null ? " - " + text : ""));
  }
}
