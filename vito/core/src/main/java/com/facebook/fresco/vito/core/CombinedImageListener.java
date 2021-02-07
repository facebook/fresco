/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public interface CombinedImageListener extends VitoImageRequestListener {

  void setImageListener(@Nullable ImageListener imageListener);

  void setVitoImageRequestListener(@Nullable VitoImageRequestListener vitoImageRequestListener);

  void setControllerListener2(@Nullable ControllerListener2<ImageInfo> controllerListener2);

  @Nullable
  ImageListener getImageListener();

  void onReset();
}
