/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options;

import com.facebook.common.internal.Objects;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class EncodedImageOptions {

  public static Builder create() {
    return new Builder(ImageOptions.defaults());
  }

  private final @Nullable Priority mPriority;
  private final @Nullable ImageRequest.CacheChoice mCacheChoice;

  public EncodedImageOptions(Builder builder) {
    mPriority = builder.mPriority;
    mCacheChoice = builder.mCacheChoice;
  }

  public @Nullable Priority getPriority() {
    return mPriority;
  }

  public @Nullable ImageRequest.CacheChoice getCacheChoice() {
    return mCacheChoice;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) return false;
    EncodedImageOptions other = (EncodedImageOptions) obj;
    return equalEncodedOptions(other);
  }

  protected boolean equalEncodedOptions(EncodedImageOptions other) {
    if (!Objects.equal(mPriority, other.mPriority)
        || !Objects.equal(mCacheChoice, other.mCacheChoice)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = mPriority != null ? mPriority.hashCode() : 0;
    return 31 * result + (mCacheChoice != null ? mCacheChoice.hashCode() : 0);
  }

  @Override
  public String toString() {
    return toStringHelper().toString();
  }

  protected Objects.ToStringHelper toStringHelper() {
    return Objects.toStringHelper(this).add("priority", mPriority).add("cacheChoice", mCacheChoice);
  }

  public static class Builder<T extends Builder> {

    private @Nullable Priority mPriority;
    private @Nullable ImageRequest.CacheChoice mCacheChoice;

    protected Builder() {}

    protected Builder(ImageOptions defaultOptions) {
      mPriority = defaultOptions.getPriority();
      mCacheChoice = defaultOptions.getCacheChoice();
    }

    public T priority(@Nullable Priority priority) {
      mPriority = priority;
      return getThis();
    }

    public T cacheChoice(@Nullable ImageRequest.CacheChoice cacheChoice) {
      mCacheChoice = cacheChoice;
      return getThis();
    }

    public EncodedImageOptions build() {
      return new EncodedImageOptions(this);
    }

    @SuppressWarnings("unchecked")
    protected T getThis() {
      return (T) this;
    }
  }
}
