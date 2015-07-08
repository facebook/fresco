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
import android.net.Uri;

import com.facebook.drawee.backends.volley.VolleyDraweeControllerBuilderSupplier;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.configs.volley.SampleVolleyFactory;
import com.facebook.samples.comparison.instrumentation.InstrumentedDraweeView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/** Populate the list view with images using Drawee backed by Volley. */
public class VolleyDraweeAdapter extends ImageListAdapter<InstrumentedDraweeView> {

  private VolleyDraweeControllerBuilderSupplier mVolleyDraweeControllerBuilderSupplier;

  public VolleyDraweeAdapter(Context context, int resourceId, PerfListener perfListener) {
    super(context, resourceId, perfListener);
    mVolleyDraweeControllerBuilderSupplier = new VolleyDraweeControllerBuilderSupplier(
        context,
        SampleVolleyFactory.getImageLoader(context));
    InstrumentedDraweeView.initialize(mVolleyDraweeControllerBuilderSupplier);
  }

  @Override
  protected Class<InstrumentedDraweeView> getViewClass() {
    return InstrumentedDraweeView.class;
  }

  protected InstrumentedDraweeView createView() {
    GenericDraweeHierarchy gdh = new GenericDraweeHierarchyBuilder(getContext().getResources())
        .setPlaceholderImage(Drawables.sPlaceholderDrawable)
        .setFailureImage(Drawables.sErrorDrawable)
        .build();
    InstrumentedDraweeView view = new InstrumentedDraweeView(getContext());
    view.setHierarchy(gdh);
    return view;
  }

  protected void bind(InstrumentedDraweeView view, String uri) {
    view.setImageURI(Uri.parse(uri));
  }

  @Override
  public void shutDown() {
    super.clear();
    InstrumentedDraweeView.shutDown();
    SampleVolleyFactory.getMemoryCache().clear();
  }
}
