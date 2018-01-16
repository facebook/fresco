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
import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.samples.showcase.imagepipeline.DurationCallback;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.request.BasePostprocessor;
import javax.annotation.Nullable;

/**
 * Postprocessor that measures the performance of {@link BasePostprocessor#process(Bitmap,
 * PlatformBitmapFactory)}.
 */
public class BenchmarkPostprocessorForManualBitmapHandling
    extends BasePostprocessorWithDurationCallback {

  private final BasePostprocessor mPostprocessor;

  public BenchmarkPostprocessorForManualBitmapHandling(
      DurationCallback durationCallback, BasePostprocessor postprocessor) {
    super(durationCallback);
    mPostprocessor = postprocessor;
  }

  @Override
  public CloseableReference<Bitmap> process(
      Bitmap sourceBitmap, PlatformBitmapFactory bitmapFactory) {
    long startTime = System.nanoTime();
    CloseableReference<Bitmap> result = mPostprocessor.process(sourceBitmap, bitmapFactory);
    showDuration(System.nanoTime() - startTime);
    return result;
  }

  @Nullable
  @Override
  public CacheKey getPostprocessorCacheKey() {
    return mPostprocessor.getPostprocessorCacheKey();
  }
}
