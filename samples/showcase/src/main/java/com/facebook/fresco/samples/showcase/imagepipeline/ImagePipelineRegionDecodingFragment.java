/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imagepipeline;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
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
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.platform.PlatformDecoder;

/** Simple region decoding example that renders the original image and a selected region. */
public class ImagePipelineRegionDecodingFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImagePipelineRegionDecodingFragment";
  private static final ImageOptions IMAGE_OPTIONS =
      ImageOptions.create().scale(ScalingUtils.ScaleType.FIT_CENTER).build();

  private ImageViewWithAspectRatio mFullImageView;
  private ResizableFrameLayout mSelectedRegion;
  private ImageView mRegionImageView;
  private Uri mUri;
  private @Nullable ImageInfo mImageInfo;

  private final ImageListener mImageListener =
      new BaseImageListener() {
        @Override
        public void onFinalImageSet(
            long id, int imageOrigin, @Nullable ImageInfo imageInfo, @Nullable Drawable drawable) {
          mImageInfo = imageInfo;
          mSelectedRegion.setUpdateMaximumDimensionOnNextSizeChange(true);
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
    return inflater.inflate(R.layout.fragment_imagepipeline_region_decoding, container, false);
  }

  @Override
  public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
    mUri =
        sampleUris()
            .createSampleUri(ImageUriProvider.ImageSize.L, ImageUriProvider.Orientation.LANDSCAPE);

    mFullImageView = (ImageViewWithAspectRatio) view.findViewById(R.id.image_view_full);
    VitoView.show(mUri, IMAGE_OPTIONS, CALLER_CONTEXT, mImageListener, mFullImageView);

    mSelectedRegion = (ResizableFrameLayout) view.findViewById(R.id.frame_main);
    mSelectedRegion.init(view.findViewById(R.id.btn_resize));
    mSelectedRegion.setSizeChangedListener(mSizeChangedListener);

    mRegionImageView = (ImageView) view.findViewById(R.id.image_view_region);
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
    int left = 0;
    int top = 0;
    int right =
        mSelectedRegion.getMeasuredWidth()
            * mImageInfo.getWidth()
            / mFullImageView.getMeasuredWidth();
    int bottom =
        mSelectedRegion.getMeasuredHeight()
            * mImageInfo.getHeight()
            / mFullImageView.getMeasuredHeight();

    ImageDecoder regionDecoder = createRegionDecoder(left, top, right, bottom);
    VitoView.show(
        mUri,
        IMAGE_OPTIONS
            .extend()
            .imageDecodeOptions(
                ImageDecodeOptions.newBuilder().setCustomImageDecoder(regionDecoder).build())
            .build(),
        CALLER_CONTEXT,
        mRegionImageView);
  }

  private ImageDecoder createRegionDecoder(int left, int top, int right, int bottom) {
    return new RegionDecoder(
        Fresco.getImagePipelineFactory().getPlatformDecoder(), new Rect(left, top, right, bottom));
  }

  public static class RegionDecoder implements ImageDecoder {

    private final PlatformDecoder mPlatformDecoder;
    private final Rect mRegion;

    public RegionDecoder(PlatformDecoder platformDecoder, Rect region) {
      mPlatformDecoder = platformDecoder;
      mRegion = region;
    }

    @Override
    public CloseableImage decode(
        EncodedImage encodedImage,
        int length,
        QualityInfo qualityInfo,
        ImageDecodeOptions options) {
      CloseableReference<Bitmap> decodedBitmapReference =
          mPlatformDecoder.decodeJPEGFromEncodedImageWithColorSpace(
              encodedImage, options.bitmapConfig, mRegion, length, options.colorSpace);
      try {
        return CloseableStaticBitmap.of(decodedBitmapReference, qualityInfo, 0);
      } finally {
        CloseableReference.closeSafely(decodedBitmapReference);
      }
    }
  }
}
