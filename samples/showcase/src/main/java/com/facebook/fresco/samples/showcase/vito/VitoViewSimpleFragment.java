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
package com.facebook.fresco.samples.showcase.vito;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.fresco.vito.view.VitoView;

public class VitoViewSimpleFragment extends BaseShowcaseFragment {

  private static final ImageOptions IMAGE_OPTIONS =
      ImageOptions.create()
          .round(RoundingOptions.asCircle())
          .placeholderRes(R.drawable.logo)
          .build();

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_vito_view_simple, container, false);
  }

  @Override
  public void onViewCreated(View container, @Nullable Bundle savedInstanceState) {
    final Uri uri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M);
    final View view = container.findViewById(R.id.view);

    VitoView.show(uri, IMAGE_OPTIONS, view);
  }

  @Override
  public int getTitleId() {
    return R.string.vito_view_simple;
  }
}
