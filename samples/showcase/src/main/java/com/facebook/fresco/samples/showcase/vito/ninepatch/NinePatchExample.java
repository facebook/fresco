/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito.ninepatch;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.BaseCloseableImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.infer.annotation.Nullsafe;
import java.io.InputStream;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class NinePatchExample {

  public static class NinePatchClosableImage extends BaseCloseableImage {
    private boolean mClosed = false;
    @Nullable private NinePatch mNinePatch;
    private final int mSizeInBytes;

    public NinePatchClosableImage(NinePatch ninePatch, int sizeInBytes) {
      mNinePatch = ninePatch;
      mSizeInBytes = sizeInBytes;
    }

    @Override
    public int getSizeInBytes() {
      return mSizeInBytes;
    }

    @Override
    public void close() {
      mNinePatch = null;
      mClosed = true;
    }

    @Override
    public boolean isClosed() {
      return mClosed;
    }

    @Override
    public boolean isStateful() {
      return false;
    }

    @Override
    public int getWidth() {
      return 0;
    }

    @Override
    public int getHeight() {
      return 0;
    }

    @Override
    public QualityInfo getQualityInfo() {
      return ImmutableQualityInfo.FULL_QUALITY;
    }

    @Override
    public ImageInfo getImageInfo() {
      return this;
    }

    @Nullable
    public NinePatch getNinePatch() {
      return mNinePatch;
    }
  }

  public static class NinePatchDecoder implements ImageDecoder {

    @Override
    public CloseableImage decode(
        EncodedImage encodedImage,
        int length,
        QualityInfo qualityInfo,
        ImageDecodeOptions options) {
      InputStream inputStream = encodedImage.getInputStream();
      Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
      int sizeInBytes = BitmapUtil.getSizeInBytes(bitmap);
      byte[] chunk = bitmap.getNinePatchChunk();
      NinePatch ninePatch = new NinePatch(bitmap, chunk, null);
      return new NinePatchClosableImage(ninePatch, sizeInBytes);
    }
  }

  public static class NinePatchDrawableFactory implements ImageOptionsDrawableFactory {
    private final Resources mRes;

    public NinePatchDrawableFactory(Resources res) {
      mRes = res;
    }

    @Nullable
    @Override
    public Drawable createDrawable(
        Resources resources, CloseableImage closeableImage, ImageOptions imageOptions) {
      NinePatch ninePatch = ((NinePatchClosableImage) closeableImage).getNinePatch();
      if (ninePatch != null) {
        return new NinePatchDrawable(mRes, ninePatch);
      }
      return null;
    }
  }
}
