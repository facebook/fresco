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
package com.facebook.fresco.samples.showcase.postprocessor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.samples.showcase.imagepipeline.DurationCallback;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.nativecode.NativeBlurFilter;

/**
 * Applies a blur filter using the {@link NativeBlurFilter#iterativeBoxBlur(Bitmap, int, int)} and
 * down-scales the bitmap beforehand.
 */
public class ScalingBlurPostprocessor extends BlurPostprocessor {

  /**
   * A scale ration of 4 means that we reduce the total number of pixels to process by factor 16.
   */
  private static final int SCALE_RATIO = 4;

  private final Paint mPaint = new Paint();

  public ScalingBlurPostprocessor(DurationCallback durationCallback) {
    super(durationCallback);
  }

  @Override
  public CloseableReference<Bitmap> process(
      Bitmap sourceBitmap,
      PlatformBitmapFactory bitmapFactory) {
    final long startNs = System.nanoTime();

    final CloseableReference<Bitmap> bitmapRef = bitmapFactory.createBitmap(
        sourceBitmap.getWidth() / SCALE_RATIO,
        sourceBitmap.getHeight() / SCALE_RATIO);

    try {
      final Bitmap destBitmap = bitmapRef.get();
      final Canvas canvas = new Canvas(destBitmap);

      canvas.drawBitmap(
          sourceBitmap,
          null,
          new Rect(0, 0, destBitmap.getWidth(), destBitmap.getHeight()),
          mPaint);

      NativeBlurFilter.iterativeBoxBlur(destBitmap, BLUR_RADIUS / SCALE_RATIO, BLUR_ITERATIONS);

      showDuration(System.nanoTime() - startNs);

      return CloseableReference.cloneOrNull(bitmapRef);
    } finally {
      CloseableReference.closeSafely(bitmapRef);
    }
  }
}
