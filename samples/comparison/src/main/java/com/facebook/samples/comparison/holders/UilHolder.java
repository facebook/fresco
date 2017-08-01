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
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * This is the Holder class for the RecycleView to use with Universal Image Loader
 */
public class UilHolder extends BaseViewHolder<InstrumentedImageView> {

  private final ImageLoader mImageLoader;

  public UilHolder(
      Context context, ImageLoader imageLoader, View layoutView,
      InstrumentedImageView view, PerfListener perfListener) {
    super(context, layoutView, view, perfListener);
    this.mImageLoader = imageLoader;
  }

  @Override
  protected void onBind(String uri) {
    mImageLoader.displayImage(uri, mImageView);
  }
}
