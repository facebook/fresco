/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

public interface VitoImagePerfListener {

  void onImageMount(FrescoDrawable2 drawable);

  void onImageUnmount(FrescoDrawable2 drawable);

  void onImageBind(FrescoDrawable2 drawable);

  void onImageUnbind(FrescoDrawable2 drawable);
}
