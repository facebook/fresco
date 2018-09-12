/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import com.facebook.imagepipeline.producers.BitmapMemoryCacheGetProducer;
import com.facebook.imagepipeline.producers.BitmapMemoryCacheProducer;
import com.facebook.imagepipeline.producers.DataFetchProducer;
import com.facebook.imagepipeline.producers.DiskCacheReadProducer;
import com.facebook.imagepipeline.producers.EncodedMemoryCacheProducer;
import com.facebook.imagepipeline.producers.LocalAssetFetchProducer;
import com.facebook.imagepipeline.producers.LocalContentUriFetchProducer;
import com.facebook.imagepipeline.producers.LocalContentUriThumbnailFetchProducer;
import com.facebook.imagepipeline.producers.LocalFileFetchProducer;
import com.facebook.imagepipeline.producers.LocalResourceFetchProducer;
import com.facebook.imagepipeline.producers.NetworkFetchProducer;

public class ImageOriginUtils {

  public static String toString(@ImageOrigin int imageOrigin) {
    switch (imageOrigin) {
      case ImageOrigin.NETWORK:
        return "network";
      case ImageOrigin.DISK:
        return "disk";
      case ImageOrigin.MEMORY_ENCODED:
        return "memory_encoded";
      case ImageOrigin.MEMORY_BITMAP:
        return "memory_bitmap";
      case ImageOrigin.LOCAL:
        return "local";
      case ImageOrigin.UNKNOWN:
        // fall through
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
      case NetworkFetchProducer.PRODUCER_NAME:
        return ImageOrigin.NETWORK;

      case DataFetchProducer.PRODUCER_NAME:
      case LocalFileFetchProducer.PRODUCER_NAME:
      case LocalResourceFetchProducer.PRODUCER_NAME:
      case LocalAssetFetchProducer.PRODUCER_NAME:
      case LocalContentUriFetchProducer.PRODUCER_NAME:
      case LocalContentUriThumbnailFetchProducer.PRODUCER_NAME:
        return ImageOrigin.LOCAL;

      default:
        return ImageOrigin.UNKNOWN;
    }
  }

  private ImageOriginUtils() {}
}
