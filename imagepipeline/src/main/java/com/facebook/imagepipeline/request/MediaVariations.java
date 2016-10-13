/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.util.ArrayList;
import java.util.List;

import android.net.Uri;

import com.facebook.common.internal.Objects;
import com.facebook.imagepipeline.common.ResizeOptions;

/**
 * An optional part of image requests which describes the piece of media being requested.
 *
 * <p> This allows for the provision of a number of image variants which are identical in content
 * but at different resolutions. This way if the image at the source URI isn't already cached an
 * appropriate fallback may be used instead.
 *
 * <p> Even without explicitly including the variants, by using the same media ID across multiple
 * image requests, the pipeline may build up a knowledge of these and fulfil requests accordingly.
 *
 * <p> This will be ignored unless {@link com.facebook.imagepipeline.core.ImagePipelineExperiments#isMediaVariationsEnabled()} is true in the image
 * pipeline config.
 */
@Immutable
public final class MediaVariations {

  private final String mMediaId;
  private final @Nullable Variant mPreferredVariant;
  private final @Nullable List<Variant> mVariants;

  private MediaVariations(Builder builder) {
    mMediaId = builder.mMediaId;
    mPreferredVariant = builder.mPreferredVariant;
    mVariants = builder.mVariants;
  }

  /**
   * Get the unique ID for this piece of media. This must be non-null, not empty and unique for this
   * piece of media (i.e. another request for the same picture at a different size should share the
   * ID but not an unrelated image and not the same media at a different orientation).
   */
  public String getMediaId() {
    return mMediaId;
  }

  /**
   * Get all known variants of this media. This may not be exhaustive and these sets may be
   * combined over time to allow the possibilities of fallbacks being offered even when not
   * specified in a later request.
   */
  public @Nullable List<Variant> getVariants() {
    return mVariants;
  }

  /**
   * Checks whether any variants are contained.
   */
  public boolean hasVariants() {
    return mVariants != null && !mVariants.isEmpty();
  }

  /**
   * Required if the image request lacks {@link com.facebook.imagepipeline.common.ResizeOptions} but
   * otherwise optional as the image pipeline will be able to choose the best size from the variants
   * provided.
   */
  public @Nullable Variant getPreferredVariant() {
    return mPreferredVariant;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MediaVariations)) {
      return false;
    }
    MediaVariations otherVariations = (MediaVariations) o;
    return Objects.equal(mMediaId, otherVariations.mMediaId) &&
        Objects.equal(mPreferredVariant, otherVariations.mPreferredVariant) &&
        Objects.equal(mVariants, otherVariations.mVariants);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mMediaId, mPreferredVariant, mVariants);
  }

  public final static class Variant {

    private final Uri mUri;
    private final int mWidth;
    private final int mHeight;

    public Variant(Uri uri, int width, int height) {
      mUri = uri;
      mWidth = width;
      mHeight = height;
    }

    public Uri getUri() {
      return mUri;
    }

    public int getWidth() {
      return mWidth;
    }

    public int getHeight() {
      return mHeight;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Variant)) {
        return false;
      }
      Variant otherVariant = (Variant) o;
      return Objects.equal(mUri, otherVariant.mUri) &&
          mWidth == otherVariant.mWidth &&
          mHeight == otherVariant.mHeight;
    }

    @Override
    public int hashCode() {
      int hashCode = mUri.hashCode();
      hashCode = 31 * hashCode + mWidth;
      hashCode = 31 * hashCode + mHeight;
      return hashCode;
    }
  }

  /**
   * Creates a builder for a new MediaVariations.
   *
   * @param mediaId the unique ID for this piece of media. This must be non-null and unique for
   *                this piece of media (i.e. another request for the same picture at a different
   *                size should share the ID but not an unrelated image and not the same media at
   *                a different orientation).
   */
  public static Builder newBuilderForMediaId(String mediaId) {
    return new Builder(mediaId);
  }

  public static class Builder {
    private final String mMediaId;
    private Variant mPreferredVariant;
    private List<Variant> mVariants;

    private Builder(String mediaId) {
      mMediaId = mediaId;
    }

    /**
     * Required if the image request lacks {@link com.facebook.imagepipeline.common.ResizeOptions}
     * or if no other variants are added but otherwise optional as the image pipeline will be able
     * to choose the best size from the variants provided.
     *
     * <p><i>This is not currently used. For now requests must include ResizeOptions for the
     * variations to be used.</i>
     */
    public Builder setPreferredVariant(Variant preferredVariant) {
      this.mPreferredVariant = preferredVariant;
      return this;
    }

    public Builder addVariant(Uri uri, int width, int height) {
      if (mVariants == null) {
        mVariants = new ArrayList<>();
      }
      mVariants.add(new Variant(uri, width, height));
      return this;
    }

    public MediaVariations build() {
      return new MediaVariations(this);
    }
  }
}
