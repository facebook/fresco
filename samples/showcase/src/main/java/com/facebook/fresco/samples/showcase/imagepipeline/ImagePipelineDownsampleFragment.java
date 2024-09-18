/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imagepipeline;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.imagepipeline.core.DownsampleMode;
import kotlin.enums.EnumEntries;

/**
 * Fragment that illustrates how to use the image pipeline directly in order to create
 * notifications.
 */
public class ImagePipelineDownsampleFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImagePipelineDownsampleFragment";

  private final EnumEntries<DownsampleMode> SPINNER_ENTRIES_MODES = DownsampleMode.getEntries();

  private ImageView mImage;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_imagepipeline_downsample, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    mImage = (ImageView) view.findViewById(R.id.image);
    reloadImage(SPINNER_ENTRIES_MODES.get(0));

    final Spinner modeSpinner = (Spinner) view.findViewById(R.id.spinner_mode);
    modeSpinner.setAdapter(new SimpleModeOptionsAdapter());
    modeSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            DownsampleMode newMode = SPINNER_ENTRIES_MODES.get(position);
            reloadImage(newMode);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
  }

  private void reloadImage(DownsampleMode mode) {
    final Uri imageUri = UriUtil.getUriForResourceId(R.drawable.large_image);
    Fresco.getImagePipeline().evictFromCache(imageUri);
    VitoView.show(
        imageUri,
        ImageOptions.create()
            .overlayRes(R.drawable.resize_outline)
            .scale(ScalingUtils.ScaleType.CENTER_CROP)
            .errorRes(R.color.primaryDark)
            .downsampleOverride(mode)
            .build(),
        CALLER_CONTEXT,
        mImage);
  }

  private class SimpleModeOptionsAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      return SPINNER_ENTRIES_MODES.size();
    }

    @Override
    public Object getItem(int position) {
      return SPINNER_ENTRIES_MODES.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final LayoutInflater layoutInflater = getLayoutInflater();

      final View view =
          convertView != null
              ? convertView
              : layoutInflater.inflate(
                  android.R.layout.simple_spinner_dropdown_item, parent, false);

      final TextView textView = (TextView) view.findViewById(android.R.id.text1);
      textView.setText(SPINNER_ENTRIES_MODES.get(position).toString());

      return view;
    }
  }
}
