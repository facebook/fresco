// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.fresco.vito.view;

import com.facebook.fresco.vito.provider.FrescoContextProvider;

public class LazyVitoViewImpl2 extends LazyVitoViewImpl {

  public LazyVitoViewImpl2(FrescoContextProvider.Implementation provider) {
    super(provider);
  }

  @Override
  protected VitoView.Implementation create(FrescoContextProvider.Implementation provider) {
    return new VitoViewImpl2(provider.getController(), provider.getImagePipeline());
  }
}
