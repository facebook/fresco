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

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;

/**
 * A {@link Fragment} that illustrates the different drawables one can set in a hierarchy.
 */
public class DraweeHierarchyFragment extends BaseShowcaseFragment {

  public DraweeHierarchyFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_hierarchy, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final ImageUriProvider imageUriProvider = ImageUriProvider.getInstance(getContext());
    final Uri uriSuccess = imageUriProvider.createSampleUri(
        ImageUriProvider.ImageSize.XL,
        ImageUriProvider.UriModification.CACHE_BREAKER);
    final Uri uriFailure = imageUriProvider.createNonExistingUri();

    final SimpleDraweeView draweeView = (SimpleDraweeView) view.findViewById(R.id.drawee);

    //noinspection deprecation
    final Drawable failureDrawable = getResources().getDrawable(R.drawable.ic_error_black_96dp);
    DrawableCompat.setTint(failureDrawable, Color.RED);

    final ProgressBarDrawable progressBarDrawable = new ProgressBarDrawable();
    progressBarDrawable.setColor(getResources().getColor(R.color.accent));
    progressBarDrawable.setBackgroundColor(getResources().getColor(R.color.primary));
    progressBarDrawable
        .setRadius(getResources().getDimensionPixelSize(R.dimen.drawee_hierarchy_progress_radius));

    draweeView.getHierarchy().setProgressBarImage(progressBarDrawable);
    draweeView.getHierarchy().setFailureImage(failureDrawable, ScaleType.CENTER_INSIDE);

    view.findViewById(R.id.load_success).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        draweeView.setImageURI(uriSuccess);
      }
    });

    view.findViewById(R.id.load_fail).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        draweeView.setImageURI(uriFailure);
      }
    });

    view.findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        draweeView.setController(null);
        Fresco.getImagePipeline().evictFromCache(uriSuccess);
      }
    });
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_hierarchy_title;
  }
}
