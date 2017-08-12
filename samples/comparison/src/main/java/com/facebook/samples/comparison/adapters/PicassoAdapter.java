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
import android.view.ViewGroup;
import com.facebook.samples.comparison.configs.picasso.SamplePicassoFactory;
import com.facebook.samples.comparison.holders.PicassoHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.squareup.picasso.Picasso;

/**
 * RecyclerView Adapter for Picasso
 */
public class PicassoAdapter extends ImageListAdapter {

  private final Picasso mPicasso;

  public PicassoAdapter(
      Context context,
      PerfListener perfListener) {
    super(context, perfListener);
    mPicasso = SamplePicassoFactory.getPicasso(context);
  }

  @Override
  public PicassoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final InstrumentedImageView instrImageView = new InstrumentedImageView(getContext());
    return new PicassoHolder(getContext(), mPicasso, parent, instrImageView, getPerfListener());
  }

  @Override
  public void shutDown() {
    for (int i = 0; i < getItemCount(); i++) {
      String uri = getItem(i);
      mPicasso.invalidate(uri);
    }
    super.clear();
  }
}
