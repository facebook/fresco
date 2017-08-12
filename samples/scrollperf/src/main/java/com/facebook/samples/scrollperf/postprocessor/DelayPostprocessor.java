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
package com.facebook.samples.scrollperf.postprocessor;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.samples.scrollperf.util.TimeWaster;

/**
 * This is a Postprocessor which just introduce a delay
 */
public final class DelayPostprocessor extends BasePostprocessor {

  private static DelayPostprocessor sSlowPostprocessor;

  private static DelayPostprocessor sMediumPostprocessor;

  private static DelayPostprocessor sFastPostprocessor;

  private final int mDelay;

  private DelayPostprocessor(final int delay) {
    this.mDelay = delay;
  }

  public static DelayPostprocessor getSlowPostprocessor() {
    if (sSlowPostprocessor == null) {
      sSlowPostprocessor = new DelayPostprocessor(20);
    }
    return sSlowPostprocessor;
  }

  public static DelayPostprocessor getMediumPostprocessor() {
    if (sMediumPostprocessor == null) {
      sMediumPostprocessor = new DelayPostprocessor(10);
    }
    return sMediumPostprocessor;
  }

  public static DelayPostprocessor getFastPostprocessor() {
    if (sFastPostprocessor == null) {
      sFastPostprocessor = new DelayPostprocessor(5);
    }
    return sFastPostprocessor;
  }

  @Override
  public void process(Bitmap bitmap) {
    TimeWaster.Fib(mDelay);
  }
}
