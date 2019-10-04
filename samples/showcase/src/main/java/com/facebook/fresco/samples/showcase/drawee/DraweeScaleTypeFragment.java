/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee;

import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.common.SimpleScaleTypeAdapter;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;

/** Simple drawee fragment that illustrates different scale types */
public class DraweeScaleTypeFragment extends BaseShowcaseFragment {

  private SimpleDraweeView mDraweeTop1;
  private SimpleDraweeView mDraweeTop2;
  private SimpleDraweeView mDraweeMain;
  private Spinner mSpinner;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_drawee_scale_type, container, false);

    final ImageUriProvider imageUriProvider = sampleUris();
    final Uri uri1 =
        imageUriProvider.createSampleUri(
            ImageUriProvider.ImageSize.M, ImageUriProvider.Orientation.LANDSCAPE);
    final Uri uri2 =
        imageUriProvider.createSampleUri(
            ImageUriProvider.ImageSize.M, ImageUriProvider.Orientation.PORTRAIT);

    mDraweeTop1 = (SimpleDraweeView) view.findViewById(R.id.drawee_view_top_1);
    mDraweeTop2 = (SimpleDraweeView) view.findViewById(R.id.drawee_view_top_2);
    mDraweeMain = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    mSpinner = (Spinner) view.findViewById(R.id.spinner);

    mDraweeTop1.setImageURI(uri1);
    mDraweeTop1.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            changeMainDraweeUri(uri1);
          }
        });

    mDraweeTop2.setImageURI(uri2);
    mDraweeTop2.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            changeMainDraweeUri(uri2);
          }
        });

    changeMainDraweeUri(uri1);

    final SimpleScaleTypeAdapter adapter = SimpleScaleTypeAdapter.createForAllScaleTypes();
    mSpinner.setAdapter(adapter);
    mSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final SimpleScaleTypeAdapter.Entry spinnerEntry =
                (SimpleScaleTypeAdapter.Entry) adapter.getItem(position);
            changeMainDraweeScaleType(spinnerEntry.scaleType, spinnerEntry.focusPoint);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    mSpinner.setSelection(0);

    return view;
  }

  private void changeMainDraweeUri(Uri uri) {
    mDraweeMain.setImageURI(uri);
  }

  private void changeMainDraweeScaleType(ScaleType scaleType, @Nullable PointF focusPoint) {
    final GenericDraweeHierarchy hierarchy = mDraweeMain.getHierarchy();
    hierarchy.setActualImageScaleType(scaleType);
    hierarchy.setActualImageFocusPoint(focusPoint != null ? focusPoint : new PointF(0.5f, 0.5f));
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_scale_type_title;
  }
}
