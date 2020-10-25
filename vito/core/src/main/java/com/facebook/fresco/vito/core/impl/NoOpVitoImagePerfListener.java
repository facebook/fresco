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
}
