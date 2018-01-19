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

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.DraweeTransition;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.R;

/**
 * Image details activity
 */
public class ImageDetailsActivity extends AppCompatActivity {

  public static Intent getStartIntent(Context context, Uri imageUri) {
    Intent intent = new Intent(context, ImageDetailsActivity.class);
    intent.setData(imageUri);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_drawee_transition_detail);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    SimpleDraweeView simpleDraweeView = (SimpleDraweeView) findViewById(R.id.image);
    simpleDraweeView.setImageURI(getIntent().getData());

    ScalingUtils.ScaleType toScaleType = ScalingUtils.ScaleType.FOCUS_CROP;
    PointF toFocusPoint = new PointF(0.5f, 0);

    simpleDraweeView.getHierarchy().setActualImageScaleType(toScaleType);
    simpleDraweeView.getHierarchy().setActualImageFocusPoint(toFocusPoint);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      ScalingUtils.ScaleType fromScaleType = ScalingUtils.ScaleType.FOCUS_CROP;
      PointF fromFocusPoint = DraweeTransitionFragment.FOCUS_POINT;

      getWindow()
          .setSharedElementEnterTransition(
              DraweeTransition.createTransitionSet(
                  fromScaleType, toScaleType, fromFocusPoint, toFocusPoint));
      getWindow()
          .setSharedElementReturnTransition(
              DraweeTransition.createTransitionSet(
                  toScaleType, fromScaleType, toFocusPoint, fromFocusPoint));
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // Respond to the action bar's Up/Home button
      case android.R.id.home:
        supportFinishAfterTransition();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
