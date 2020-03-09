/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view;

import android.net.Uri;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.FrescoContextProvider;
import com.facebook.imagepipeline.multiuri.MultiUri;

public abstract class LazyVitoViewImpl implements VitoView.Implementation {

  private final FrescoContextProvider.Implementation mProvider;
  private @Nullable VitoView.Implementation mImplementation;

  public LazyVitoViewImpl(FrescoContextProvider.Implementation provider) {
    mProvider = provider;
  }

  @Override
  public void show(
      Uri uri,
      MultiUri multiUri,
      ImageOptions imageOptions,
      Object callerContext,
      ImageListener imageListener,
      View target) {
    get().show(uri, multiUri, imageOptions, callerContext, imageListener, target);
  }

  private synchronized VitoView.Implementation get() {
    if (mImplementation == null) {
      mImplementation = create(mProvider);
    }
    return mImplementation;
  }

  protected abstract VitoView.Implementation create(FrescoContextProvider.Implementation provider);
}
