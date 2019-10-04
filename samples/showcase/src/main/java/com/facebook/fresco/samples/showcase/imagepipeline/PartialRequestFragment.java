/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import androidx.annotation.Nullable;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

public class PartialRequestFragment extends BaseShowcaseFragment {

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

    final SimpleDraweeView partialDrawee =
        (SimpleDraweeView) view.findViewById(R.id.drawee_partial_img);

    final SimpleDraweeView fullDrawee = (SimpleDraweeView) view.findViewById(R.id.drawee_full_img);

    final Button clearCacheButton = (Button) view.findViewById(R.id.clear_cache);
    clearCacheButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            partialDrawee.setController(null);
            fullDrawee.setController(null);
            Fresco.getImagePipeline().clearDiskCaches();
            Fresco.getImagePipeline().clearMemoryCaches();
          }
        });

    final Button prefetchButton = (Button) view.findViewById(R.id.prefetch_now);
    prefetchButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            loadImageIntoDrawee(partialDrawee, BytesRange.toMax(30000));
          }
        });

    Button loadFull = (Button) view.findViewById(R.id.load_full);
    loadFull.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            loadImageIntoDrawee(fullDrawee, null);
          }
        });
  }

  private void loadImageIntoDrawee(SimpleDraweeView draweeView, @Nullable BytesRange bytesRange) {
    final ImageRequest imageRequest =
        ImageRequestBuilder.newBuilderWithSource(
                sampleUris().createSampleUri(ImageUriProvider.ImageSize.L))
            .setBytesRange(bytesRange)
            .build();

    final DraweeController draweeController =
        Fresco.newDraweeControllerBuilder()
            .setOldController(draweeView.getController())
            .setImageRequest(imageRequest)
            .build();

    draweeView.setController(draweeController);
  }

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_partial_request_title;
  }
}
