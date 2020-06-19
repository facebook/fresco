/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view.impl;

import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.view.VitoView;

public abstract class LazyVitoViewImpl implements VitoView.Implementation {

  private final FrescoVitoProvider.Implementation mProvider;
  private @Nullable VitoView.Implementation mImplementation;

  public LazyVitoViewImpl(FrescoVitoProvider.Implementation provider) {
    mProvider = provider;
  }

  @Override
  public void show(
      ImageSource imageSource,
      ImageOptions imageOptions,
      @Nullable Object callerContext,
      @Nullable ImageListener imageListener,
      View target) {
    get().show(imageSource, imageOptions, callerContext, imageListener, target);
  }

  private synchronized VitoView.Implementation get() {
    if (mImplementation == null) {
      mImplementation = create(mProvider);
    }
    return mImplementation;
  }

  protected abstract VitoView.Implementation create(FrescoVitoProvider.Implementation provider);
}
