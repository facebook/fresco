/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.core;

import com.facebook.common.internal.Supplier;
import com.facebook.common.webp.WebpBitmapFactory;

/**
 * Encapsulates additional elements of the {@link ImagePipelineConfig} which are currently in an
 * experimental state.
 *
 * <p>These options may often change or disappear altogether and it is not recommended to change
 * their values from their defaults.
 */
public class ImagePipelineExperiments {

  private final int mForceSmallCacheThresholdBytes;
  private final boolean mWebpSupportEnabled;
  private final boolean mDecodeFileDescriptorEnabled;
  private final int mThrottlingMaxSimultaneousRequests;
  private final boolean mExternalCreatedBitmapLogEnabled;
  private final Supplier<Boolean> mMediaVariationsIndexEnabled;
  private final WebpBitmapFactory.WebpErrorLogger mWebpErrorLogger;
  private final boolean mDecodeCancellationEnabled;
  private final WebpBitmapFactory mWebpBitmapFactory;
  private final boolean mSuppressBitmapPrefetching;

  private ImagePipelineExperiments(Builder builder, ImagePipelineConfig.Builder configBuilder) {
    mForceSmallCacheThresholdBytes = builder.mForceSmallCacheThresholdBytes;
    mWebpSupportEnabled = builder.mWebpSupportEnabled;
    mDecodeFileDescriptorEnabled = configBuilder.isDownsampleEnabled() &&
        builder.mDecodeFileDescriptorEnabled;
    mThrottlingMaxSimultaneousRequests = builder.mThrottlingMaxSimultaneousRequests;
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
  }

  public boolean isDecodeFileDescriptorEnabled() {
    return mDecodeFileDescriptorEnabled;
  }

  public boolean isExternalCreatedBitmapLogEnabled() {
    return mExternalCreatedBitmapLogEnabled;
  }

  public int getForceSmallCacheThresholdBytes() {
    return mForceSmallCacheThresholdBytes;
  }

  public boolean getMediaVariationsIndexEnabled() {
    return mMediaVariationsIndexEnabled.get().booleanValue();
  }

  public boolean isWebpSupportEnabled() {
    return mWebpSupportEnabled;
  }

  public boolean isDecodeCancellationEnabled() {
    return mDecodeCancellationEnabled;
  }

  public int getThrottlingMaxSimultaneousRequests() {
    return mThrottlingMaxSimultaneousRequests;
  }

  public WebpBitmapFactory.WebpErrorLogger getWebpErrorLogger() {
    return mWebpErrorLogger;
  }

  public WebpBitmapFactory getWebpBitmapFactory() {
    return mWebpBitmapFactory;
  }

  public static ImagePipelineExperiments.Builder newBuilder(
      ImagePipelineConfig.Builder configBuilder) {
    return new ImagePipelineExperiments.Builder(configBuilder);
  }

  public static class Builder {

    private static final int DEFAULT_MAX_SIMULTANEOUS_FILE_FETCH_AND_RESIZE = 5;

    private final ImagePipelineConfig.Builder mConfigBuilder;
    private int mForceSmallCacheThresholdBytes = 0;
    private boolean mWebpSupportEnabled = false;
    private boolean mDecodeFileDescriptorEnabled = false;
    private boolean mExternalCreatedBitmapLogEnabled = false;
    private int mThrottlingMaxSimultaneousRequests = DEFAULT_MAX_SIMULTANEOUS_FILE_FETCH_AND_RESIZE;
    private Supplier<Boolean> mMediaVariationsIndexEnabled = null;
    private WebpBitmapFactory.WebpErrorLogger mWebpErrorLogger;
    private boolean mDecodeCancellationEnabled = false;
    private WebpBitmapFactory mWebpBitmapFactory;
    private boolean mSuppressBitmapPrefetching = false;

    public Builder(ImagePipelineConfig.Builder configBuilder) {
      mConfigBuilder = configBuilder;
    }

    public ImagePipelineConfig.Builder setDecodeFileDescriptorEnabled(
        boolean decodeFileDescriptorEnabled) {
      mDecodeFileDescriptorEnabled = decodeFileDescriptorEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setExternalCreatedBitmapLogEnabled(
        boolean externalCreatedBitmapLogEnabled) {
      mExternalCreatedBitmapLogEnabled = externalCreatedBitmapLogEnabled;
      return mConfigBuilder;
    }

    /**
     * If this value is nonnegative, then all network-downloaded images below this size will be
     * written to the small image cache.
     *
     * <p>This will require the image pipeline to do up to two disk reads, instead of one, before
     * going out to network. Use only if this pattern makes sense for your application.
     */
    public ImagePipelineConfig.Builder setForceSmallCacheThresholdBytes(
        int forceSmallCacheThresholdBytes) {
      mForceSmallCacheThresholdBytes = forceSmallCacheThresholdBytes;
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
     * Using this method is possible to change the max number of threads for loading and sizing
     * local images
     * @param throttlingMaxSimultaneousRequests Max number of thread
     * @return The Builder itself for chaining
     */
    public ImagePipelineConfig.Builder setThrottlingMaxSimultaneousRequests(
        int throttlingMaxSimultaneousRequests) {
      mThrottlingMaxSimultaneousRequests = throttlingMaxSimultaneousRequests;
      return mConfigBuilder;
    }

    public ImagePipelineExperiments build() {
      return new ImagePipelineExperiments(this, mConfigBuilder);
    }
  }
}
