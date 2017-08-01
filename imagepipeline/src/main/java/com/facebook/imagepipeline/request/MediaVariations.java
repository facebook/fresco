/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.request;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.net.Uri;
import android.support.annotation.StringDef;
import com.facebook.common.internal.Objects;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

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
 * <p> This extra functionality is currently only enabled if
 * {@link com.facebook.imagepipeline.core.ImagePipelineExperiments#getMediaVariationsIndexEnabled()}
 * is true in the image pipeline config.
 */
@Immutable
public class MediaVariations {

  /**
   * Defines the range of valid source values to be held by an instance. These are then used in
   * logging events.
   */
  @Retention(SOURCE)
  @StringDef({
      SOURCE_IMAGE_REQUEST,
      SOURCE_INDEX_DB,
      SOURCE_ID_EXTRACTOR,
  })
  public @interface Source {
  }

  public static final String SOURCE_IMAGE_REQUEST = "request";
  public static final String SOURCE_INDEX_DB = "index_db";
  public static final String SOURCE_ID_EXTRACTOR = "id_extractor";

  private final String mMediaId;
  private final @Nullable List<Variant> mVariants;
  private final boolean mForceRequestForSpecifiedUri;
  private final @Source String mSource;

  private MediaVariations(Builder builder) {
    mMediaId = builder.mMediaId;
    mVariants = builder.mVariants;
    mForceRequestForSpecifiedUri = builder.mForceRequestForSpecifiedUri;
    mSource = builder.mSource;
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
   * Get the number of contained variants. This may not be an exhaustive set of variants and these
   * sets may be combined over time to allow the possibilities of fallbacks being offered even when
   * not specified in a later request.
   *
   * @return the number of variants
   */
  public int getVariantsCount() {
    return mVariants == null ? 0 : mVariants.size();
  }

  /**
   * Gets the variant at the specified index.
   *
   * @param index index of the element to return
   * @return the element at the specified position in this list
   * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;=
   * getVariantsCount()</tt>)
   * @throws NullPointerException if there are no variants
   */
  public Variant getVariant(int index) {
    return mVariants.get(index);
  }

  /**
   * Gets a list of the variants in an order determined by the provided comparator.
   */
  public List<MediaVariations.Variant> getSortedVariants(Comparator<Variant> comparator) {
    int variantsCount = getVariantsCount();
    if (variantsCount == 0) {
      return Collections.emptyList();
    }

    List<MediaVariations.Variant> variants = new ArrayList<>(variantsCount);
    for (int i = 0; i < variantsCount; i++) {
      variants.add(mVariants.get(i));
    }

    Collections.sort(variants, comparator);

    return variants;
  }

  /**
   * Gets whether the URI in the original request should be downloaded even if another cached image
   * appears to be big enough for the request.
   *
   * @return true if any found image should only be used as a placeholder, whatever the size
   */
  public boolean shouldForceRequestForSpecifiedUri() {
    return mForceRequestForSpecifiedUri;
  }

  /**
   * Get the source of these variations, for the purposes of logging.
   * @see Source
   */
  @Source
  public String getSource() {
    return mSource;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MediaVariations)) {
      return false;
    }
    MediaVariations otherVariations = (MediaVariations) o;
    return Objects.equal(mMediaId, otherVariations.mMediaId) &&
        mForceRequestForSpecifiedUri == otherVariations.mForceRequestForSpecifiedUri &&
        Objects.equal(mVariants, otherVariations.mVariants);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mMediaId, mForceRequestForSpecifiedUri, mVariants, mSource);
  }

  @Override
  public String toString() {
    return String.format(
        (Locale) null,
        "%s-%b-%s-%s",
        mMediaId,
        mForceRequestForSpecifiedUri,
        mVariants,
        mSource);
  }

  public final static class Variant {

    private final Uri mUri;
    private final int mWidth;
    private final int mHeight;
    @Nullable private final ImageRequest.CacheChoice mCacheChoice;

    public Variant(Uri uri, int width, int height) {
      this(uri, width, height, null);
    }

    public Variant(Uri uri, int width, int height, @Nullable ImageRequest.CacheChoice cacheChoice) {
      mUri = uri;
      mWidth = width;
      mHeight = height;
      mCacheChoice = cacheChoice;
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

    @Nullable
    public ImageRequest.CacheChoice getCacheChoice() {
      return mCacheChoice;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Variant)) {
        return false;
      }
      Variant otherVariant = (Variant) o;
      return Objects.equal(mUri, otherVariant.mUri) &&
          mWidth == otherVariant.mWidth &&
          mHeight == otherVariant.mHeight &&
          mCacheChoice == otherVariant.mCacheChoice;
    }

    @Override
    public int hashCode() {
      int hashCode = mUri.hashCode();
      hashCode = 31 * hashCode + mWidth;
      hashCode = 31 * hashCode + mHeight;
      return hashCode;
    }

    @Override
    public String toString() {
      return String.format((Locale) null, "%dx%d %s %s", mWidth, mHeight, mUri, mCacheChoice);
    }
  }

  /**
   * Creates an instance with a media ID and without specific variants. If a null or empty ID is
   * given, null is returned.
   *
   * @param mediaId the unique ID for this piece of media. If this is neither null or empty, it must
   *                be unique for this piece of media (i.e. another request for the same picture at
   *                a different size should share the ID but not an unrelated image and not the same
   *                media at a different orientation).
   */
  public static @Nullable MediaVariations forMediaId(@Nullable String mediaId) {
    if (mediaId == null || mediaId.isEmpty()) {
      return null;
    }
    return newBuilderForMediaId(mediaId).build();
  }

  /**
   * Creates a builder for a new MediaVariations to which you can add specific variants.
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
    private List<Variant> mVariants;
    private boolean mForceRequestForSpecifiedUri = false;
    private @Source String mSource = SOURCE_IMAGE_REQUEST;

    private Builder(String mediaId) {
      mMediaId = mediaId;
    }

    public Builder addVariant(Uri uri, int width, int height) {
      return addVariant(uri, width, height, null);
    }

    public Builder addVariant(
        Uri uri,
        int width,
        int height,
        ImageRequest.CacheChoice cacheChoice) {
      if (mVariants == null) {
        mVariants = new ArrayList<>();
      }
      mVariants.add(new Variant(uri, width, height, cacheChoice));
      return this;
    }

    /**
     * Sets whether the URI specified in the image request should be downloaded, even if another
     * cached image appears to be big enough for the request. This may be useful for example if the
     * original image should be saved in encoded format but the original size is unknown.
     *
     * <p>In this case, whatever other image is found may still be returned as a non-final
     * placeholder.
     */
    public Builder setForceRequestForSpecifiedUri(boolean forceRequestForSpecifiedUri) {
      mForceRequestForSpecifiedUri = forceRequestForSpecifiedUri;
      return this;
    }

    /**
     * Sets the source of these variations.
     *
     * <p> It is not intended that uses of Fresco will set this manually. It's intended to be set
     * within the library so that logging events can differentiate between variations defined in a
     * request from those using the index database.
     *
     * @see MediaVariations.Source
     */
    public Builder setSource(@Source String source) {
      mSource = source;
      return this;
    }

    public MediaVariations build() {
      return new MediaVariations(this);
    }
  }
}
