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
import com.facebook.samples.comparison.configs.uil.SampleUilFactory;
import com.facebook.samples.comparison.holders.UilHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * RecyclerView Adapter for Universal ImageLoader
 */
public class UilAdapter extends ImageListAdapter {

  private final ImageLoader mImageLoader;

  public UilAdapter(
      Context context,
      PerfListener perfListener) {
    super(context, perfListener);
    mImageLoader = SampleUilFactory.getImageLoader(context);
  }

  @Override
  public UilHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final InstrumentedImageView instrumentedImageView = new InstrumentedImageView(getContext());
    return new UilHolder(
        getContext(), mImageLoader, parent,
        instrumentedImageView, getPerfListener());
  }

  @Override
  public void shutDown() {
    super.clear();
    mImageLoader.clearMemoryCache();
  }
}
