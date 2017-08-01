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
package com.facebook.samples.scrollperf.fragments.recycler;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.data.SimpleAdapter;
import com.facebook.samples.scrollperf.instrumentation.InstrumentedDraweeView;
import com.facebook.samples.scrollperf.instrumentation.PerfListener;
import com.facebook.samples.scrollperf.util.DraweeUtil;

/**
 * The RecyclerView.Adapter for the DraweeView
 */
public class DraweeViewAdapter extends RecyclerView.Adapter<DraweeViewHolder> {

  private final SimpleAdapter<Uri> mSimpleAdapter;

  private final Context mContext;

  private final Config mConfig;

  private final int mPaddingPx;

  private final PerfListener mPerfListener;

  public DraweeViewAdapter(
      Context context,
      SimpleAdapter<Uri> simpleAdapter,
      Config config,
      PerfListener perfListener) {
    this.mContext = context;
    this.mSimpleAdapter = simpleAdapter;
    this.mConfig = config;
    this.mPaddingPx = context.getResources().getDimensionPixelSize(R.dimen.drawee_padding);
    mPerfListener = perfListener;
  }

  @Override
  public DraweeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    GenericDraweeHierarchy gdh = DraweeUtil.createDraweeHierarchy(mContext, mConfig);
    final InstrumentedDraweeView simpleDraweeView =
        new InstrumentedDraweeView(mContext, gdh, mConfig);
    simpleDraweeView.setPadding(mPaddingPx, mPaddingPx, mPaddingPx, mPaddingPx);
    return new DraweeViewHolder(parent, simpleDraweeView, mConfig, mPerfListener);
  }

  @Override
  public void onBindViewHolder(DraweeViewHolder holder, int position) {
    holder.bind(mSimpleAdapter.get(position));
  }

  @Override
  public int getItemCount() {
    return mSimpleAdapter.getSize();
  }
}
