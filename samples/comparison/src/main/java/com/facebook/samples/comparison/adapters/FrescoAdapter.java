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
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.holders.FrescoHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedDraweeView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/**
 * RecyclerView Adapter for Fresco
 */
public class FrescoAdapter extends ImageListAdapter {

  public FrescoAdapter(
      Context context,
      PerfListener perfListener,
      ImagePipelineConfig imagePipelineConfig) {
    super(context, perfListener);
    Fresco.initialize(context, imagePipelineConfig);
  }

  @Override
  public FrescoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    GenericDraweeHierarchy gdh = new GenericDraweeHierarchyBuilder(getContext().getResources())
        .setPlaceholderImage(Drawables.sPlaceholderDrawable)
        .setFailureImage(Drawables.sErrorDrawable)
        .setProgressBarImage(new ProgressBarDrawable())
        .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
        .build();
    final InstrumentedDraweeView instrView = new InstrumentedDraweeView(getContext(), gdh);

    return new FrescoHolder(getContext(), parent, instrView, getPerfListener());
  }

  @Override
  public void shutDown() {
    Fresco.shutDown();
  }
}
