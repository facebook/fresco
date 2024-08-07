/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imageformat.gif;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import com.facebook.animated.giflite.GifDecoder;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ImageDecodeOptionsBuilder;

/** GIF example that illustrates how to display a simple GIF file */
public class ImageFormatGifFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImageFormatGifFragment";
  private Entry[] mSpinnerEntries;

  private Spinner mSpinner;
  private ImageView mImageView;
  private @Nullable GifDecoder mGifDecoder;
  private boolean mAutoPlayEnabled;
  private boolean mIsPlaying;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_gif, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    mSpinnerEntries =
        new Entry[] {
          new Entry(
              R.string.format_gif_label_small,
              sampleUris().createGifUri(ImageUriProvider.ImageSize.S)),
          new Entry(
              R.string.format_gif_label_medium,
              sampleUris().createGifUri(ImageUriProvider.ImageSize.M)),
          new Entry(
              R.string.format_gif_label_large,
              sampleUris().createGifUri(ImageUriProvider.ImageSize.L)),
        };

    mImageView = view.findViewById(R.id.image);

    final SwitchCompat switchBackground = view.findViewById(R.id.switch_background);
    switchBackground.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mImageView.setBackground(isChecked ? new CheckerBoardDrawable(getResources()) : null);
          }
        });
    final SwitchCompat switchAutoPlay = view.findViewById(R.id.switch_autoplay);
    switchAutoPlay.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mAutoPlayEnabled = isChecked;
            refreshAnimation();
          }
        });
    mAutoPlayEnabled = switchAutoPlay.isEnabled();

    final SwitchCompat switchAspect = view.findViewById(R.id.switch_aspect_ratio);
    switchAspect.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            ViewGroup.LayoutParams layoutParams = mImageView.getLayoutParams();
            layoutParams.height = layoutParams.width * (isChecked ? 2 : 1);
            mImageView.setLayoutParams(layoutParams);
          }
        });

    mSpinner = (Spinner) view.findViewById(R.id.spinner);
    mSpinner.setAdapter(new SimpleUriListAdapter());
    mSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            refreshAnimation();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    mSpinner.setSelection(0);

    final Spinner decoderSpinner = view.findViewById(R.id.spinner_select_decoder);
    decoderSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
              case 0:
                mGifDecoder = null;
                break;
              case 1:
                mGifDecoder = new GifDecoder();
                break;
              default:
                throw new IllegalArgumentException("Unknown decoder selected");
            }
            refreshAnimation();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    decoderSpinner.setSelection(0);
  }

  private void refreshAnimation() {
    final Entry spinnerEntry = mSpinnerEntries[mSpinner.getSelectedItemPosition()];
    setAnimationUri(spinnerEntry.uri);
  }

  private void setAnimationUri(Uri uri) {

    final ImageDecodeOptionsBuilder optionsBuilder =
        ImageDecodeOptions.newBuilder().setMaxDimensionPx(4000);

    if (mGifDecoder != null) {
      optionsBuilder.setCustomImageDecoder(mGifDecoder);
    }
    final ImageOptions imageOptionsAutoPlay =
        ImageOptions.create().autoPlay(true).imageDecodeOptions(optionsBuilder.build()).build();
    final ImageOptions imageOptionsPaused =
        imageOptionsAutoPlay.extend().autoPlay(false).overlay(getPlayOverlayDrawable()).build();
    if (!mAutoPlayEnabled) {
      mImageView.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (mIsPlaying) {
                VitoView.show(uri, imageOptionsPaused, CALLER_CONTEXT, mImageView);
              } else {
                VitoView.show(uri, imageOptionsAutoPlay, CALLER_CONTEXT, mImageView);
              }
              mIsPlaying = !mIsPlaying;
            }
          });
      VitoView.show(uri, imageOptionsPaused, CALLER_CONTEXT, mImageView);
    } else {
      VitoView.show(uri, imageOptionsAutoPlay, CALLER_CONTEXT, mImageView);
    }
  }

  public Drawable getPlayOverlayDrawable() {
    return new ScaleTypeDrawable(
        getResources().getDrawable(android.R.drawable.ic_media_play),
        ScalingUtils.ScaleType.CENTER);
  }

  private class SimpleUriListAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      return mSpinnerEntries.length;
    }

    @Override
    public Entry getItem(int position) {
      return mSpinnerEntries[position];
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
      textView.setText(mSpinnerEntries[position].descriptionId);

      return view;
    }
  }

  private static class Entry {

    final int descriptionId;
    final Uri uri;

    private Entry(int descriptionId, Uri uri) {
      this.descriptionId = descriptionId;
      this.uri = uri;
    }
  }
}
