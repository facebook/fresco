/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imagepipeline;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.samples.showcase.vito.source.ImageRequestImageSource;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

public class PartialRequestFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "PartialRequestFragment";

  private static final ImageOptions IMAGE_OPTIONS =
      ImageOptions.create()
          .placeholderRes(R.mipmap.ic_launcher, ScalingUtils.ScaleType.CENTER)
          .scale(ScalingUtils.ScaleType.CENTER_CROP)
          .build();

  public PartialRequestFragment() {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_partial_request, container, false);
  }

  @Override
  public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    final ImageView partialImage = (ImageView) view.findViewById(R.id.partial_img);

    final ImageView fullImage = (ImageView) view.findViewById(R.id.full_img);

    final Button clearCacheButton = (Button) view.findViewById(R.id.clear_cache);
    clearCacheButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            VitoView.release(partialImage);
            VitoView.release(fullImage);
            Fresco.getImagePipeline().clearDiskCaches();
            Fresco.getImagePipeline().clearMemoryCaches();
          }
        });

    final Button prefetchButton = (Button) view.findViewById(R.id.prefetch_now);
    prefetchButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            loadImage(partialImage, BytesRange.toMax(30000));
          }
        });

    Button loadFull = (Button) view.findViewById(R.id.load_full);
    loadFull.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            loadImage(fullImage, null);
          }
        });
  }

  private void loadImage(ImageView imageView, @Nullable BytesRange bytesRange) {
    final ImageRequest imageRequest =
        ImageRequestBuilder.newBuilderWithSource(
                sampleUris().createSampleUri(ImageUriProvider.ImageSize.L))
            .setBytesRange(bytesRange)
            .build();
    VitoView.show(
        new ImageRequestImageSource(imageRequest), IMAGE_OPTIONS, CALLER_CONTEXT, imageView);
  }
}
