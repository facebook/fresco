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
package com.facebook.fresco.samples.showcase.drawee.transition;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;

/**
 * Simple drawee fragment that just displays an image.
 */
public class DraweeTransitionFragment extends BaseShowcaseFragment {

  public static final PointF FOCUS_POINT = new PointF(1, 0.5f);

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_transition, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final ImageUriProvider imageUriProvider = ImageUriProvider.getInstance(getContext());
    final Uri imageUri = imageUriProvider.createSampleUri(ImageUriProvider.ImageSize.M);

    final SimpleDraweeView simpleDraweeView =
        (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    // You have to enable legacy visibility handling for the start view in order for this to work
    simpleDraweeView.setLegacyVisibilityHandlingEnabled(true);
    simpleDraweeView.setImageURI(imageUri);
    simpleDraweeView.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.FOCUS_CROP);
    simpleDraweeView.getHierarchy().setActualImageFocusPoint(FOCUS_POINT);
    simpleDraweeView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startTransition(v, imageUri);
      }
    });
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_transition_title;
  }

  public void startTransition(View startView, Uri uri) {
    Intent intent = ImageDetailsActivity.getStartIntent(getContext(), uri);
    final String transitionName = getString(R.string.transition_name);
    final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
        getActivity(),
        startView,
        transitionName);
    startActivity(intent, options.toBundle());
  }
}
