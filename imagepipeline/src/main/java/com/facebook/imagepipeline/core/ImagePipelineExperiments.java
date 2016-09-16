/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.core;

import com.facebook.common.webp.WebpBitmapFactory;
import com.facebook.imagepipeline.producers.WebpTranscodeProducer;

import static com.facebook.common.webp.WebpSupportStatus.sWebpLibraryPresent;

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
  private final @WebpTranscodeProducer.EnhancedTranscodingType int mEnhancedWebpTranscodingType;
  private boolean mDecodeFileDescriptorEnabled;
  private final int mThrottlingMaxSimultaneousRequests;
  private final boolean mExternalCreatedBitmapLogEnabled;
  private final WebpBitmapFactory.WebpErrorLogger mWebpErrorLogger;

  private ImagePipelineExperiments(Builder builder, ImagePipelineConfig.Builder configBuilder) {
    mForceSmallCacheThresholdBytes = builder.mForceSmallCacheThresholdBytes;
    mWebpSupportEnabled = builder.mWebpSupportEnabled && sWebpLibraryPresent;
    mDecodeFileDescriptorEnabled = configBuilder.isDownsampleEnabled() &&
        builder.mDecodeFileDescriptorEnabled;
    mThrottlingMaxSimultaneousRequests = builder.mThrottlingMaxSimultaneousRequests;
    mExternalCreatedBitmapLogEnabled = builder.mExternalCreatedBitmapLogEnabled;
    mWebpErrorLogger = builder.mWebpErrorLogger;
    mEnhancedWebpTranscodingType = builder.mEnhancedWebpTranscodingType;
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

  public boolean isWebpSupportEnabled() {
    return mWebpSupportEnabled;
  }

  public @WebpTranscodeProducer.EnhancedTranscodingType int getEnhancedWebpTranscodingType() {
    return mEnhancedWebpTranscodingType;
  }

  public int getThrottlingMaxSimultaneousRequests() {
    return mThrottlingMaxSimultaneousRequests;
  }

  public WebpBitmapFactory.WebpErrorLogger getWebpErrorLogger() {
    return mWebpErrorLogger;
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
    private @WebpTranscodeProducer.EnhancedTranscodingType int mEnhancedWebpTranscodingType;
    private boolean mDecodeFileDescriptorEnabled = false;
    private boolean mExternalCreatedBitmapLogEnabled = false;
    private int mThrottlingMaxSimultaneousRequests = DEFAULT_MAX_SIMULTANEOUS_FILE_FETCH_AND_RESIZE;
    private WebpBitmapFactory.WebpErrorLogger mWebpErrorLogger;

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

    public ImagePipelineConfig.Builder setWebpSupportEnabled(boolean webpSupportEnabled) {
      mWebpSupportEnabled = webpSupportEnabled;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setWebpErrorLogger(
        WebpBitmapFactory.WebpErrorLogger webpErrorLogger) {
      mWebpErrorLogger = webpErrorLogger;
      return mConfigBuilder;
    }

    public ImagePipelineConfig.Builder setEnhancedWebpTranscodingType(
        @WebpTranscodeProducer.EnhancedTranscodingType int enhancedWebpTranscodingType) {
      mEnhancedWebpTranscodingType = enhancedWebpTranscodingType;
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
