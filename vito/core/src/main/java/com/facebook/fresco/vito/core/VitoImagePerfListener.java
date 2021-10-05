/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public interface VitoImagePerfListener {

  void onImageMount(FrescoDrawable2 drawable);

  void onImageUnmount(FrescoDrawable2 drawable);

  void onImageBind(FrescoDrawable2 drawable);

  void onImageUnbind(FrescoDrawable2 drawable);

  void onImageFetch(FrescoDrawable2 drawable);

  void onImageSuccess(FrescoDrawable2 drawable, boolean wasImmediate);

  void onImageError(FrescoDrawable2 drawable);

  void onImageRelease(FrescoDrawable2 drawable);

  void onScheduleReleaseDelayed(FrescoDrawable2 drawable);

  void onScheduleReleaseNextFrame(FrescoDrawable2 drawable);

  void onReleaseImmediately(FrescoDrawable2 drawable);

  void onDrawableReconfigured(FrescoDrawable2 drawable);

  void onIgnoreResult(FrescoDrawable2 drawable);

  void onIgnoreFailure(FrescoDrawable2 drawable);
}
