/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.core;

import android.graphics.Bitmap;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.webp.WebpBitmapFactory;

/**
 * Encapsulates additional elements of the {@link ImagePipelineConfig} which are currently in an
 * experimental state.
 *
 * <p>These options may often change or disappear altogether and it is not recommended to change
 * their values from their defaults.
 */
public class ImagePipelineExperiments {

  private final boolean mWebpSupportEnabled;
  private final boolean mExternalCreatedBitmapLogEnabled;
  private final Supplier<Boolean> mMediaVariationsIndexEnabled;
  private final WebpBitmapFactory.WebpErrorLogger mWebpErrorLogger;
  private final boolean mDecodeCancellationEnabled;
  private final WebpBitmapFactory mWebpBitmapFactory;
  private final boolean mSuppressBitmapPrefetching;
  private final boolean mUseDownsamplingRatioForResizing;
  private final boolean mUseBitmapPrepareToDraw;
  private final int mBitmapPrepareToDrawMinSizeBytes;
  private final int mBitmapPrepareToDrawMaxSizeBytes;
  private boolean mBitmapPrepareToDrawForPrefetch;
  private final boolean mPartialImageCachingEnabled;
  private final Supplier<Boolean> mSmartResizingEnabled;

  private ImagePipelineExperiments(Builder builder) {
    mWebpSupportEnabled = builder.mWebpSupportEnabled;
    mExternalCreatedBitmapLogEnabled = builder.mExternalCreatedBitmapLogEnabled;
    if (builder.mMediaVariationsIndexEnabled != null) {
      mMediaVariationsIndexEnabled = builder.mMediaVariationsIndexEnabled;
    } else {
      mMediaVariationsIndexEnabled = new Supplier<Boolean>() {
        @Override
        public Boolean get() {
          return Boolean.FALSE;
        }
      };
    }
    mWebpErrorLogger = builder.mWebpErrorLogger;
    mDecodeCancellationEnabled = builder.mDecodeCancellationEnabled;
    mWebpBitmapFactory = builder.mWebpBitmapFactory;
    mSuppressBitmapPrefetching = builder.mSuppressBitmapPrefetching;
    mUseDownsamplingRatioForResizing = builder.mUseDownsamplingRatioForResizing;
    mUseBitmapPrepareToDraw = builder.mUseBitmapPrepareToDraw;
    mBitmapPrepareToDrawMinSizeBytes = builder.mBitmapPrepareToDrawMinSizeBytes;
    mBitmapPrepareToDrawMaxSizeBytes = builder.mBitmapPrepareToDrawMaxSizeBytes;
    mBitmapPrepareToDrawForPrefetch = builder.mBitmapPrepareToDrawForPrefetch;
    mPartialImageCachingEnabled = builder.mPartialImageCachingEnabled;
    mSmartResizingEnabled = builder.mSmartResizingEnabled;
  }

  public boolean isExternalCreatedBitmapLogEnabled() {
    return mExternalCreatedBitmapLogEnabled;
  }

  public boolean getMediaVariationsIndexEnabled() {
    return mMediaVariationsIndexEnabled.get().booleanValue();
  }

  public boolean getUseDownsamplingRatioForResizing() {
    return mUseDownsamplingRatioForResizing;
  }

  public boolean isWebpSupportEnabled() {
    return mWebpSupportEnabled;
  }

  public boolean isDecodeCancellationEnabled() {
    return mDecodeCancellationEnabled;
  }

  public WebpBitmapFactory.WebpErrorLogger getWebpErrorLogger() {
    return mWebpErrorLogger;
  }

  public WebpBitmapFactory getWebpBitmapFactory() {
    return mWebpBitmapFactory;
  }

  public boolean getUseBitmapPrepareToDraw() {
    return mUseBitmapPrepareToDraw;
  }

  public int getBitmapPrepareToDrawMinSizeBytes() {
    return mBitmapPrepareToDrawMinSizeBytes;
  }

  public int getBitmapPrepareToDrawMaxSizeBytes() {
    return mBitmapPrepareToDrawMaxSizeBytes;
  }

  public boolean isPartialImageCachingEnabled() {
    return mPartialImageCachingEnabled;
  }

  public Supplier<Boolean> isSmartResizingEnabled() {
    return mSmartResizingEnabled;
  }

  public static ImagePipelineExperiments.Builder newBuilder(
      ImagePipelineConfig.Builder configBuilder) {
    return new ImagePipelineExperiments.Builder(configBuilder);
  }

  public boolean getBitmapPrepareToDrawForPrefetch() {
    return mBitmapPrepareToDrawForPrefetch;
  }

  public static class Builder {

