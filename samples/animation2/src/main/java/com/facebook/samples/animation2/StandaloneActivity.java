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

import javax.annotation.Nullable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.time.RealtimeSinceBootClock;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;

/**
 * Simple standalone activity that creates a new {@link AnimatedDrawable2} and a custom backend
 * that cycles through colors.
 * Tap the view to start / stop the animation.
 *
 * When the animation is inactive for more than 2 seconds (no new frames drawn), a toast message
 * will be displayed.
 */
public class StandaloneActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_standalone);

    // Get the animation container
    View animationContainer = findViewById(R.id.animation_container);

    // Create the animation backend
    // In addition, we wrap it with an activity check. Tap the view to stop the animation to see
    // an inactivity toast message after 2 seconds.
    // In order to remove the inactivity check, just remove the wrapper method and set it to
    // the backend directly.
    AnimationBackend animationBackend = wrapAnimationBackendWithInactivityCheck(
        createColorAnimationBackend());

    // Create a new animated drawable with the example backend
    final AnimatedDrawable2 animatedDrawable = new AnimatedDrawable2(animationBackend);

    // Set the animation as a background
    animationContainer.setBackgroundDrawable(animatedDrawable);

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

  /**
   * Wraps the given animation backend with an activity check.
   * When no frame has been drawn for more than 2 seconds, an inactivity toast message will
   * be displayed.
   *
   * @param animationBackend the backend to wrap
   * @return the backend to use
   */
  private AnimationBackend wrapAnimationBackendWithInactivityCheck(
      AnimationBackend animationBackend) {
    AnimationBackendDelegateWithInactivityCheck.InactivityListener inactivityListener =
        new AnimationBackendDelegateWithInactivityCheck.InactivityListener() {
      @Override
      public void onInactive() {
        Toast.makeText(
            StandaloneActivity.this,
            "Animation backend inactive.",
            Toast.LENGTH_SHORT)
            .show();
      }
    };
    return AnimationBackendDelegateWithInactivityCheck.createForBackend(
        animationBackend,
        inactivityListener,
        RealtimeSinceBootClock.get(),
        UiThreadImmediateExecutorService.getInstance());
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
