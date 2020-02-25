/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Objects;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.multiuri.MultiUri;
import com.facebook.imagepipeline.request.ImageRequest;
import javax.annotation.Nullable;

public class VitoImageRequest {

  public final Resources resources;
  public final Uri uri;
  public final MultiUri multiUri;
  public final ImageOptions imageOptions;
  public final ImageRequest imageRequest;
  public final CacheKey cacheKey;

  public VitoImageRequest(
      Resources resources,
      Uri uri,
      MultiUri multiUri,
      ImageOptions imageOptions,
      ImageRequest imageRequest,
      CacheKey cacheKey) {
    this.resources = resources;
    this.uri = uri;
    this.multiUri = multiUri;
    this.imageOptions = imageOptions;
    this.imageRequest = imageRequest;
    this.cacheKey = cacheKey;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) return false;
    VitoImageRequest other = (VitoImageRequest) obj;
    return resources == other.resources
        && Objects.equal(uri, other.uri)
        && Objects.equal(multiUri, other.multiUri)
        && Objects.equal(imageOptions, other.imageOptions)
        && Objects.equal(imageRequest, other.imageRequest)
        && Objects.equal(cacheKey, other.cacheKey);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (resources != null ? resources.hashCode() : 0);
    result = 31 * result + (uri != null ? uri.hashCode() : 0);
    result = 31 * result + (multiUri != null ? multiUri.hashCode() : 0);
    result = 31 * result + (imageOptions != null ? imageOptions.hashCode() : 0);
    result = 31 * result + (imageRequest != null ? imageRequest.hashCode() : 0);
    result = 31 * result + (cacheKey != null ? cacheKey.hashCode() : 0);
    return result;
  }
}
