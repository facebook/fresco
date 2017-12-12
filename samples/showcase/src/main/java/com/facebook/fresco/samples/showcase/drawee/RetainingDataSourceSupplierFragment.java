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
package com.facebook.fresco.samples.showcase.drawee;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.RetainingDataSourceSupplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.List;

public class RetainingDataSourceSupplierFragment extends BaseShowcaseFragment {

  private static final List<String> SAMPLE_URIS =
      ImageUriProvider.getSampleUris(ImageUriProvider.ImageSize.M);

  private int mUriIndex = 0;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_retaining_supplier, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final SimpleDraweeView simpleDraweeView = view.findViewById(R.id.drawee_view);
    final RetainingDataSourceSupplier<CloseableReference<CloseableImage>> retainingSupplier =
        new RetainingDataSourceSupplier<>();
    simpleDraweeView.setController(
        Fresco.newDraweeControllerBuilder().setDataSourceSupplier(retainingSupplier).build());
    replaceImage(retainingSupplier);
    simpleDraweeView.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            replaceImage(retainingSupplier);
          }
        });
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_retaining_supplier_title;
  }

  private void replaceImage(
      RetainingDataSourceSupplier<CloseableReference<CloseableImage>> retainingSupplier) {

    retainingSupplier.replaceSupplier(
        Fresco.getImagePipeline()
            .getDataSourceSupplier(
                ImageRequest.fromUri(getNextUriString()),
                null,
                ImageRequest.RequestLevel.FULL_FETCH));
  }

  private synchronized String getNextUriString() {
    int previousIndex = mUriIndex;
    mUriIndex = (mUriIndex + 1) % SAMPLE_URIS.size();
    return SAMPLE_URIS.get(previousIndex);
  }
}
