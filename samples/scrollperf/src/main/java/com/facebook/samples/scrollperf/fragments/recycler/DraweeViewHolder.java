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

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.conf.Const;
import com.facebook.samples.scrollperf.util.PipelineUtil;
import com.facebook.samples.scrollperf.util.SizeUtil;

/**
 * This is the ViewHolder for the RecyclerView in order to contain the DraweeView
 */
public class DraweeViewHolder extends RecyclerView.ViewHolder {

  private final View mParentView;

  private final SimpleDraweeView mDraweeView;

  private final Config mConfig;

  public DraweeViewHolder(View parentView, SimpleDraweeView simpleDraweeView, Config config) {
    super(simpleDraweeView);
    mParentView = parentView;
    mDraweeView = simpleDraweeView;
    mConfig = config;
    if (mParentView != null) {
      int size = SizeUtil.calcDesiredSize(
              mParentView.getContext(),
              mParentView.getWidth(),
              mParentView.getHeight());
      SizeUtil.updateViewLayoutParams(mDraweeView, size, (int) (size / Const.RATIO));
    }
  }

  /**
   * @param uri The Uri to show into the DraweeView for this Holder
   */
  public void bind(Uri uri) {
    ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder
        .newBuilderWithSource(uri)
        .setResizeOptions(
            new ResizeOptions(
              mDraweeView.getLayoutParams().width,
              mDraweeView.getLayoutParams().height));
    PipelineUtil.addOptionalFeatures(imageRequestBuilder, mConfig);
    // Create the Builder
    PipelineDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder()
                                                      .setImageRequest(imageRequestBuilder.build());
    if (mConfig.reuseOldController) {
      builder.setOldController(mDraweeView.getController());
    }
    mDraweeView.setController(builder.build());
  }
}
