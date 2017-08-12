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

package com.facebook.samples.comparison.holders;

import android.content.Context;
import android.view.View;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.squareup.picasso.Picasso;

/**
 * This is the Holder class for the RecycleView to use with Picasso
 */
public class PicassoHolder extends BaseViewHolder<InstrumentedImageView> {

  private final Picasso mPicasso;

  public PicassoHolder(
      Context context, Picasso picasso, View parent,
      InstrumentedImageView view, PerfListener perfListener) {
    super(context, parent, view, perfListener);
    mPicasso = picasso;
  }

  @Override
  protected void onBind(String uri) {
    mPicasso.load(uri)
        .placeholder(Drawables.sPlaceholderDrawable)
        .error(Drawables.sErrorDrawable)
        .fit()
        .into(mImageView);
  }
}
