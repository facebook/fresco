/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.holders;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.samples.comparison.MainActivity;
import com.facebook.samples.comparison.instrumentation.Instrumented;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/** The base ViewHolder with instrumentation */
public abstract class BaseViewHolder<V extends View & Instrumented>
    extends RecyclerView.ViewHolder {

  private final PerfListener mPerfListener;
  private final View mParentView;
  protected final V mImageView;
  private Context mContext;

  public BaseViewHolder(Context context, View parentView, V imageView, PerfListener perfListener) {
    super(imageView);
    this.mContext = context;
    this.mPerfListener = perfListener;
    this.mParentView = parentView;
    this.mImageView = imageView;
    if (mParentView != null) {
      int size = calcDesiredSize(mParentView.getWidth(), mParentView.getHeight());
      updateViewLayoutParams(mImageView, size, size);
    }
  }

  public void bind(String model) {
    mImageView.initInstrumentation(model.toString(), mPerfListener);
    onBind(model);
  }

  /** Load an image of the specified uri into the view, asynchronously. */
  protected abstract void onBind(String uri);

  protected Context getContext() {
    return mContext;
  }

  private void updateViewLayoutParams(View view, int width, int height) {
    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
    if (layoutParams == null || layoutParams.height != width || layoutParams.width != height) {
      layoutParams = new AbsListView.LayoutParams(width, height);
      view.setLayoutParams(layoutParams);
    }
  }

  private int calcDesiredSize(int parentWidth, int parentHeight) {
    return MainActivity.calcDesiredSize(mContext, parentWidth, parentHeight);
  }
}
