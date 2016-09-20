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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.data.SimpleAdapter;
import com.facebook.samples.scrollperf.util.DraweeUtil;
import com.facebook.samples.scrollperf.util.PipelineUtil;
import com.facebook.samples.scrollperf.util.SizeUtil;

/**
 * This is the implementation of the Adapter for the ListView
 */
public class DraweeViewListAdapter extends BaseAdapter {

  private final SimpleAdapter<Uri> mSimpleAdapter;

  private final Config mConfig;

  public DraweeViewListAdapter(SimpleAdapter<Uri> simpleAdapter, Config config) {
    this.mSimpleAdapter = simpleAdapter;
    this.mConfig = config;
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
      GenericDraweeHierarchy gdh = DraweeUtil.createDraweeHierarchy(context, mConfig);
      draweeView = new SimpleDraweeView(context, gdh);
      SizeUtil.setConfiguredSize(parent, draweeView, mConfig);
    } else {
      draweeView = (SimpleDraweeView) convertView;
    }
    ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder
            .newBuilderWithSource(getItem(position))
            .setResizeOptions(
                    new ResizeOptions(
                            draweeView.getLayoutParams().width,
                            draweeView.getLayoutParams().height));
    PipelineUtil.addOptionalFeatures(imageRequestBuilder, mConfig);
    // Create the Builder
    PipelineDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder()
            .setImageRequest(imageRequestBuilder.build());
    if (mConfig.reuseOldController) {
      builder.setOldController(draweeView.getController());
    }
    draweeView.setController(builder.build());
    return draweeView;
  }
}
