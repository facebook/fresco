/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito.transition;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.vito.view.transition.VitoTransition;

/** Image details activity */
public class ImageDetailsActivity extends AppCompatActivity {

  private static final String CALLER_CONTEXT = "VitoTransitionFragment";

  public static Intent getStartIntent(Context context, Uri imageUri) {
    Intent intent = new Intent(context, ImageDetailsActivity.class);
    intent.setData(imageUri);
    return intent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_vito_transition_detail);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    ImageView imageView = findViewById(R.id.image);
    imageView.setImageURI(getIntent().getData());

    ScalingUtils.ScaleType toScaleType = ScalingUtils.ScaleType.FOCUS_CROP;
    PointF toFocusPoint = new PointF(0.5f, 0);

    ImageOptions imageOptions =
        ImageOptions.create().scale(toScaleType).focusPoint(toFocusPoint).build();
    VitoView.show(getIntent().getData(), imageOptions, CALLER_CONTEXT, imageView);

    ScalingUtils.ScaleType fromScaleType = ScalingUtils.ScaleType.FOCUS_CROP;
    PointF fromFocusPoint = VitoTransitionFragment.FOCUS_POINT;

    getWindow()
        .setSharedElementEnterTransition(
            VitoTransition.createTransitionSet(
                CALLER_CONTEXT, fromScaleType, toScaleType, fromFocusPoint, toFocusPoint));
    getWindow()
        .setSharedElementReturnTransition(
            VitoTransition.createTransitionSet(
                CALLER_CONTEXT, toScaleType, fromScaleType, toFocusPoint, fromFocusPoint));
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
