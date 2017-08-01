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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setSharedElementEnterTransition(DraweeTransition.createTransitionSet(
          ScalingUtils.ScaleType.CENTER_CROP, ScalingUtils.ScaleType.FIT_CENTER));
      getWindow().setSharedElementReturnTransition(DraweeTransition.createTransitionSet(
          ScalingUtils.ScaleType.FIT_CENTER, ScalingUtils.ScaleType.CENTER_CROP));
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
