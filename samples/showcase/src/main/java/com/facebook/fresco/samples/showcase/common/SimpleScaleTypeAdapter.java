/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.common;

import android.graphics.PointF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.facebook.drawee.drawable.ScalingUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SimpleScaleTypeAdapter extends BaseAdapter {

  private static final Entry[] BUILT_IN_SPINNER_ENTRIES =
      new Entry[] {
        new Entry(ScalingUtils.ScaleType.CENTER, "center", null),
        new Entry(ScalingUtils.ScaleType.CENTER_CROP, "center_crop", null),
        new Entry(ScalingUtils.ScaleType.CENTER_INSIDE, "center_inside", null),
        new Entry(ScalingUtils.ScaleType.FIT_CENTER, "fit_center", null),
        new Entry(ScalingUtils.ScaleType.FIT_START, "fit_start", null),
        new Entry(ScalingUtils.ScaleType.FIT_END, "fit_end", null),
        new Entry(ScalingUtils.ScaleType.FIT_XY, "fit_xy", null),
        new Entry(ScalingUtils.ScaleType.FOCUS_CROP, "focus_crop (0, 0)", new PointF(0, 0)),
        new Entry(ScalingUtils.ScaleType.FOCUS_CROP, "focus_crop (1, 0.5)", new PointF(1, 0.5f))
      };

  private static final Entry[] CUSTOM_TYPES =
      new Entry[] {
        new Entry(CustomScaleTypes.FIT_X, "custom: fit_x", null),
        new Entry(CustomScaleTypes.FIT_Y, "custom: fit_y", null),
      };

  public static SimpleScaleTypeAdapter createForAllScaleTypes() {
    List<Entry> entries = new ArrayList<>(BUILT_IN_SPINNER_ENTRIES.length + CUSTOM_TYPES.length);
    Collections.addAll(entries, BUILT_IN_SPINNER_ENTRIES);
    Collections.addAll(entries, CUSTOM_TYPES);
    return new SimpleScaleTypeAdapter(entries);
  }

  private final List<Entry> mEntries;

  private SimpleScaleTypeAdapter(Entry[] entries) {
    this(Arrays.asList(entries));
  }

  private SimpleScaleTypeAdapter(List<Entry> entries) {
    mEntries = entries;
  }

  @Override
  public int getCount() {
    return mEntries.size();
  }

  @Override
  public Object getItem(int position) {
    return mEntries.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

    final View view =
        convertView != null
            ? convertView
            : layoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);

    final TextView textView = (TextView) view.findViewById(android.R.id.text1);
    textView.setText(mEntries.get(position).description);

    return view;
  }

  public static class Entry {

    public final ScalingUtils.ScaleType scaleType;
    public final String description;
    public final @Nullable PointF focusPoint;

    private Entry(
        ScalingUtils.ScaleType scaleType, String description, @Nullable PointF focusPoint) {
      this.scaleType = scaleType;
      this.description = description;
      this.focusPoint = focusPoint;
    }
  }
}
