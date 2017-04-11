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
package com.facebook.fresco.samples.showcase;

import java.util.HashSet;
import java.util.Set;

import android.app.Application;
import android.content.Context;

import com.facebook.common.internal.Supplier;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.DraweeConfig;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.samples.showcase.imagepipeline.ShowcaseMediaIdExtractor;
import com.facebook.fresco.samples.showcase.misc.DebugOverlaySupplierSingleton;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;
import com.facebook.imagepipeline.stetho.FrescoStethoPlugin;
import com.facebook.stetho.DumperPluginsProvider;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.dumpapp.DumperPlugin;

/**
 * Showcase Application implementation where we set up Fresco
 */
public class ShowcaseApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    FLog.setMinimumLoggingLevel(FLog.VERBOSE);
    Set<RequestListener> listeners = new HashSet<>();
    listeners.add(new RequestLoggingListener());

    ImagePipelineConfig imagePipelineConfig = ImagePipelineConfig.newBuilder(this)
        .setRequestListeners(listeners)
        .setImageDecoderConfig(CustomImageFormatConfigurator.createImageDecoderConfig(this))
        .experiment().setMediaVariationsIndexEnabled(new Supplier<Boolean>() {
          @Override
          public Boolean get() {
            return true;
          }
        })
        .experiment().setMediaIdExtractor(new ShowcaseMediaIdExtractor())
        .build();

    DraweeConfig.Builder draweeConfigBuilder = DraweeConfig.newBuilder();
    CustomImageFormatConfigurator.addCustomDrawableFactories(this, draweeConfigBuilder);

    draweeConfigBuilder.setDebugOverlayEnabledSupplier(
        DebugOverlaySupplierSingleton.getInstance(getApplicationContext()));

    Fresco.initialize(this, imagePipelineConfig, draweeConfigBuilder.build());

    final Context context = this;
    Stetho.initialize(Stetho.newInitializerBuilder(context)
        .enableDumpapp(new DumperPluginsProvider() {
          @Override
          public Iterable<DumperPlugin> get() {
            return new Stetho.DefaultDumperPluginsBuilder(context)
                .provide(new FrescoStethoPlugin())
                .finish();
          }
        })
        .build());
  }
}
