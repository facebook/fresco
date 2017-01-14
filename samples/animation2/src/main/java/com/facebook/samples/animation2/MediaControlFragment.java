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
package com.facebook.samples.animation2;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.samples.animation2.color.ExampleColorBackend;
import com.facebook.samples.animation2.local.LocalDrawableAnimationBackend;
import com.facebook.samples.animation2.utils.AnimationBackendUtils;
import com.facebook.samples.animation2.utils.AnimationControlsManager;

/**
 * Sample that displays an animated image and media controls to start / stop / seek.
 */
public class MediaControlFragment extends Fragment {

  private AnimationControlsManager mAnimationControlsManager;
  private AnimatedDrawable2 mAnimatedDrawable;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_media_controls, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    // Get the animation container
    ImageView imageView = (ImageView) view.findViewById(R.id.animation_container);

    // Create the animation backend
    // In addition, we wrap it with an activity check. Tap the view to stop the animation to see
    // an inactivity toast message after 2 seconds.
    // In order to remove the inactivity check, just remove the wrapper method and set it to
    // the backend directly.
    AnimationBackend animationBackend =
        AnimationBackendUtils.wrapAnimationBackendWithInactivityCheck(
            getContext(),
            createLocalDrawableBackend());


    // Create a new animated drawable, assign it to the image view and start the animation.
    mAnimatedDrawable = new AnimatedDrawable2(animationBackend);

    imageView.setImageDrawable(mAnimatedDrawable);

    mAnimationControlsManager = new AnimationControlsManager(
        mAnimatedDrawable,
        (SeekBar) view.findViewById(R.id.seekbar),
        (ToggleButton) view.findViewById(R.id.playpause),
        view.findViewById(R.id.reset));
  }

  private AnimationBackend createColorBackend() {
    return ExampleColorBackend.createSampleColorAnimationBackend(getResources());
  }

  private AnimationBackend createLocalDrawableBackend() {
    return new LocalDrawableAnimationBackend.Builder(getResources())
        .addDrawableFrame(R.mipmap.ic_alarm)
        .addDrawableFrame(R.mipmap.ic_android)
        .addDrawableFrame(R.mipmap.ic_launcher)
        .loopCount(3)
        .frameDurationMs(500)
        .build();
  }
}
