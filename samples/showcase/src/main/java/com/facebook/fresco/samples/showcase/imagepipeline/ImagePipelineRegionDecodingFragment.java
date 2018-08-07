/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.fresco.samples.showcase.imagepipeline;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.imagepipeline.widget.ResizableFrameLayout;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/** Simple region decoding example that renders the original image and a selected region. */
public class ImagePipelineRegionDecodingFragment extends BaseShowcaseFragment {

  private SimpleDraweeView mFullDraweeView;
  private ResizableFrameLayout mSelectedRegion;
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
          mSelectedRegion.setUpdateMaximumDimensionOnNextSizeChange(true);
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
    return inflater.inflate(R.layout.fragment_imagepipeline_region_decoding, container, false);
  }

  @Override
  public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
    final ImageUriProvider imageUriProvider = ImageUriProvider.getInstance(getContext());
    mUri =
        imageUriProvider.createSampleUri(
            ImageUriProvider.ImageSize.L, ImageUriProvider.Orientation.LANDSCAPE);

    mFullDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view_full);
    mFullDraweeView.setController(
        Fresco.newDraweeControllerBuilder()
            .setUri(mUri)
            .setControllerListener(mControllerListener)
            .build());

    mSelectedRegion = (ResizableFrameLayout) view.findViewById(R.id.frame_main);
    mSelectedRegion.init(view.findViewById(R.id.btn_resize));
    mSelectedRegion.setSizeChangedListener(mSizeChangedListener);

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
    int left = 0;
    int top = 0;
    int right =
        mSelectedRegion.getMeasuredWidth()
            * mImageInfo.getWidth()
            / mFullDraweeView.getMeasuredWidth();
    int bottom =
        mSelectedRegion.getMeasuredHeight()
            * mImageInfo.getHeight()
            / mFullDraweeView.getMeasuredHeight();

    ImageDecoder regionDecoder = createRegionDecoder(left, top, right, bottom);
    mRegionDraweeView.setController(
        Fresco.newDraweeControllerBuilder()
            .setImageRequest(
                ImageRequestBuilder.newBuilderWithSource(mUri)
                    .setImageDecodeOptions(
                        ImageDecodeOptions.newBuilder()
                            .setCustomImageDecoder(regionDecoder)
                            .build())
                    .build())
            .build());
  }

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_region_decoding_title;
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
              encodedImage, options.bitmapConfig, mRegion, length, options.transformToSRGB);
      try {
        return new CloseableStaticBitmap(decodedBitmapReference, qualityInfo, 0);
      } finally {
        CloseableReference.closeSafely(decodedBitmapReference);
      }
    }
  }
}
