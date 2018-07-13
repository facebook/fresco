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
package com.facebook.fresco.samples.showcase.imagepipeline;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

public class PartialRequestFragment extends BaseShowcaseFragment {

  public static final Uri URI =
      Uri.parse("http://frescolib.org/static/sample-images/animal_e_l.jpg");

  public PartialRequestFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_partial_request, container, false);
  }

  @Override
  public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    final SimpleDraweeView partialDrawee =
        (SimpleDraweeView) view.findViewById(R.id.drawee_partial_img);

    final SimpleDraweeView fullDrawee =
        (SimpleDraweeView) view.findViewById(R.id.drawee_full_img);

    final Button clearCacheButton = (Button) view.findViewById(R.id.clear_cache);
    clearCacheButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        partialDrawee.setController(null);
        fullDrawee.setController(null);
        Fresco.getImagePipeline().clearDiskCaches();
        Fresco.getImagePipeline().clearMemoryCaches();
      }
    });

    final Button prefetchButton = (Button) view.findViewById(R.id.prefetch_now);
    prefetchButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        loadImageIntoDrawee(partialDrawee, BytesRange.toMax(30000));
      }
    });

    Button loadFull = (Button) view.findViewById(R.id.load_full);
    loadFull.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        loadImageIntoDrawee(fullDrawee, null);
      }
    });
  }

  private void loadImageIntoDrawee(SimpleDraweeView draweeView, @Nullable BytesRange bytesRange) {
    final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(URI)
        .setBytesRange(bytesRange)
        .build();

    final DraweeController draweeController = Fresco.newDraweeControllerBuilder()
        .setOldController(draweeView.getController())
        .setImageRequest(imageRequest)
        .build();

    draweeView.setController(draweeController);
  }

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_partial_request_title;
  }
}
