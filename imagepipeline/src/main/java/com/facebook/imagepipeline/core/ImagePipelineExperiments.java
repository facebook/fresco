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

  private ImagePipelineExperiments(Builder builder, ImagePipelineConfig.Builder configBuilder) {
    mForceSmallCacheThresholdBytes = builder.mForceSmallCacheThresholdBytes;
    mWebpSupportEnabled = builder.mWebpSupportEnabled && sWebpLibraryPresent;
    mDecodeFileDescriptorEnabled = configBuilder.isDownsampleEnabled() &&
        builder.mDecodeFileDescriptorEnabled;
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

  public static ImagePipelineExperiments.Builder newBuilder(
      ImagePipelineConfig.Builder configBuilder) {
    return new ImagePipelineExperiments.Builder(configBuilder);
  }

  public static class Builder {

    private final ImagePipelineConfig.Builder mConfigBuilder;
    private int mForceSmallCacheThresholdBytes = 0;
    private boolean mWebpSupportEnabled = false;
    private boolean mDecodeFileDescriptorEnabled = false;

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

    public ImagePipelineExperiments build() {
      return new ImagePipelineExperiments(this, mConfigBuilder);
    }
  }
}
