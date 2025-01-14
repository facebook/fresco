/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.RetainingDataSourceSupplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.vito.core.impl.source.DataSourceImageSource;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.List;

public class RetainingDataSourceSupplierFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "RetainingDataSourceSupplierFragment";

  private final ImageOptions mImageOptions = ImageOptions.create().autoPlay(true).build();
  private List<Uri> mSampleUris;
  private int mUriIndex = 0;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSampleUris = sampleUris().getSampleGifUris();
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_vito_retaining_supplier, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final ImageView imageView = view.findViewById(R.id.image_view);
    final RetainingDataSourceSupplier<CloseableReference<CloseableImage>> retainingSupplier =
        new RetainingDataSourceSupplier<>();
    VitoView.show(
        new DataSourceImageSource(retainingSupplier), mImageOptions, CALLER_CONTEXT, imageView);
    replaceImage(retainingSupplier);
    imageView.setOnClickListener(v -> replaceImage(retainingSupplier));
  }

  private void replaceImage(
      RetainingDataSourceSupplier<CloseableReference<CloseableImage>> retainingSupplier) {

    retainingSupplier.replaceSupplier(
        Fresco.getImagePipeline()
            .getDataSourceSupplier(
                ImageRequest.fromUri(getNextUri()),
                CALLER_CONTEXT,
                ImageRequest.RequestLevel.FULL_FETCH));
  }

  private synchronized Uri getNextUri() {
    int previousIndex = mUriIndex;
    mUriIndex = (mUriIndex + 1) % mSampleUris.size();
    return mSampleUris.get(previousIndex);
  }
}
