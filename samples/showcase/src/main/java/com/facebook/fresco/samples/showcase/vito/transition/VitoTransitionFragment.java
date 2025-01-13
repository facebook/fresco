/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito.transition;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.facebook.common.internal.Suppliers;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.fresco.vito.view.impl.VitoViewImpl2;

/** Simple drawee fragment that just displays an image. */
public class VitoTransitionFragment extends BaseShowcaseFragment {

  public static final PointF FOCUS_POINT = new PointF(1, 0.5f);
  private static final String CALLER_CONTEXT = "VitoTransitionFragment";

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_vito_transition, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final Uri imageUri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M);

    // In Android N, visibility handling for ImageView was changed to use onVisibilityAggregated,
    // which doesn't update the visibility of the starting Vito view correctly when the activity
    // transition is reversed.
    // Hence, we have to (globally) disable Visibility APIs for now.
    VitoViewImpl2.useVisibilityCallbacks = Suppliers.BOOLEAN_FALSE;

    final ImageView imageView = view.findViewById(R.id.image_view);
    final ImageOptions imageOptions =
        ImageOptions.create()
            .scale(ScalingUtils.ScaleType.FOCUS_CROP)
            .focusPoint(FOCUS_POINT)
            .build();
    imageView.setImageURI(imageUri);
    VitoView.show(imageUri, imageOptions, CALLER_CONTEXT, imageView);
    imageView.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            startTransition(v, imageUri);
          }
        });
  }

  public void startTransition(View startView, Uri uri) {
    Intent intent = ImageDetailsActivity.getStartIntent(getContext(), uri);
    final String transitionName = getString(R.string.transition_name);
    final ActivityOptions options =
        ActivityOptions.makeSceneTransitionAnimation(getActivity(), startView, transitionName);
    startActivity(intent, options.toBundle());
  }
}
