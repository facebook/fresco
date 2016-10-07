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
package com.facebook.samples.decoders;

import java.util.HashSet;
import java.util.Set;

import android.app.Application;

import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.DraweeConfig;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.decoder.ImageDecoderConfig;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;
import com.facebook.samples.decoders.color.ColorImageExample;

/**
 * Demo Application implementation where we set up Fresco
 */
public class CustomDecoderApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FLog.setMinimumLoggingLevel(FLog.VERBOSE);
        Set<RequestListener> listeners = new HashSet<>();
        listeners.add(new RequestLoggingListener());

        // Add custom decoding capabilities to the image decoder config
        ImageDecoderConfig imageDecoderConfig = ImageDecoderConfig.newBuilder()
            .addDecodingCapability(
                ColorImageExample.COLOR,
                ColorImageExample.getChecker(),
                ColorImageExample.getDecoder())
            .build();

        // Set the image decoder config to be used by the image pipeline
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setRequestListeners(listeners)
                .setImageDecoderConfig(imageDecoderConfig)
                .build();

        // Add a Drawee config so that we're able to correctly render our custom images
        DraweeConfig draweeConfig = DraweeConfig.newBuilder()
            .addCustomDrawableFactory(new ColorImageExample.ColorDrawableFactory())
            .build();

        // Initialize Fresco with our configurations
        Fresco.initialize(this, config, draweeConfig);
    }
}
