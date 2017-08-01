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

package com.facebook.samples.comparison.holders;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.samples.comparison.instrumentation.InstrumentedDraweeView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/**
 * This is the Holder class for the RecycleView to use with Fresco
 */
public class FrescoHolder extends BaseViewHolder<InstrumentedDraweeView> {

  public FrescoHolder(
      Context context, View parentView,
      InstrumentedDraweeView intrumentedDraweeView, PerfListener perfListener) {
    super(context, parentView, intrumentedDraweeView, perfListener);
  }

  @Override
  protected void onBind(String uriString) {
    Uri uri = Uri.parse(uriString);
    ImageRequestBuilder imageRequestBuilder =
        ImageRequestBuilder.newBuilderWithSource(uri);
    if (UriUtil.isNetworkUri(uri)) {
      imageRequestBuilder.setProgressiveRenderingEnabled(true);
    } else {
      imageRequestBuilder.setResizeOptions(new ResizeOptions(
          mImageView.getLayoutParams().width,
          mImageView.getLayoutParams().height));
    }
    DraweeController draweeController = Fresco.newDraweeControllerBuilder()
        .setImageRequest(imageRequestBuilder.build())
        .setOldController(mImageView.getController())
        .setControllerListener(mImageView.getListener())
        .setAutoPlayAnimations(true)
        .build();
    mImageView.setController(draweeController);
  }

}
