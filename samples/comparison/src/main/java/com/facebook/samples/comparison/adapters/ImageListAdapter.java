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
import android.support.v7.widget.RecyclerView;
import com.facebook.samples.comparison.holders.BaseViewHolder;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for RecyclerView Adapters
 */
public abstract class ImageListAdapter extends RecyclerView.Adapter<BaseViewHolder<?>> {

  private final PerfListener mPerfListener;

  private final Context mContext;

  private List<String> mModel;

  public ImageListAdapter(final Context context, final PerfListener perfListener) {
    this.mContext = context;
    this.mPerfListener = perfListener;
    this.mModel = new LinkedList<String>();
  }

  public void addUrl(final String url) {
    mModel.add(url);
  }

  protected PerfListener getPerfListener() {
    return mPerfListener;
  }

  protected String getItem(final int position) {
    return mModel.get(position);
  }

  @Override
  public int getItemCount() {
    return mModel.size();
  }

  protected Context getContext() {
    return mContext;
  }

  public void clear() {
    mModel.clear();
  }

  @Override
  public void onBindViewHolder(BaseViewHolder<?> holder, int position) {
    holder.bind(getItem(position));
  }

  /**
   * Releases any resources and tears down the adapter.
   */
  public abstract void shutDown();
}
