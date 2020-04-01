/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

public interface FrescoContext {

  ImagePipelineFactory getImagePipelineFactory();

  void setImagePipelineFactory(@Nullable ImagePipelineFactory imagePipelineFactory);

  Hierarcher getHierarcher();

  ImagePipeline getImagePipeline();

  FrescoController getController();

  FrescoExperiments getExperiments();

  @Nullable
  ImageListener getGlobalImageListener();

  ImageStateListener getGlobalImageStateListener();

  FrescoVitoPrefetcher getPrefetcher();

  ImagePipelineUtils getImagePipelineUtils();

  void verifyCallerContext(@Nullable Object callerContext);

  Executor getUiThreadExecutorService();

  Executor getLightweightBackgroundThreadExecutor();

  void setController(FrescoController controller);
}
