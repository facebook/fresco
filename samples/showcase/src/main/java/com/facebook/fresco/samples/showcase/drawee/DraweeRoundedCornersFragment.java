/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.facebook.common.internal.Preconditions;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.common.SimpleScaleTypeAdapter;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider.ImageSize;

/** A {@link Fragment} that illustrates using rounded corners with Fresco. */
public class DraweeRoundedCornersFragment extends BaseShowcaseFragment {

  private int mCornerBackgroundColor;
  private int mColorPrimary;

  private SimpleDraweeView mDraweeRound;
  private SimpleDraweeView mDraweeRadius;
  private SimpleDraweeView mDraweeSome;
  private SimpleDraweeView mDraweeSomeRtl;
  private SimpleDraweeView mDraweeFancy;

  private CheckBox mShowBordersCheck;
  private CheckBox mScaleInsideBordersCheck;
  private CheckBox mColorOverlayCheck;
  private CheckBox mFixRepeatedEdgesCheck;

  public DraweeRoundedCornersFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_rounded_corners, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    findDrawees(view);
    initColors();

    final ImageUriProvider imageUriProvider = sampleUris();
    mDraweeRound.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));
    mDraweeRadius.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));
    mDraweeSome.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));
    mDraweeSomeRtl.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));
    mDraweeFancy.setImageURI(imageUriProvider.createSampleUri(ImageSize.L));

    final Spinner scaleType = view.findViewById(R.id.scaleType);
    final SimpleScaleTypeAdapter scaleTypeAdapter = SimpleScaleTypeAdapter.createForAllScaleTypes();
    scaleType.setAdapter(scaleTypeAdapter);
    scaleType.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final SimpleScaleTypeAdapter.Entry spinnerEntry =
                (SimpleScaleTypeAdapter.Entry) scaleTypeAdapter.getItem(position);
            final ScaleType scaleType = spinnerEntry.scaleType;

            changeDraweeViewScaleType(mDraweeRound, scaleType, spinnerEntry.focusPoint);
            changeDraweeViewScaleType(mDraweeRadius, scaleType, spinnerEntry.focusPoint);
            changeDraweeViewScaleType(mDraweeSome, scaleType, spinnerEntry.focusPoint);
            changeDraweeViewScaleType(mDraweeSomeRtl, scaleType, spinnerEntry.focusPoint);
            changeDraweeViewScaleType(mDraweeFancy, scaleType, spinnerEntry.focusPoint);
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
    mColorOverlayCheck = view.findViewById(R.id.color_overlay);
    mColorOverlayCheck.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateRounding();
          }
        });

    mFixRepeatedEdgesCheck = view.findViewById(R.id.fix_repeated_edges);
    mFixRepeatedEdgesCheck.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateRounding();
          }
        });

    final Resources res = getResources();
    final RoundingParams fancyRoundingParams =
        RoundingParams.fromCornersRadii(
            res.getDimensionPixelSize(R.dimen.drawee_rounded_corners_fancy_top_left),
            res.getDimensionPixelSize(R.dimen.drawee_rounded_corners_fancy_top_right),
            res.getDimensionPixelSize(R.dimen.drawee_rounded_corners_fancy_bottom_right),
            res.getDimensionPixelSize(R.dimen.drawee_rounded_corners_fancy_bottom_left));
    mDraweeFancy.getHierarchy().setRoundingParams(fancyRoundingParams);
  }

  private void findDrawees(View view) {
    mDraweeRound = view.findViewById(R.id.drawee_round);
    mDraweeRadius = view.findViewById(R.id.drawee_radius);
    mDraweeSome = view.findViewById(R.id.drawee_some);
    mDraweeSomeRtl = view.findViewById(R.id.drawee_some_rtl);
    mDraweeFancy = view.findViewById(R.id.drawee_fancy);
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
      mCornerBackgroundColor = Color.BLUE;
    } finally {
      attrs.recycle();
    }
  }

  private void changeDraweeViewScaleType(
      SimpleDraweeView draweeView, ScaleType scaleType, @Nullable PointF focusPoint) {
    final GenericDraweeHierarchy hierarchy = draweeView.getHierarchy();
    hierarchy.setActualImageScaleType(scaleType);
    hierarchy.setActualImageFocusPoint(focusPoint != null ? focusPoint : new PointF(0.5f, 0.5f));

    final RoundingParams roundingParams = Preconditions.checkNotNull(hierarchy.getRoundingParams());

    hierarchy.setRoundingParams(roundingParams);
  }

  private void updateRounding() {
    boolean showBorder = mShowBordersCheck.isChecked();
    boolean scaleInsideBorder = showBorder && mScaleInsideBordersCheck.isChecked();
    setShowBorder(mDraweeRound, showBorder, scaleInsideBorder);
    setShowBorder(mDraweeRadius, showBorder, scaleInsideBorder);
    setShowBorder(mDraweeSome, showBorder, scaleInsideBorder);
    setShowBorder(mDraweeSomeRtl, showBorder, scaleInsideBorder);
    setShowBorder(mDraweeFancy, showBorder, scaleInsideBorder);
  }

  private void setShowBorder(SimpleDraweeView draweeView, boolean show, boolean scaleInside) {
    final RoundingParams roundingParams =
        Preconditions.checkNotNull(draweeView.getHierarchy().getRoundingParams());
    if (show) {
      roundingParams.setBorder(
          mColorPrimary,
          getResources().getDimensionPixelSize(R.dimen.drawee_rounded_corners_border_width));
      roundingParams.setScaleDownInsideBorders(scaleInside);
    } else {
      roundingParams.setBorder(Color.TRANSPARENT, 0);
    }
    if (mColorOverlayCheck.isChecked()) {
      roundingParams.setOverlayColor(mCornerBackgroundColor);
    } else {
      roundingParams.setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
    }
    roundingParams.setRepeatEdgePixels(!mFixRepeatedEdgesCheck.isChecked());
    draweeView.getHierarchy().setRoundingParams(roundingParams);
  }
}
