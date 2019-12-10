/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.imagepipeline.widget.ResizableFrameLayout;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.core.FrescoVitoImageDecodeOptions;
import com.facebook.fresco.vito.core.FrescoVitoRegionDecoder;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/** Simple experimental Fresco Vito fragment that just displays an image. */
public class FrescoVitoLithoRegionDecodeFragment extends BaseShowcaseFragment {

  private SimpleDraweeView mFullDraweeView;
  private ResizableFrameLayout mSelectedParentBounds;
  private ResizableFrameLayout mSelectedFocusPoint;
  private SimpleDraweeView mRegionDraweeView;
  private Uri mUri;
  private @Nullable ImageInfo mImageInfo;

  private final ControllerListener<ImageInfo> mControllerListener =
      new BaseControllerListener<ImageInfo>() {
        @Override
        public void onFinalImageSet(
            String id,
            @javax.annotation.Nullable ImageInfo imageInfo,
            @javax.annotation.Nullable Animatable animatable) {
          mImageInfo = imageInfo;
          mSelectedParentBounds.setUpdateMaximumDimensionOnNextSizeChange(true);
          mSelectedFocusPoint.setUpdateMaximumDimensionOnNextSizeChange(true);
          if (imageInfo != null) {
            mFullDraweeView.setAspectRatio(imageInfo.getWidth() / (float) imageInfo.getHeight());
            mFullDraweeView.requestLayout();
            updateRegion();
          }
        }
      };

  private final ResizableFrameLayout.SizeChangedListener mSizeChangedListener =
      new ResizableFrameLayout.SizeChangedListener() {
        @Override
        public void onSizeChanged(int widthPx, int heightPx) {
          updateRegion();
        }
      };

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_vito_litho_region_decoding, container, false);
  }

  @Override
  public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
    mUri =
        sampleUris()
            .createSampleUri(ImageUriProvider.ImageSize.L, ImageUriProvider.Orientation.LANDSCAPE);

    mFullDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view_full);
    mFullDraweeView.setController(
        Fresco.newDraweeControllerBuilder()
            .setUri(mUri)
            .setControllerListener(mControllerListener)
            .build());

    mSelectedParentBounds = (ResizableFrameLayout) view.findViewById(R.id.frame_parent_bounds);
    mSelectedParentBounds.init(view.findViewById(R.id.btn_resize_parent_bounds));
    mSelectedParentBounds.setSizeChangedListener(mSizeChangedListener);

    mSelectedFocusPoint = (ResizableFrameLayout) view.findViewById(R.id.frame_focus_point);
    mSelectedFocusPoint.init(view.findViewById(R.id.btn_resize_focus_point));
    mSelectedFocusPoint.setSizeChangedListener(mSizeChangedListener);

    mRegionDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view_region);
    mRegionDraweeView.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            updateRegion();
          }
        });
  }

  private void updateRegion() {
    if (mImageInfo == null) {
      return;
    }
    int right =
        mSelectedParentBounds.getMeasuredWidth()
            * mImageInfo.getWidth()
            / mFullDraweeView.getMeasuredWidth();
    int bottom =
        mSelectedParentBounds.getMeasuredHeight()
            * mImageInfo.getHeight()
            / mFullDraweeView.getMeasuredHeight();
    PointF focusPoint =
        new PointF(
            (float) mSelectedFocusPoint.getMeasuredWidth()
                / (float) mFullDraweeView.getMeasuredWidth(),
            (float) mSelectedFocusPoint.getMeasuredHeight()
                / (float) mFullDraweeView.getMeasuredHeight());

    ImageDecoder regionDecoder = createRegionDecoder();
    mRegionDraweeView.setController(
        Fresco.newDraweeControllerBuilder()
            .setImageRequest(
                ImageRequestBuilder.newBuilderWithSource(mUri)
                    .setImageDecodeOptions(
                        FrescoVitoImageDecodeOptions.newBuilder()
                            .setCustomImageDecoder(regionDecoder)
                            .setScaleType(ScalingUtils.ScaleType.FOCUS_CROP)
                            .setFocusPoint(focusPoint)
                            .setParentBounds(new Rect(0, 0, right, bottom))
                            .build())
                    .build())
            .build());
  }

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_region_decoding_title;
  }

  private ImageDecoder createRegionDecoder() {
    return new FrescoVitoRegionDecoder(Fresco.getImagePipelineFactory().getPlatformDecoder());
  }
}
