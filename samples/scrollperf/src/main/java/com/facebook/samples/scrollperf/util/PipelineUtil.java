/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.util;

import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.postprocessor.DelayPostprocessor;

/** Utility class in order to manage Pipeline objects */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class PipelineUtil {

  /**
   * Utility method which adds optional configuration to ImageRequest
   *
   * @param imageOptionsBuilder The Builder for ImageOptions
   * @param config The Config
   */
  public static void addOptionalFeatures(ImageOptions.Builder imageOptionsBuilder, Config config) {
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
      imageOptionsBuilder.postprocess(postprocessor);
    }
    if (config.rotateUsingMetaData) {
      imageOptionsBuilder.rotate(RotationOptions.autoRotateAtRenderTime());
    } else {
      imageOptionsBuilder.rotate(RotationOptions.forceRotation(config.forcedRotationAngle));
    }
  }
}
