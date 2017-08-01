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
import com.android.volley.toolbox.ImageLoader;
import com.facebook.samples.comparison.instrumentation.InstrumentedNetworkImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/**
 * This is the Holder class for the RecycleView to use with Volley
 */
public class VolleyHolder extends BaseViewHolder<InstrumentedNetworkImageView> {

  private final ImageLoader mImageLoader;

  public VolleyHolder(
      Context context, ImageLoader imageLoader, View layoutView,
      InstrumentedNetworkImageView view, PerfListener perfListener) {
    super(context, layoutView, view, perfListener);
    mImageLoader = imageLoader;
  }

  @Override
  protected void onBind(String uri) {
    mImageView.setImageUrl(uri, mImageLoader);
  }
}
