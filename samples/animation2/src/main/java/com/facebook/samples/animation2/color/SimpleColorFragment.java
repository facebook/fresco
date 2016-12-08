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
package com.facebook.samples.animation2.color;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.samples.animation2.R;
import com.facebook.samples.animation2.utils.AnimationBackendUtils;

/**
 * Simple standalone activity that creates a new {@link AnimatedDrawable2} and a custom backend
 * that cycles through colors.
 * Tap the view to start / stop the animation.
 *
 * When the animation is inactive for more than 2 seconds (no new frames drawn), a toast message
 * will be displayed.
 */
public class SimpleColorFragment extends Fragment {

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_simple_container, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    // Get the animation container
    ImageView animationContainer = (ImageView) view.findViewById(R.id.animation_container);

    // Create the animation backend
    // In addition, we wrap it with an activity check. Tap the view to stop the animation to see
    // an inactivity toast message after 2 seconds.
    // In order to remove the inactivity check, just remove the wrapper method and set it to
    // the backend directly.
    AnimationBackend animationBackend =
        AnimationBackendUtils.wrapAnimationBackendWithInactivityCheck(
            getContext(),
            createColorAnimationBackend());

    // Create a new animated drawable with the example backend
    final AnimatedDrawable2 animatedDrawable = new AnimatedDrawable2(animationBackend);

    // Set the animation as a background
    animationContainer.setImageDrawable(animatedDrawable);

    // Add a click listener to start / stop the animation
    animationContainer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (animatedDrawable.isRunning()) {
          animatedDrawable.stop();
        } else {
          animatedDrawable.start();
        }
      }
    });

    // Start the animation
    animatedDrawable.start();
  }

  /**
   * Creates a simple animation backend that cycles through a list of colors.
   *
   * @return the backend to use
   */
  private AnimationBackend createColorAnimationBackend() {
    // Define the colors
    int[] colors = {
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.CYAN,
        Color.BLACK,
        Color.GRAY,
    };
    // Get the animation duration in ms for each color frame
    int frameDurationMs = getResources().getInteger(android.R.integer.config_mediumAnimTime);
    // Create and return the backend
    return new ExampleColorBackend(colors, frameDurationMs);
  }

  private static class ExampleColorBackend implements AnimationBackend {

    private final Paint mPaint = new Paint();
    private final int[] mColors;
    private final int mFrameDurationMs;

    private Rect mBounds;

    private ExampleColorBackend(int[] colors, int frameDurationMs) {
      mColors = colors;
      mFrameDurationMs = frameDurationMs;
    }

    @Override
    public int getFrameCount() {
      return mColors.length;
    }

    @Override
    public int getFrameDurationMs(int frameNumber) {
      return mFrameDurationMs;
    }

    @Override
    public int getLoopCount() {
      return 3;
    }

    @Override
    public boolean drawFrame(
        Drawable parent, Canvas canvas, int frameNumber) {
      if (mBounds == null) {
        return false;
      }
      mPaint.setColor(mColors[frameNumber]);
      canvas.drawRect(mBounds, mPaint);
      return true;
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
      mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
      mPaint.setColorFilter(colorFilter);
    }

    @Override
    public void setBounds(Rect bounds) {
      mBounds = bounds;
    }

    @Override
    public int getSizeInBytes() {
      return mColors.length * 4;
    }

    @Override
    public void clear() {
    }

    @Override
    public int getIntrinsicWidth() {
      return INTRINSIC_DIMENSION_UNSET;
    }

    @Override
    public int getIntrinsicHeight() {
      return INTRINSIC_DIMENSION_UNSET;
    }
  }
}
