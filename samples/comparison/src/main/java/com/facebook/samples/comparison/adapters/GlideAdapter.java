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

package com.facebook.samples.comparison.adapters;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/** Populate the list view with images using the Glide library. */
public class GlideAdapter extends ImageListAdapter<InstrumentedImageView> {

  private final Context mContext;

  public GlideAdapter(Context context, int resourceId, PerfListener perfListener) {
    super(context, resourceId, perfListener);
    mContext = context;
  }

  @Override
  protected Class<InstrumentedImageView> getViewClass() {
    return InstrumentedImageView.class;
  }

  @Override
  protected InstrumentedImageView createView() {
    return new InstrumentedImageView(getContext());
  }

  @Override
  protected void bind(InstrumentedImageView view, String uri) {
    Glide.with(mContext)
        .load(uri)
        .placeholder(Drawables.sPlaceholderDrawable)
        .error(Drawables.sErrorDrawable)
        .crossFade()
        .into(view);
  }

  @Override
  public void shutDown() {
    super.clear();
    Glide.get(mContext).clearMemory();
  }
}
