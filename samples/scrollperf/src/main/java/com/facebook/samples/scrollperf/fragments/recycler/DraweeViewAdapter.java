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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.data.SimpleAdapter;

/**
 * The RecyclerView.Adapter for the DraweeView
 */
public class DraweeViewAdapter extends RecyclerView.Adapter<DraweeViewHolder> {

  private static final Drawable PLACEHOLDER = new ColorDrawable(Color.GRAY);

  private static final Drawable FAILURE = new ColorDrawable(Color.RED);

  private final SimpleAdapter<Uri> mSimpleAdapter;

  private final Context mContext;

  private final Config mConfig;

  public DraweeViewAdapter(Context context, SimpleAdapter<Uri> simpleAdapter, Config config) {
    this.mContext = context;
    this.mSimpleAdapter = simpleAdapter;
    this.mConfig = config;
  }

  @Override
  public DraweeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    GenericDraweeHierarchy gdh = new GenericDraweeHierarchyBuilder(mContext.getResources())
                                         .setPlaceholderImage(PLACEHOLDER)
                                         .setFailureImage(FAILURE)
                                         .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                                         .build();
    final SimpleDraweeView simpleDraweeView = new SimpleDraweeView(mContext, gdh);
    return new DraweeViewHolder(parent, simpleDraweeView, mConfig);
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
