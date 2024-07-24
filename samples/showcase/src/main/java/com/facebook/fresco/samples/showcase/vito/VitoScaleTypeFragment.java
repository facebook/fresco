/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.annotation.Nullable;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.common.SimpleScaleTypeAdapter;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;

/** Simple Vito fragment that illustrates different scale types */
public class VitoScaleTypeFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "VitoScaleTypeFragment";
  private static final ImageOptions TOP_IMAGE_OPTIONS =
      ImageOptions.create().scale(ScaleType.CENTER_INSIDE).build();

  private ImageView mVitoTop1;
  private ImageView mVitoTop2;
  private ImageView mVitoMain;
  private Spinner mSpinner;

  private Uri mCurrentUri;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_vito_scale_type, container, false);

    final ImageUriProvider imageUriProvider = sampleUris();
    final Uri uri1 =
        imageUriProvider.createSampleUri(
            ImageUriProvider.ImageSize.M, ImageUriProvider.Orientation.LANDSCAPE);
    final Uri uri2 =
        imageUriProvider.createSampleUri(
            ImageUriProvider.ImageSize.M, ImageUriProvider.Orientation.PORTRAIT);

    mVitoTop1 = view.findViewById(R.id.image_top_1);
    mVitoTop2 = view.findViewById(R.id.image_top_2);
    mVitoMain = view.findViewById(R.id.image);
    mSpinner = view.findViewById(R.id.spinner);

    VitoView.show(uri1, TOP_IMAGE_OPTIONS, CALLER_CONTEXT, mVitoTop1);
    mVitoTop1.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            changeMainVitoUri(uri1);
          }
        });

    VitoView.show(uri2, TOP_IMAGE_OPTIONS, CALLER_CONTEXT, mVitoTop2);
    mVitoTop2.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            changeMainVitoUri(uri2);
          }
        });

    changeMainVitoUri(uri1);

    final SimpleScaleTypeAdapter adapter = SimpleScaleTypeAdapter.createForAllScaleTypes();
    mSpinner.setAdapter(adapter);
    mSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final SimpleScaleTypeAdapter.Entry spinnerEntry =
                (SimpleScaleTypeAdapter.Entry) adapter.getItem(position);
            changeMainVitoScaleType(spinnerEntry.scaleType, spinnerEntry.focusPoint);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    mSpinner.setSelection(0);

    return view;
  }

  private void changeMainVitoUri(Uri uri) {
    mCurrentUri = uri;
    VitoView.show(uri, CALLER_CONTEXT, mVitoMain);
  }

  private void changeMainVitoScaleType(ScaleType scaleType, @Nullable PointF focusPoint) {
    VitoView.show(
        mCurrentUri,
        ImageOptions.create().scale(scaleType).focusPoint(focusPoint).build(),
        CALLER_CONTEXT,
        mVitoMain);
  }
}
