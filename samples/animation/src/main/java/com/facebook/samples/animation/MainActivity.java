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


import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.animated.base.AbstractAnimatedDrawable;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.image.ImageInfo;

public class MainActivity extends Activity {

  private SimpleDraweeView mAnimatedGifView;
  private SimpleDraweeView mAnimatedWebpView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // GIF
    final ViewGroup gifControls = (ViewGroup) findViewById(R.id.gif_controls);
    updateAnimationControls(gifControls, null);

    mAnimatedGifView = (SimpleDraweeView) findViewById(R.id.animated_gif);
    Uri animatedGifUri =
        Uri.parse("http://s3.amazonaws.com/giphygifs/media/4aBQ9oNjgEQ2k/giphy.gif");

    final TextView gifInfo = (TextView) findViewById(R.id.gif_info);

    DraweeController gifController = Fresco.newDraweeControllerBuilder()
        .setUri(animatedGifUri)
        .setControllerListener(new BaseControllerListener<ImageInfo>() {
          @Override
          public void onFinalImageSet(
              String id,
              ImageInfo imageInfo,
              Animatable anim) {
            updateAnimationControls(gifControls, anim);
            gifInfo.setText(getAnimationInformation(anim));
          }
        })
        .build();
    mAnimatedGifView.setController(gifController);

    // Animated WebP
    final ViewGroup webpControls = (ViewGroup) findViewById(R.id.webp_controls);
    updateAnimationControls(webpControls, null);

    mAnimatedWebpView = (SimpleDraweeView) findViewById(R.id.animated_webp);
    final TextView webpInfo = (TextView) findViewById(R.id.webp_info);

    Uri animatedWebpUri = Uri.parse("http://www.gstatic.com/webp/animated/1.webp");
    DraweeController webpController = Fresco.newDraweeControllerBuilder()
        .setUri(animatedWebpUri)
        .setControllerListener(new BaseControllerListener<ImageInfo>() {
          @Override
          public void onFinalImageSet(
              String id,
              ImageInfo imageInfo,
              Animatable anim) {
            updateAnimationControls(webpControls, anim);
            webpInfo.setText(getAnimationInformation(anim));
          }
        })
        .build();
    mAnimatedWebpView.setController(webpController);
  }

  public String getAnimationInformation(Animatable animatable) {
    if (animatable instanceof AbstractAnimatedDrawable) {
      AbstractAnimatedDrawable animatedDrawable = (AbstractAnimatedDrawable) animatable;
      int animationDuration = animatedDrawable.getDuration();
      int frameCount = animatedDrawable.getFrameCount();
      String loopCountString = getLoopCountString(animatedDrawable);
      return getString(R.string.animation_info, animationDuration, frameCount, loopCountString);
    }
    return getString(R.string.unknown_animation_info);
  }

  public boolean tryPausing(Animatable animatable) {
    // AbstractAnimatedDrawable supports pausing, but we could also have a different Animatable
    if (animatable instanceof AbstractAnimatedDrawable) {
      AbstractAnimatedDrawable animatedDrawable = (AbstractAnimatedDrawable) animatable;
      animatedDrawable.pause();
      return true;
    } else {
      Toast.makeText(this, "Could not pause animation", Toast.LENGTH_SHORT).show();
      return false;
    }
  }

  private void updateAnimationControls(
          ViewGroup controlsContainer,
          @Nullable final Animatable animatable) {
    Button play = (Button) controlsContainer.findViewById(R.id.play);
    Button pause = (Button) controlsContainer.findViewById(R.id.pause);
    Button stop = (Button) controlsContainer.findViewById(R.id.stop);

    play.setEnabled(animatable != null);
    pause.setEnabled(animatable != null);
    stop.setEnabled(animatable != null);

    play.setOnClickListener(animatable == null ? null : new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        animatable.start();
      }
    });

    pause.setOnClickListener(animatable == null ? null : new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        tryPausing(animatable);
      }
    });

    stop.setOnClickListener(animatable == null ? null : new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        animatable.stop();
      }
    });
  }

  private String getLoopCountString(AbstractAnimatedDrawable animatedDrawable) {
    return animatedDrawable.getLoopCount() == AnimatedImage.LOOP_COUNT_INFINITE
            ? getString(R.string.infinite)
            : animatedDrawable.getLoopCount() + "";
  }
}
