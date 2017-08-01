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
package com.facebook.samples.scrollperf;

import android.app.Application;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.drawee.backends.pipeline.DraweeConfig;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.DefaultExecutorSupplier;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.conf.Const;
import com.facebook.samples.scrollperf.internal.ScrollPerfExecutorSupplier;

/**
 * Application for Fresco initialization
 */
public class ScrollPerfApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    final Config config = Config.load(this);
    ImagePipelineConfig.Builder imagePipelineConfigBuilder = ImagePipelineConfig.newBuilder(this)
        .setResizeAndRotateEnabledForNetwork(false)
        .setDownsampleEnabled(config.downsampling);
    if (WebpSupportStatus.sIsWebpSupportRequired) {
      imagePipelineConfigBuilder.experiment().setWebpSupportEnabled(config.webpSupportEnabled);
    }
    if (config.decodingThreadCount == 0) {
      imagePipelineConfigBuilder.setExecutorSupplier(
          new DefaultExecutorSupplier(Const.NUMBER_OF_PROCESSORS));
    } else {
      imagePipelineConfigBuilder.setExecutorSupplier(
          new ScrollPerfExecutorSupplier(Const.NUMBER_OF_PROCESSORS, config.decodingThreadCount));
    }
    imagePipelineConfigBuilder.experiment().setDecodeCancellationEnabled(config.decodeCancellation);
    DraweeConfig draweeConfig = DraweeConfig.newBuilder()
        .setDrawDebugOverlay(config.draweeOverlayEnabled)
        .build();
    Fresco.initialize(this, imagePipelineConfigBuilder.build(), draweeConfig);
  }
}
