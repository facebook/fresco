/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.common.ImageViewWithAspectRatio;
import com.facebook.fresco.samples.showcase.imagepipeline.widget.ResizableFrameLayout;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.listener.BaseImageListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.ImageInfo;

/** Simple experimental Fresco Vito fragment that just displays an image. */
public class FrescoVitoLithoRegionDecodeFragment extends BaseShowcaseFragment {

  private static final ImageOptions IMAGE_OPTIONS =
      ImageOptions.create().scale(ScalingUtils.ScaleType.FIT_CENTER).build();

  private ImageViewWithAspectRatio mFullImageView;
  private ResizableFrameLayout mSelectedParentBounds;
  private ResizableFrameLayout mSelectedFocusPoint;
  private ImageView mRegionImageView;
  private Uri mUri;
  private @Nullable ImageInfo mImageInfo;

  private final ImageDecoder mRegionDecoder = createRegionDecoder();

  private final ImageListener mImageListener =
      new BaseImageListener() {
        @Override
        public void onFinalImageSet(
            long id, int imageOrigin, @Nullable ImageInfo imageInfo, @Nullable Drawable drawable) {
          mImageInfo = imageInfo;
          mSelectedParentBounds.setUpdateMaximumDimensionOnNextSizeChange(true);
          mSelectedFocusPoint.setUpdateMaximumDimensionOnNextSizeChange(true);
          if (imageInfo != null) {
            mFullImageView.setAspectRatio(imageInfo.getWidth() / (float) imageInfo.getHeight());
            mFullImageView.requestLayout();
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

    mFullImageView = (ImageViewWithAspectRatio) view.findViewById(R.id.image_full);
    mFullImageView.setAspectRatio(2f);
    VitoView.show(
        mUri, IMAGE_OPTIONS, "FrescoVitoLithoRegionDecodeFragment", mImageListener, mFullImageView);

    mSelectedParentBounds = (ResizableFrameLayout) view.findViewById(R.id.frame_parent_bounds);
    mSelectedParentBounds.init(view.findViewById(R.id.btn_resize_parent_bounds));
    mSelectedParentBounds.setSizeChangedListener(mSizeChangedListener);

    mSelectedFocusPoint = (ResizableFrameLayout) view.findViewById(R.id.frame_focus_point);
    mSelectedFocusPoint.init(view.findViewById(R.id.btn_resize_focus_point));
    mSelectedFocusPoint.setSizeChangedListener(mSizeChangedListener);

    mRegionImageView = (ImageView) view.findViewById(R.id.image_region);
    mRegionImageView.setOnClickListener(
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
            / mFullImageView.getMeasuredWidth();
    int bottom =
        mSelectedParentBounds.getMeasuredHeight()
            * mImageInfo.getHeight()
            / mFullImageView.getMeasuredHeight();
    PointF focusPoint =
        new PointF(
            (float) mSelectedFocusPoint.getMeasuredWidth()
                / (float) mFullImageView.getMeasuredWidth(),
            (float) mSelectedFocusPoint.getMeasuredHeight()
                / (float) mFullImageView.getMeasuredHeight());

    VitoView.show(
        mUri,
        ImageOptions.create()
            .imageDecodeOptions(
                FrescoVitoImageDecodeOptions.newBuilder()
                    .setCustomImageDecoder(mRegionDecoder)
                    .setScaleType(ScalingUtils.ScaleType.FOCUS_CROP)
                    .setFocusPoint(focusPoint)
                    .setParentBounds(new Rect(0, 0, right, bottom))
                    .build())
            .build(),
        "FrescoVitoLithoRegionDecodeFragment",
        mRegionImageView);
  }

  private ImageDecoder createRegionDecoder() {
    return new FrescoVitoRegionDecoder(Fresco.getImagePipelineFactory().getPlatformDecoder());
  }
}
