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
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.imagepipeline.request.ImageRequest;

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

    final SimpleDraweeView draweeView = view.findViewById(R.id.drawee);
    final SwitchCompat retrySwitch = view.findViewById(R.id.retry_enabled);

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
        setUri(draweeView, uriSuccess, retrySwitch.isChecked());
      }
    });

    view.findViewById(R.id.load_fail).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        setUri(draweeView, uriFailure, retrySwitch.isChecked());
      }
    });

    view.findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        draweeView.setController(null);
        Fresco.getImagePipeline().evictFromCache(uriSuccess);
      }
    });

    final SwitchCompat roundCorners = view.findViewById(R.id.switch_rounded);
    roundCorners.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        RoundingParams roundingParams = new RoundingParams().setCornersRadius(isChecked
            ? buttonView.getResources()
                .getDimensionPixelSize(R.dimen.drawee_hierarchy_corner_radius)
            : 0);
        draweeView.getHierarchy().setRoundingParams(roundingParams);
      }
    });

    final SwitchCompat useNinePatch = view.findViewById(R.id.switch_ninepatch);
    useNinePatch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        draweeView.getHierarchy().setPlaceholderImage(
            isChecked ? R.drawable.ninepatch : R.drawable.logo,
            isChecked ? ScaleType.FIT_XY : ScaleType.CENTER_INSIDE);
      }
    });
  }

  private void setUri(SimpleDraweeView draweeView, Uri uri, boolean retryEnabled) {
    draweeView.setController(Fresco.newDraweeControllerBuilder()
        .setOldController(draweeView.getController())
        .setTapToRetryEnabled(retryEnabled)
        .setUri(uri)
        .build());
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_hierarchy_title;
  }
}
