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
import com.bumptech.glide.Glide;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/**
 * This is the Holder class for the RecycleView to use with Glide
 */
public class GlideHolder extends BaseViewHolder<InstrumentedImageView> {

  public GlideHolder(
      Context context, View layoutView,
      InstrumentedImageView instrumentedImageView, PerfListener perfListener) {
    super(context, layoutView, instrumentedImageView, perfListener);
  }

  @Override
  protected void onBind(String uri) {
    Glide.with(mImageView.getContext())
        .load(uri)
        .placeholder(Drawables.sPlaceholderDrawable)
        .error(Drawables.sErrorDrawable)
        .crossFade()
        .into(mImageView);
  }

}
