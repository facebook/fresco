/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.RetainingDataSourceSupplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.List;

public class RetainingDataSourceSupplierFragment extends BaseShowcaseFragment {

  private List<Uri> mSampleUris;
  private int mUriIndex = 0;

  private ControllerListener controllerListener =
      new BaseControllerListener<ImageInfo>() {
        @Override
        public void onFinalImageSet(
            String id, @Nullable ImageInfo imageInfo, @Nullable Animatable anim) {
          if (anim != null) {
            // app-specific logic to enable animation starting
            anim.start();
          }
        }
      };

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSampleUris = sampleUris().getSampleGifUris();
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_retaining_supplier, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final SimpleDraweeView simpleDraweeView = view.findViewById(R.id.drawee_view);
    final RetainingDataSourceSupplier<CloseableReference<CloseableImage>> retainingSupplier =
        new RetainingDataSourceSupplier<>();
    simpleDraweeView.setController(
        Fresco.newDraweeControllerBuilder()
            .setDataSourceSupplier(retainingSupplier)
            .setControllerListener(controllerListener)
            .build());
    replaceImage(retainingSupplier);
    simpleDraweeView.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            replaceImage(retainingSupplier);
          }
        });
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_retaining_supplier_title;
  }

  private void replaceImage(
      RetainingDataSourceSupplier<CloseableReference<CloseableImage>> retainingSupplier) {

    retainingSupplier.replaceSupplier(
        Fresco.getImagePipeline()
            .getDataSourceSupplier(
                ImageRequest.fromUri(getNextUri()), null, ImageRequest.RequestLevel.FULL_FETCH));
  }

  private synchronized Uri getNextUri() {
    int previousIndex = mUriIndex;
    mUriIndex = (mUriIndex + 1) % mSampleUris.size();
    return mSampleUris.get(previousIndex);
  }
}
