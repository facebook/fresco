/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.core;

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
  private boolean mDecodeFileDescriptorEnabled;
  private final int mThrottlingMaxSimultaneousRequests;

  private ImagePipelineExperiments(Builder builder, ImagePipelineConfig.Builder configBuilder) {
    mForceSmallCacheThresholdBytes = builder.mForceSmallCacheThresholdBytes;
    mWebpSupportEnabled = builder.mWebpSupportEnabled && sWebpLibraryPresent;
    mDecodeFileDescriptorEnabled = configBuilder.isDownsampleEnabled() &&
        builder.mDecodeFileDescriptorEnabled;
    mThrottlingMaxSimultaneousRequests = builder.mThrottlingMaxSimultaneousRequests;
  }

  public boolean isDecodeFileDescriptorEnabled() {
    return mDecodeFileDescriptorEnabled;
  }

  public int getForceSmallCacheThresholdBytes() {
    return mForceSmallCacheThresholdBytes;
  }

  public boolean isWebpSupportEnabled() {
    return mWebpSupportEnabled;
  }

  public int getThrottlingMaxSimultaneousRequests() {
    return mThrottlingMaxSimultaneousRequests;
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
    private int mThrottlingMaxSimultaneousRequests = DEFAULT_MAX_SIMULTANEOUS_FILE_FETCH_AND_RESIZE;

    public Builder(ImagePipelineConfig.Builder configBuilder) {
      mConfigBuilder = configBuilder;
    }

    public ImagePipelineConfig.Builder setDecodeFileDescriptorEnabled(
        boolean decodeFileDescriptorEnabled) {
      mDecodeFileDescriptorEnabled = decodeFileDescriptorEnabled;
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
