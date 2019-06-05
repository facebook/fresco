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
package com.facebook.fresco.samples.showcase

import android.app.Application
import android.content.Context
import com.facebook.common.logging.FLog
import com.facebook.common.memory.manager.NoOpDebugMemoryManager
import com.facebook.drawee.backends.pipeline.DraweeConfig
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.backends.pipeline.info.ImagePerfData
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.core.FlipperClient
import com.facebook.flipper.perflogger.NoOpFlipperPerfLogger
import com.facebook.flipper.plugins.fresco.FrescoFlipperPlugin
import com.facebook.flipper.plugins.fresco.FrescoFlipperRequestListener
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.fresco.samples.showcase.misc.DebugOverlaySupplierSingleton
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.provider.DefaultFrescoContext
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.debug.FlipperCacheKeyFactory
import com.facebook.imagepipeline.debug.FlipperImageTracker
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.facebook.imagepipeline.listener.ForwardingRequestListener
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.listener.RequestLoggingListener
import com.facebook.imagepipeline.memory.BitmapCounterConfig
import com.facebook.imagepipeline.memory.BitmapCounterProvider
import com.facebook.imagepipeline.stetho.FrescoStethoPlugin
import com.facebook.stetho.DumperPluginsProvider
import com.facebook.stetho.Stetho
import com.facebook.stetho.dumpapp.DumperPlugin
import com.facebook.stetho.okhttp3.StethoInterceptor
import java.util.HashSet
import okhttp3.OkHttpClient

/**
 * Showcase Application implementation where we set up Fresco
 */
class ShowcaseApplication : Application() {

    private var frescoFlipperPlugin: FrescoFlipperPlugin? = null

    override fun onCreate() {
        super.onCreate()
        imageUriProvider = ImageUriProvider(applicationContext)
        FLog.setMinimumLoggingLevel(FLog.VERBOSE)
        val forwardingRequestListener = ForwardingRequestListener()
        val requestListeners = HashSet<RequestListener>().apply {
            add(forwardingRequestListener)
            add(RequestLoggingListener())
        }

        val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .build()

        val imagePipelineConfigBuilder = OkHttpImagePipelineConfigFactory.newBuilder(this, okHttpClient)
                .setRequestListeners(requestListeners)
                .setProgressiveJpegConfig(SimpleProgressiveJpegConfig())
                .setImageDecoderConfig(CustomImageFormatConfigurator.createImageDecoderConfig(this))
                .experiment()
                .setBitmapPrepareToDraw(true, 0, Integer.MAX_VALUE, true)

        if (shouldEnableFlipper()) {
            imagePipelineConfigBuilder.setCacheKeyFactory(
                    FlipperCacheKeyFactory(sFlipperImageTracker))
        }

        val imagePipelineConfig = imagePipelineConfigBuilder.build()
        ImagePipelineConfig.getDefaultImageRequestConfig().isProgressiveRenderingEnabled = true

        val draweeConfigBuilder = DraweeConfig.newBuilder()
        CustomImageFormatConfigurator.addCustomDrawableFactories(this, draweeConfigBuilder)

        draweeConfigBuilder.setDebugOverlayEnabledSupplier(
                DebugOverlaySupplierSingleton.getInstance(applicationContext))

        DefaultFrescoContext.setDebugOverlayEnabledSupplier(DebugOverlaySupplierSingleton.getInstance(applicationContext))

        if (shouldEnableFlipper()) {
            draweeConfigBuilder.setImagePerfDataListener(
                    object : ImagePerfDataListener {
                        override fun onImageLoadStatusUpdated(imagePerfData: ImagePerfData, imageLoadStatus: Int) {
                            frescoFlipperPlugin?.flipperImageTracker?.onImageLoadStatusUpdated(imagePerfData, imageLoadStatus)
                            frescoFlipperPlugin?.onImageLoadStatusUpdated(imagePerfData, imageLoadStatus)
                        }

                        override fun onImageVisibilityUpdated(imagePerfData: ImagePerfData, visibilityState: Int) {
                            // nop
                        }
                    })
        }

        BitmapCounterProvider.initialize(
                BitmapCounterConfig.newBuilder()
                        .setMaxBitmapCount(BitmapCounterConfig.DEFAULT_MAX_BITMAP_COUNT)
                        .build())
        Fresco.initialize(this, imagePipelineConfig, draweeConfigBuilder.build())

        val context = this
        Stetho.initialize(
                Stetho.newInitializerBuilder(context)
                        .enableDumpapp {
                            Stetho.DefaultDumperPluginsBuilder(context)
                                    .provide(FrescoStethoPlugin())
                                    .finish()
                        }
                        .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(context))
                        .build())

        if (shouldEnableFlipper()) {
            frescoFlipperPlugin = FrescoFlipperPlugin(
                    sFlipperImageTracker,
                    Fresco.getImagePipelineFactory().platformBitmapFactory,
                    null,
                    NoOpDebugMemoryManager(),
                    NoOpFlipperPerfLogger(), null)
            forwardingRequestListener.addRequestListener(
                    FrescoFlipperRequestListener(frescoFlipperPlugin!!.flipperImageTracker))
            AndroidFlipperClient.getInstance(context).apply {
                addPlugin(InspectorFlipperPlugin(context, DescriptorMapping.withDefaults()))
                addPlugin(frescoFlipperPlugin)
                start()
            }
        }
    }

    private fun shouldEnableFlipper(): Boolean {
        return BuildConfig.DEBUG && FlipperUtils.shouldEnableFlipper(this)
    }

    companion object {
        private val sFlipperImageTracker = FlipperImageTracker()
        lateinit var imageUriProvider: ImageUriProvider
            private set
    }
}
