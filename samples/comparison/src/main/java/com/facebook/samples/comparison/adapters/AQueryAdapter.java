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
import com.androidquery.AQuery;
import com.facebook.samples.comparison.holders.AQueryHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/**
 * RecyclerView Adapter for Android Query
 */
public class AQueryAdapter extends ImageListAdapter {

  private AQuery mAQuery;

  public AQueryAdapter(
      Context context,
      PerfListener perfListener) {
    super(context, perfListener);
    mAQuery = new AQuery(context);
  }

  @Override
  public AQueryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final InstrumentedImageView instrImageView = new InstrumentedImageView(getContext());
    return new AQueryHolder(getContext(), mAQuery, parent, instrImageView, getPerfListener());
  }

  @Override
  public void shutDown() {
    for (int i = 0; i < getItemCount(); i++) {
      String uri = getItem(i);
      mAQuery.invalidate(uri);
    }
    super.clear();
  }
}
