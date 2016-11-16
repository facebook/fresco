/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.samples.scrollperf.util;

import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.postprocessor.DelayPostprocessor;

/**
 * Utility class in order to manage Pipeline objects
 */
public final class PipelineUtil {

  /**
   * Utility method which adds optional configuration to ImageRequest
   *
   * @param imageRequestBuilder The Builder for ImageRequest
   * @param config              The Config
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
      imageRequestBuilder
          .setRotationOptions(RotationOptions.forceRotation(config.forcedRotationAngle));
    }
  }
}
