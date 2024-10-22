/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imageformat.xml;

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
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.common.util.UriUtil;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;

/** This fragment displays different XML images. */
public class ImageFormatXmlFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImageFormatXmlFragment";

  private ImageView mImageView;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_xml, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    mImageView = view.findViewById(R.id.image);

    final Spinner spinner = view.findViewById(R.id.spinner);
    final Entry[] entries =
        new Entry[] {
          new Entry("Vector", fromResourceId(R.drawable.xml_vector)),
          new Entry("Nine patch", fromResourceId(R.drawable.xml_nine_patch)),
          new Entry("Layer list", fromResourceId(R.drawable.xml_layer_list)),
          new Entry("Level list", fromResourceId(R.drawable.xml_level_list)),
          new Entry("State list", fromResourceId(R.drawable.xml_state_list)),
          new Entry("Bitmap", fromResourceId(R.drawable.xml_bitmap)),
        };
    spinner.setAdapter(new SimpleUriListAdapter(entries));
    spinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final Entry spinnerEntry = entries[spinner.getSelectedItemPosition()];
            setImageUri(spinnerEntry.uri);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    spinner.setSelection(0);
  }

  private void setImageUri(Uri uri) {
    VitoView.show(uri, ImageOptions.create().build(), CALLER_CONTEXT, mImageView);
  }

  private Uri fromResourceId(@DrawableRes int resourceId) {
    return UriUtil.getUriForResourceId(resourceId);
  }

  private class SimpleUriListAdapter extends BaseAdapter {
    final Entry[] mEntries;

    SimpleUriListAdapter(Entry[] entries) {
      this.mEntries = entries;
    }

    @Override
    public int getCount() {
      return mEntries.length;
    }

    @Override
    public Entry getItem(int position) {
      return mEntries[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final LayoutInflater layoutInflater = getLayoutInflater(null);
      final View view =
          convertView != null
              ? convertView
              : layoutInflater.inflate(
                  android.R.layout.simple_spinner_dropdown_item, parent, false);
      final TextView textView = (TextView) view.findViewById(android.R.id.text1);
      textView.setText(mEntries[position].label);
      return view;
    }
  }

  private static class Entry {
    final @NonNull String label;
    final Uri uri;

    private Entry(@NonNull String label, Uri uri) {
      this.label = label;
      this.uri = uri;
    }
  }
}
