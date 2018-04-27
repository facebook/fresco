/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import com.facebook.imagepipeline.producers.BitmapMemoryCacheGetProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.DiskCacheReadProducer;
import com.facebook.imagepipeline.producers.EncodedMemoryCacheProducer;
import com.facebook.imagepipeline.producers.MediaVariationsFallbackProducer;
import com.facebook.imagepipeline.producers.NetworkFetchProducer;

public class ImageOriginUtils {

  public static String toString(@ImageOrigin int imageOrigin) {
    switch (imageOrigin) {
      case ImageOrigin.NETWORK:
        return "network";
      case ImageOrigin.DISK:
        return "disk";
      case ImageOrigin.MEMORY_BITMAP:
        return "memory_bitmap";
      case ImageOrigin.MEMORY_ENCODED:
        return "memory_encoded";
      default:
        return "unknown";
    }
  }

  public static @ImageOrigin int mapProducerNameToImageOrigin(final String producerName) {
    switch (producerName) {
      case BitmapMemoryCacheGetProducer.PRODUCER_NAME:
      case BitmapMemoryCacheProducer.PRODUCER_NAME:
        return ImageOrigin.MEMORY_BITMAP;
      case EncodedMemoryCacheProducer.PRODUCER_NAME:
        return ImageOrigin.MEMORY_ENCODED;
      case DiskCacheReadProducer.PRODUCER_NAME:
        return ImageOrigin.DISK;
      case MediaVariationsFallbackProducer.PRODUCER_NAME:
        return ImageOrigin.MEMORY_BITMAP;
      case NetworkFetchProducer.PRODUCER_NAME:
        return ImageOrigin.NETWORK;
      default:
        return ImageOrigin.UNKNOWN;
    }
  }

  private ImageOriginUtils() {}
}
