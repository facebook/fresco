// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.fresco.vito.view;

import com.facebook.fresco.vito.provider.FrescoVitoProvider;

public class LazyVitoViewImpl2 extends LazyVitoViewImpl {

  public LazyVitoViewImpl2(FrescoVitoProvider.Implementation provider) {
    super(provider);
  }

  @Override
  protected VitoView.Implementation create(FrescoVitoProvider.Implementation provider) {
    return new VitoViewImpl2(provider.getController(), provider.getImagePipeline());
  }
}
