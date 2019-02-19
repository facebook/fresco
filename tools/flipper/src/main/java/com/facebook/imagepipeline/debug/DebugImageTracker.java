/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug;

import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.request.ImageRequest;

/** Image tracker that tracks additional information, such as the original URI. */
public interface DebugImageTracker {

  void trackImage(ImageRequest imageRequest, CacheKey cacheKey);

  void trackImageRequest(ImageRequest imageRequest, String requestId);
}