    private final ImagePipelineConfig.Builder mConfigBuilder;
    private boolean mWebpSupportEnabled = false;
    private boolean mExternalCreatedBitmapLogEnabled = false;
    private Supplier<Boolean> mMediaVariationsIndexEnabled = null;
    private WebpBitmapFactory.WebpErrorLogger mWebpErrorLogger;
    private boolean mDecodeCancellationEnabled = false;
    private WebpBitmapFactory mWebpBitmapFactory;
    private boolean mSuppressBitmapPrefetching = false;
    private boolean mUseDownsamplingRatioForResizing = false;
    private boolean mUseBitmapPrepareToDraw = false;
    private int mBitmapPrepareToDrawMinSizeBytes = 0;
    private int mBitmapPrepareToDrawMaxSizeBytes = 0;
    public boolean mBitmapPrepareToDrawForPrefetch = false;
    private boolean mPartialImageCachingEnabled = false;
    private Supplier<Boolean> mSmartResizingEnabled = Suppliers.BOOLEAN_FALSE;

    public Builder(ImagePipelineConfig.Builder configBuilder) {
      mConfigBuilder = configBuilder;
    }

    public ImagePipelineConfig.Builder setExternalCreatedBitmapLogEnabled(
        boolean externalCreatedBitmapLogEnabled) {
      mExternalCreatedBitmapLogEnabled = externalCreatedBitmapLogEnabled;
      return mConfigBuilder;
    }

    /**
     * If true, this will allow the image pipeline to keep an index of the ID in each request's
     * {@link com.facebook.imagepipeline.request.MediaVariations} object (if present) to possibly
     * provide fallback images which are present in cache, without the need to explicitly provide
     * alternative variants of the image in the request.
     */
    public ImagePipelineConfig.Builder setMediaVariationsIndexEnabled(
        Supplier<Boolean> mediaVariationsIndexEnabled) {
      mMediaVariationsIndexEnabled = mediaVariationsIndexEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setWebpSupportEnabled(boolean webpSupportEnabled) {
      mWebpSupportEnabled = webpSupportEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setUseDownsampligRatioForResizing(
        boolean useDownsamplingRatioForResizing) {
      mUseDownsamplingRatioForResizing = useDownsamplingRatioForResizing;
      return mConfigBuilder;
    }

    /**
     * Enables the caching of partial image data, for example if the request is cancelled or fails
     * after some data has been received.
     */
    public ImagePipelineConfig.Builder setPartialImageCachingEnabled(
        boolean partialImageCachingEnabled) {
      mPartialImageCachingEnabled = partialImageCachingEnabled;
      return mConfigBuilder;
    }

    public boolean isPartialImageCachingEnabled() {
      return mPartialImageCachingEnabled;
    }

    /**
     * If true we cancel decoding jobs when the related request has been cancelled
     * @param decodeCancellationEnabled If true the decoding of cancelled requests are cancelled
     * @return The Builder itself for chaining
     */
    public ImagePipelineConfig.Builder setDecodeCancellationEnabled(
        boolean decodeCancellationEnabled) {
      mDecodeCancellationEnabled = decodeCancellationEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setWebpErrorLogger(
        WebpBitmapFactory.WebpErrorLogger webpErrorLogger) {
      mWebpErrorLogger = webpErrorLogger;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setWebpBitmapFactory(
        WebpBitmapFactory webpBitmapFactory) {
      mWebpBitmapFactory = webpBitmapFactory;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setSuppressBitmapPrefetching(
        boolean suppressBitmapPrefetching) {
      mSuppressBitmapPrefetching = suppressBitmapPrefetching;
      return mConfigBuilder;
    }

    /**
     * If enabled, the pipeline will call {@link android.graphics.Bitmap#prepareToDraw()} after
     * decoding. This potentially reduces lag on Android N+ as this step now happens async when the
     * RendererThread is idle.
     *
     * @param useBitmapPrepareToDraw set true for enabling prepareToDraw
     * @param minBitmapSizeBytes Bitmaps with a {@link Bitmap#getByteCount()} smaller than this
     *     value are not uploaded
     * @param maxBitmapSizeBytes Bitmaps with a {@link Bitmap#getByteCount()} larger than this value
     *     are not uploaded
     * @param preparePrefetch If this is true, also pre-fetching image requests will trigger the
     *     {@link android.graphics.Bitmap#prepareToDraw()} call.
     * @return The Builder itself for chaining
     */
    public ImagePipelineConfig.Builder setBitmapPrepareToDraw(
        boolean useBitmapPrepareToDraw,
        int minBitmapSizeBytes,
        int maxBitmapSizeBytes,
        boolean preparePrefetch) {
      mUseBitmapPrepareToDraw = useBitmapPrepareToDraw;
      mBitmapPrepareToDrawMinSizeBytes = minBitmapSizeBytes;
      mBitmapPrepareToDrawMaxSizeBytes = maxBitmapSizeBytes;
      mBitmapPrepareToDrawForPrefetch = preparePrefetch;
      return mConfigBuilder;
    }

    /**
     * Smart resizing combines transcoding and downsampling depending on the image format.
     *
     * @param smartResizingEnabled true if smart resizing should be enabled
     * @return The Builder itself for chaining
     */
    public ImagePipelineConfig.Builder setSmartResizingEnabled(
        Supplier<Boolean> smartResizingEnabled) {
      mSmartResizingEnabled = smartResizingEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineExperiments build() {
      return new ImagePipelineExperiments(this);
    }
  }
}
