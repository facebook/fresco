/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider;

import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoVitoConfig;
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class FrescoVitoProvider {

  public interface Implementation {
    FrescoController2 getController();

    FrescoVitoPrefetcher getPrefetcher();

    VitoImagePipeline getImagePipeline();

    FrescoVitoConfig getConfig();
  }

  @Nullable private static Implementation sImplementation;

  public static synchronized FrescoController2 getController() {
    return getImplementation().getController();
  }

  public static synchronized FrescoVitoPrefetcher getPrefetcher() {
    return getImplementation().getPrefetcher();
  }

  public static synchronized VitoImagePipeline getImagePipeline() {
    return getImplementation().getImagePipeline();
  }

  public static synchronized FrescoVitoConfig getConfig() {
    return getImplementation().getConfig();
  }

  public static synchronized void setImplementation(Implementation implementation) {
    sImplementation = implementation;
  }

  private static synchronized Implementation getImplementation() {
    if (sImplementation == null) {
      throw new RuntimeException("Fresco context provider must be set");
    }
    return sImplementation;
  }
}
