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

  void onImageMount(FrescoDrawableInterface drawable);

  void onImageUnmount(FrescoDrawableInterface drawable);

  void onImageBind(FrescoDrawableInterface drawable);

  void onImageUnbind(FrescoDrawableInterface drawable);

  void onImageFetch(FrescoDrawableInterface drawable);

  void onImageSuccess(FrescoDrawableInterface drawable, boolean wasImmediate);

  void onImageError(FrescoDrawableInterface drawable);

  void onImageRelease(FrescoDrawableInterface drawable);

  void onScheduleReleaseDelayed(FrescoDrawableInterface drawable);

  void onScheduleReleaseNextFrame(FrescoDrawableInterface drawable);

  void onReleaseImmediately(FrescoDrawableInterface drawable);

  void onDrawableReconfigured(FrescoDrawableInterface drawable);

  void onIgnoreResult(FrescoDrawableInterface drawable);

  void onIgnoreFailure(FrescoDrawableInterface drawable);
}
