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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.samples.scrollperf.data.SimpleAdapter;

/**
 * This is the implementation of the Adapter for the ListView
 */
public class DraweeViewListAdapter extends BaseAdapter {

  private static final Drawable PLACEHOLDER = new ColorDrawable(Color.GRAY);

  private static final Drawable FAILURE = new ColorDrawable(Color.RED);

  private static final double RATIO = 4.0 / 3.0;

  private final SimpleAdapter<Uri> mSimpleAdapter;

  public DraweeViewListAdapter(SimpleAdapter<Uri> simpleAdapter) {
    this.mSimpleAdapter = simpleAdapter;
  }

  @Override
  public int getCount() {
    return mSimpleAdapter.getSize();
  }

  @Override
  public Uri getItem(int position) {
    return mSimpleAdapter.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    SimpleDraweeView draweeView;
    if (convertView == null) {
      final Context context = parent.getContext();
      GenericDraweeHierarchy gdh = new GenericDraweeHierarchyBuilder(context.getResources())
              .setPlaceholderImage(PLACEHOLDER)
              .setFailureImage(FAILURE)
              .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
              .build();
      draweeView = new SimpleDraweeView(context, gdh);
      int size = calcDesiredSize(
              parent.getContext(),
              parent.getWidth(),
              parent.getHeight());
      updateViewLayoutParams(draweeView, size, (int) (size / RATIO));
    } else {
      draweeView = (SimpleDraweeView) convertView;
    }
    ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder
            .newBuilderWithSource(getItem(position))
            .setResizeOptions(
                    new ResizeOptions(
                            draweeView.getLayoutParams().width,
                            draweeView.getLayoutParams().height));
    // Create the Builder
    PipelineDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder()
            .setImageRequest(imageRequestBuilder.build());
    draweeView.setController(builder.build());
    return draweeView;
  }

  private void updateViewLayoutParams(View view, int width, int height) {
    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
    if (layoutParams == null || layoutParams.height != width || layoutParams.width != height) {
      layoutParams = new AbsListView.LayoutParams(width, height);
      view.setLayoutParams(layoutParams);
    }
  }

  private static int calcDesiredSize(Context context, int parentWidth, int parentHeight) {
    int orientation = context.getResources().getConfiguration().orientation;
    int desiredSize = (orientation == Configuration.ORIENTATION_LANDSCAPE) ?
            parentWidth  : parentHeight ;
    return Math.min(desiredSize, parentWidth);
  }
}
