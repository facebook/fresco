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
import com.facebook.fresco.samples.showcase.imagepipeline.DurationCallback;
import com.facebook.imagepipeline.nativecode.NativeBlurFilter;

/**
 * Applies a blur filter using the {@link NativeBlurFilter#iterativeBoxBlur(Bitmap, int, int)}, but
 * does not down-scale the image beforehand.
 */
public class BlurPostprocessor extends BasePostprocessorWithDurationCallback {

  protected static final int BLUR_RADIUS = 25;
  protected static final int BLUR_ITERATIONS = 3;

  public BlurPostprocessor(DurationCallback durationCallback) {
    super(durationCallback);
  }

  public void process(Bitmap bitmap) {
    final long startNs = System.nanoTime();

    NativeBlurFilter.iterativeBoxBlur(bitmap, BLUR_RADIUS, BLUR_ITERATIONS);

    showDuration(System.nanoTime() - startNs);
  }
}
