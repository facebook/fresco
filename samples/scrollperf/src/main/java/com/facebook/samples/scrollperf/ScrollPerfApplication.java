/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf;

import android.app.Application;
import com.facebook.common.internal.Suppliers;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.vito.init.FrescoVito;
import com.facebook.imagepipeline.core.DefaultExecutorSupplier;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.MemoryChunkType;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.conf.Const;
import com.facebook.samples.scrollperf.internal.ScrollPerfExecutorSupplier;

/** Application for Fresco initialization */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ScrollPerfApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    final Config config = Config.load(this);
    ImagePipelineConfig.Builder imagePipelineConfigBuilder =
        ImagePipelineConfig.newBuilder(this)
            .setResizeAndRotateEnabledForNetwork(false)
            .setDownsampleEnabled(config.downsampling);
    if (config.decodingThreadCount == 0) {
      imagePipelineConfigBuilder.setExecutorSupplier(
          new DefaultExecutorSupplier(Const.NUMBER_OF_PROCESSORS));
    } else {
      imagePipelineConfigBuilder.setExecutorSupplier(
          new ScrollPerfExecutorSupplier(Const.NUMBER_OF_PROCESSORS, config.decodingThreadCount));
    }
    imagePipelineConfigBuilder.experiment().setDecodeCancellationEnabled(config.decodeCancellation);
    if (BuildConfig.FLAVOR == "noNativeCode") {
      imagePipelineConfigBuilder.setMemoryChunkType(MemoryChunkType.BUFFER_MEMORY);
      Fresco.initialize(this, imagePipelineConfigBuilder.build(), null, false);
    } else {
      Fresco.initialize(this, imagePipelineConfigBuilder.build(), null, true);
    }
    FrescoVito.initialize(null, null, null, Suppliers.of(config.vitoOverlayEnabled));
  }
}
