/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.util;

import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.postprocessor.DelayPostprocessor;

/** Utility class in order to manage Pipeline objects */
public final class PipelineUtil {

  /**
   * Utility method which adds optional configuration to ImageRequest
   *
   * @param imageRequestBuilder The Builder for ImageRequest
   * @param config The Config
   */
  public static void addOptionalFeatures(ImageRequestBuilder imageRequestBuilder, Config config) {
    if (config.usePostprocessor) {
      final Postprocessor postprocessor;
      switch (config.postprocessorType) {
        case "use_slow_postprocessor":
          postprocessor = DelayPostprocessor.getMediumPostprocessor();
          break;
        case "use_fast_postprocessor":
          postprocessor = DelayPostprocessor.getFastPostprocessor();
          break;
        default:
          postprocessor = DelayPostprocessor.getMediumPostprocessor();
      }
      imageRequestBuilder.setPostprocessor(postprocessor);
    }
    if (config.rotateUsingMetaData) {
      imageRequestBuilder.setRotationOptions(RotationOptions.autoRotateAtRenderTime());
    } else {
      imageRequestBuilder.setRotationOptions(
          RotationOptions.forceRotation(config.forcedRotationAngle));
    }
  }
}
