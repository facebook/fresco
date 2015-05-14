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

package com.facebook.samples.comparison.instrumentation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import com.facebook.samples.comparison.Drawables;
import com.facebook.common.internal.Preconditions;

/**
 * {@link ImageView} that notifies its instance of {@link Instrumentation} whenever an image request
 * lifecycle event happens.
 *
 * <p> setImageResource and setImageURI methods are not expected to be used by any library,
 * UnsupportedOperationException is thrown if those are called
 */
public class InstrumentedImageView extends ImageView implements Instrumented {

  private final Instrumentation mInstrumentation;

  public InstrumentedImageView(final Context context) {
    super(context);
    mInstrumentation = new Instrumentation(this);
  }

  @Override
  public void initInstrumentation(final String tag, PerfListener perfListener) {
    mInstrumentation.init(tag, perfListener);
    // we don't have a better estimate on when to call onStart, so do it here.
    mInstrumentation.onStart();
  }

  @Override
  public void onDraw(final Canvas canvas) {
    super.onDraw(canvas);
    mInstrumentation.onDraw(canvas);
  }

  @Override
  public void setImageDrawable(final Drawable drawable) {
    Preconditions.checkNotNull(drawable);
    if (drawable == Drawables.sPlaceholderDrawable) {
      // ignore
    } else if (drawable == Drawables.sErrorDrawable) {
      mInstrumentation.onFailure();
    } else {
      mInstrumentation.onSuccess();
    }
    super.setImageDrawable(drawable);
  }

  /**
   * Throws UnsupportedOperationException
   */
  @Override
  public void setImageResource(int resourceId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Throws UnsupportedOperationException
   */
  @Override
  public void setImageURI(Uri uri) {
    throw new UnsupportedOperationException();
  }
}
