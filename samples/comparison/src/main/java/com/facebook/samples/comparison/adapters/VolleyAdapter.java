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
import com.android.volley.toolbox.ImageLoader;
import com.facebook.samples.comparison.R;
import com.facebook.samples.comparison.configs.volley.SampleVolleyFactory;
import com.facebook.samples.comparison.holders.VolleyHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedNetworkImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/**
 * RecyclerView Adapter for Volley
 */
public class VolleyAdapter extends ImageListAdapter {

  private final ImageLoader mImageLoader;

  public VolleyAdapter(
      Context context,
      PerfListener perfListener) {
    super(context, perfListener);
    mImageLoader = SampleVolleyFactory.getImageLoader(context);
  }

  @Override
  public VolleyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    InstrumentedNetworkImageView view = new InstrumentedNetworkImageView(getContext());
    view.setDefaultImageResId(R.color.placeholder);
    view.setErrorImageResId(R.color.error);
    return new VolleyHolder(
        getContext(), mImageLoader, parent,
        view, getPerfListener());
  }

  @Override
  public void shutDown() {
    super.clear();
    SampleVolleyFactory.getMemoryCache().clear();
  }
}
