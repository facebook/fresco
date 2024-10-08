/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.common.SimpleScaleTypeAdapter;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider.ImageSize;
import com.facebook.fresco.vito.options.BorderOptions;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.fresco.vito.view.VitoView;

/** A {@link Fragment} that illustrates using rounded corners with Fresco. */
public class VitoRoundedCornersFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "VitoRoundedCornersFragment";

  private static final ImageOptions BASE_IMAGE_OPTIONS =
      ImageOptions.create().scale(ScaleType.CENTER).build();

  private int mColorPrimary;

  private ImageView mImageRound;
  private ImageOptions mImageRoundOptions = BASE_IMAGE_OPTIONS;

  private ImageView mImageRadius;
  private ImageOptions mImageRadiusOptions = BASE_IMAGE_OPTIONS;
  private ImageView mImageSome;
  private ImageOptions mImageSomeOptions = BASE_IMAGE_OPTIONS;
  private ImageView mImageFancy;
  private ImageOptions mImageFancyOptions = BASE_IMAGE_OPTIONS;

  private CheckBox mShowBordersCheck;
  private CheckBox mScaleInsideBordersCheck;
  private ImageSource mImageSource = ImageSourceProvider.emptySource();

  public VitoRoundedCornersFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_vito_rounded_corners, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    findImages(view);
    initColors();

    final Spinner scaleType = view.findViewById(R.id.scaleType);
    final SimpleScaleTypeAdapter scaleTypeAdapter = SimpleScaleTypeAdapter.createForAllScaleTypes();
    scaleType.setAdapter(scaleTypeAdapter);
    scaleType.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final SimpleScaleTypeAdapter.Entry spinnerEntry =
                (SimpleScaleTypeAdapter.Entry) scaleTypeAdapter.getItem(position);
            changeImageScaleType(spinnerEntry.scaleType, spinnerEntry.focusPoint);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    mShowBordersCheck = view.findViewById(R.id.borders);
    mShowBordersCheck.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateRounding();
          }
        });

    mScaleInsideBordersCheck = view.findViewById(R.id.scaleInside);
    mScaleInsideBordersCheck.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateRounding();
          }
        });

    final ImageUriProvider imageUriProvider = sampleUris();
    mImageSource = ImageSourceProvider.forUri(imageUriProvider.createSampleUri(ImageSize.L));

    int radius =
        (int)
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

    mImageRoundOptions = mImageRoundOptions.extend().round(RoundingOptions.asCircle()).build();
    mImageRadiusOptions =
        mImageRadiusOptions.extend().round(RoundingOptions.forCornerRadiusPx(radius)).build();
    mImageSomeOptions =
        mImageSomeOptions
            .extend()
            .round(RoundingOptions.forCornerRadii(radius, 0, radius, 0))
            .build();
    mImageFancyOptions =
        mImageFancyOptions
            .extend()
            .round(
                RoundingOptions.forCornerRadii(
                    getResources()
                        .getDimensionPixelSize(R.dimen.image_rounded_corners_fancy_top_left),
                    getResources()
                        .getDimensionPixelSize(R.dimen.image_rounded_corners_fancy_top_right),
                    getResources()
                        .getDimensionPixelSize(R.dimen.image_rounded_corners_fancy_bottom_right),
                    getResources()
                        .getDimensionPixelSize(R.dimen.image_rounded_corners_fancy_bottom_left)))
            .build();

    loadImages();
  }

  private void findImages(View view) {
    mImageRound = view.findViewById(R.id.image_round);
    mImageRadius = view.findViewById(R.id.image_radius);
    mImageSome = view.findViewById(R.id.image_some);
    mImageFancy = view.findViewById(R.id.image_fancy);
  }

  private void loadImages() {
    VitoView.show(mImageSource, mImageRoundOptions, CALLER_CONTEXT, mImageRound);
    VitoView.show(mImageSource, mImageRadiusOptions, CALLER_CONTEXT, mImageRadius);
    VitoView.show(mImageSource, mImageSomeOptions, CALLER_CONTEXT, mImageSome);
    VitoView.show(mImageSource, mImageFancyOptions, CALLER_CONTEXT, mImageFancy);
  }

  @SuppressWarnings("ResourceType")
  private void initColors() {
    final TypedArray attrs =
        getActivity()
            .getTheme()
            .obtainStyledAttributes(
                R.style.AppTheme,
                new int[] {
                  androidx.appcompat.R.attr.colorPrimary, android.R.attr.windowBackground
                });
    try {
      mColorPrimary = attrs.getColor(0, Color.BLACK);
    } finally {
      attrs.recycle();
    }
  }

  private void changeImageScaleType(ScaleType scaleType, @Nullable PointF focusPoint) {
    focusPoint = focusPoint != null ? focusPoint : new PointF(0.5f, 0.5f);

    mImageRoundOptions =
        mImageRoundOptions.extend().scale(scaleType).focusPoint(focusPoint).build();
    mImageRadiusOptions =
        mImageRadiusOptions.extend().scale(scaleType).focusPoint(focusPoint).build();
    mImageSomeOptions = mImageSomeOptions.extend().scale(scaleType).focusPoint(focusPoint).build();
    mImageFancyOptions =
        mImageFancyOptions.extend().scale(scaleType).focusPoint(focusPoint).build();

    loadImages();
  }

  private void updateRounding() {
    BorderOptions borderOptions = null;
    if (mShowBordersCheck.isChecked()) {
      borderOptions =
          BorderOptions.create(
              mColorPrimary,
              getResources().getDimensionPixelSize(R.dimen.image_rounded_corners_border_width),
              0f,
              mScaleInsideBordersCheck.isChecked());
    }

    mImageRoundOptions = mImageRoundOptions.extend().borders(borderOptions).build();
    mImageRadiusOptions = mImageRadiusOptions.extend().borders(borderOptions).build();
    mImageSomeOptions = mImageSomeOptions.extend().borders(borderOptions).build();
    mImageFancyOptions = mImageFancyOptions.extend().borders(borderOptions).build();

    loadImages();
  }
}
