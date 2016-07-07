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

package com.facebook.samples.animation;


import java.util.HashSet;
import java.util.Set;
import android.widget.ToggleButton;
import android.widget.CompoundButton;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;

import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;

public class MainActivity extends Activity {

  private SimpleDraweeView mAnimatedGifView;
  private SimpleDraweeView mAnimatedWebpView;

  private Animatable animatableGif;

  private Animatable animatableWebp;



  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    FLog.setMinimumLoggingLevel(FLog.VERBOSE);
    Set<RequestListener> listeners = new HashSet<>();
    listeners.add(new RequestLoggingListener());
    ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
        .setRequestListeners(listeners)
        .setBitmapsConfig(Bitmap.Config.ARGB_8888)
        .build();
    Fresco.initialize(this, config);
    setContentView(R.layout.activity_main);


    final ToggleButton gifToggle = (ToggleButton) findViewById(R.id.toggle_gif);
    gifToggle.setEnabled(false);
    mAnimatedGifView = (SimpleDraweeView) findViewById(R.id.animated_gif);
    Uri animatedGifUri =
        Uri.parse("http://s3.amazonaws.com/giphygifs/media/4aBQ9oNjgEQ2k/giphy.gif");

    DraweeController gifController = Fresco.newDraweeControllerBuilder()
        .setUri(animatedGifUri)
        .setControllerListener(new BaseControllerListener<ImageInfo>() {
          @Override
          public void onFinalImageSet(
              String id,
              ImageInfo imageInfo,
              Animatable anim) {
            if (anim != null) {
              animatableGif = anim;
              // app-specific logic to enable animation starting
              gifToggle.setEnabled(true);
            }
          }
        })
        .build();
    mAnimatedGifView.setController(gifController);

    gifToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (animatableGif == null) {
          return;
        }
        if (isChecked) {
          animatableGif.start();
        } else {
          animatableGif.stop();
        }
      }
    });





    final ToggleButton webpToggle = (ToggleButton) findViewById(R.id.toggle_webp);
    webpToggle.setEnabled(false);
    mAnimatedWebpView = (SimpleDraweeView) findViewById(R.id.animated_webp);
    Uri animatedWebpUri = Uri.parse("http://www.gstatic.com/webp/animated/1.webp");
    DraweeController webpController = Fresco.newDraweeControllerBuilder()
        .setUri(animatedWebpUri)
        .setControllerListener(new BaseControllerListener<ImageInfo>() {
          @Override
          public void onFinalImageSet(
              String id,
              ImageInfo imageInfo,
              Animatable anim) {
            if (anim != null) {
              animatableWebp = anim;
              // app-specific logic to enable animation starting
              webpToggle.setEnabled(true);
            }
          }
        })
        .build();
    mAnimatedWebpView.setController(webpController);

    webpToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (animatableWebp == null) {
          return;
        }
        if (isChecked) {
          animatableWebp.start();
        } else {
          animatableWebp.stop();
        }
      }
    });

  }
}
