/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.backends.okhttp;

import android.content.Context;

import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.squareup.okhttp.OkHttpClient;

/**
 * Factory for getting an {@link com.facebook.imagepipeline.core.ImagePipelineConfig} that uses
 * {@link com.facebook.imagepipeline.backends.okhttp.OkHttpNetworkFetchProducer}.
 */
public class OkHttpImagePipelineConfigFactory {

  public static ImagePipelineConfig.Builder newBuilder(Context context, OkHttpClient okHttpClient) {
    // Before creating OkHttpNetworkFetchProducer we need to configure and get pool factory -
    // OkHttpNetworkFetchProducer requires pooled byte buffer factory and common byte array pool.
    PoolConfig poolConfig = PoolConfig.newBuilder().build();
    PoolFactory poolFactory = new PoolFactory(poolConfig);

    // Create OkHttpNetworkFetchProducer
    OkHttpNetworkFetchProducer producer = new OkHttpNetworkFetchProducer(
        okHttpClient,
        true,
        poolFactory.getPooledByteBufferFactory(),
        poolFactory.getCommonByteArrayPool());

    // Pass OkHttpNetworkFetchProducer and PoolFactory we created above the pipeline configuration
    return ImagePipelineConfig.newBuilder(context)
        .setNetworkFetchProducer(producer)
        .setPoolFactory(poolFactory);
  }
}
