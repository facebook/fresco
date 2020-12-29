/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee.transition;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;

/** Simple drawee fragment that just displays an image. */
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
    final Uri imageUri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M);

    final SimpleDraweeView simpleDraweeView =
        (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    // You have to enable legacy visibility handling for the start view in order for this to work
    simpleDraweeView.setLegacyVisibilityHandlingEnabled(true);
    simpleDraweeView.setImageURI(imageUri);
    simpleDraweeView.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.FOCUS_CROP);
    simpleDraweeView.getHierarchy().setActualImageFocusPoint(FOCUS_POINT);
    simpleDraweeView.setOnClickListener(
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
