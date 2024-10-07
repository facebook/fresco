/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.fresco.vito.view.VitoView;

/** A {@link Fragment} that illustrates the different drawables one can set for ImageOptions. */
public class ImageLayersFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImageLayersFragment";

  private ImageOptions mImageOptions =
      ImageOptions.create()
          .placeholderRes(R.drawable.logo)
          .placeholderScaleType(ScaleType.CENTER_INSIDE)
          .build();

  public ImageLayersFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    return inflater.inflate(R.layout.fragment_image_layers, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final Uri uriSuccess =
        sampleUris()
            .createSampleUri(
                ImageUriProvider.ImageSize.XL,
                ImageUriProvider.Orientation.ANY,
                ImageUriProvider.UriModification.CACHE_BREAKER);
    final Uri uriFailure = sampleUris().getNonExistingUri();

    final ImageView imageView = view.findViewById(R.id.image);

    //noinspection deprecation
    final Drawable failureDrawable = getResources().getDrawable(R.drawable.ic_error_black_96dp);
    DrawableCompat.setTint(failureDrawable, Color.RED);

    final ProgressBarDrawable progressBarDrawable = new ProgressBarDrawable();
    progressBarDrawable.setColor(getResources().getColor(R.color.accent));
    progressBarDrawable.setBackgroundColor(getResources().getColor(R.color.primary));
    progressBarDrawable.setRadius(
        getResources().getDimensionPixelSize(R.dimen.drawee_hierarchy_progress_radius));

    mImageOptions =
        mImageOptions
            .extend()
            .progress(progressBarDrawable)
            .errorDrawable(failureDrawable)
            .errorScaleType(ScaleType.CENTER_INSIDE)
            .build();

    view.findViewById(R.id.load_success)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                setUri(imageView, uriSuccess);
              }
            });

    view.findViewById(R.id.load_fail)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                setUri(imageView, uriFailure);
              }
            });

    view.findViewById(R.id.clear)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                VitoView.release(imageView);
                Fresco.getImagePipeline().evictFromCache(uriSuccess);
              }
            });

    final SwitchCompat roundCorners = view.findViewById(R.id.switch_rounded);
    roundCorners.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            RoundingOptions roundingOptions = null;
            if (isChecked) {
              roundingOptions =
                  RoundingOptions.forCornerRadiusPx(
                      buttonView
                          .getResources()
                          .getDimensionPixelSize(R.dimen.drawee_hierarchy_corner_radius));
            }
            mImageOptions = mImageOptions.extend().round(roundingOptions).build();
          }
        });

    final SwitchCompat useNinePatch = view.findViewById(R.id.switch_ninepatch);
    useNinePatch.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mImageOptions =
                mImageOptions
                    .extend()
                    .placeholderRes(isChecked ? R.drawable.ninepatch : R.drawable.logo)
                    .placeholderScaleType(isChecked ? ScaleType.FIT_XY : ScaleType.CENTER_INSIDE)
                    .build();
          }
        });
  }

  private void setUri(ImageView imageView, Uri uri) {
    VitoView.show(uri, mImageOptions, CALLER_CONTEXT, imageView);
  }
}
