/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.fresco.vito.listener.ImageListener;
import javax.annotation.Nullable;

public interface FrescoController2 {

  boolean fetch(
      FrescoDrawable2 frescoDrawable,
      VitoImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable ImageListener listener);

  void releaseDelayed(FrescoDrawable2 drawable);

  void release(FrescoDrawable2 drawable);
}
