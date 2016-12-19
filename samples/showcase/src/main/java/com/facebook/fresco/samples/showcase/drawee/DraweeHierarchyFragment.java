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
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;

/**
 * A {@link Fragment} that illustrates the different drawables one can set in a hierarchy.
 */
public class DraweeHierarchyFragment extends BaseShowcaseFragment {
  private static final Uri URI_SUCCESS =
      Uri.parse("http://frescolib.org/static/sample-images/animal_a.png");
  private static final Uri URI_FAIL =
      Uri.parse("http://frescolib.org/static/sample-images/pancakes.png");

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
    final SimpleDraweeView draweeView = (SimpleDraweeView) view.findViewById(R.id.drawee);

    //noinspection deprecation
    final Drawable failureDrawable = getResources().getDrawable(R.drawable.ic_error_black_96dp);
    DrawableCompat.setTint(failureDrawable, Color.RED);

    final ProgressBarDrawable progressBarDrawable = new ProgressBarDrawable();
    progressBarDrawable.setColor(getResources().getColor(R.color.accent));
    progressBarDrawable.setBackgroundColor(getResources().getColor(R.color.primary));
    progressBarDrawable
        .setRadius(getResources().getDimensionPixelSize(R.dimen.drawee_hierarchy_progress_radius));

    draweeView.setHierarchy(
        new GenericDraweeHierarchyBuilder(getResources())
            .setProgressBarImage(progressBarDrawable)
            .setPlaceholderImage(R.mipmap.ic_launcher)
            .setFailureImage(failureDrawable)
            .build());

    view.findViewById(R.id.load_success).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        draweeView.setImageURI(URI_SUCCESS);
      }
    });

    view.findViewById(R.id.load_fail).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        draweeView.setImageURI(URI_FAIL);
      }
    });

    view.findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        draweeView.setController(null);
        Fresco.getImagePipeline().evictFromCache(URI_SUCCESS);
      }
    });
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_hierarchy_title;
  }
}
