/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.facebook.common.callercontext.ContextChain;
import com.facebook.fresco.ui.common.OnFadeListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public interface FrescoController2 {

  <T extends Drawable & FrescoDrawableInterface> T createDrawable();

  boolean fetch(
      FrescoDrawableInterface drawableInterface,
      VitoImageRequest imageRequest,
      @Nullable Object callerContext,
      @Nullable ContextChain contextChain,
      @Nullable ImageListener listener,
      @Nullable OnFadeListener onFadeListener,
      @Nullable Rect viewportDimensions);

  void releaseDelayed(FrescoDrawableInterface drawableInterface);

  void release(FrescoDrawableInterface drawableInterface);

  void releaseImmediately(FrescoDrawableInterface drawableInterface);
}
