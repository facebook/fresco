/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request;

import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_DATA;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_ASSET;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_CONTENT;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_RESOURCE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_NETWORK;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_QUALIFIED_RESOURCE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_UNKNOWN;

import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Fn;
import com.facebook.common.internal.Objects;
import com.facebook.common.media.MediaUtils;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.common.SourceUriType;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imageutils.BitmapUtil;
import java.io.File;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Immutable object encapsulating everything pipeline has to know about requested image to proceed.
 */
@Immutable
public class ImageRequest {

  /** Cache choice */
  private final CacheChoice mCacheChoice;

  /** Source Uri */
  private final Uri mSourceUri;

  private final @SourceUriType int mSourceUriType;

  /** Source File - for local fetches only, lazily initialized */
  private File mSourceFile;

  /** If set - the client will receive intermediate results */
  private final boolean mProgressiveRenderingEnabled;

  /** If set the client will receive thumbnail previews for local images, before the whole image */
  private final boolean mLocalThumbnailPreviewsEnabled;

  private final ImageDecodeOptions mImageDecodeOptions;

  /** resize options */
  private final @Nullable ResizeOptions mResizeOptions;

  /** rotation options */
  private final RotationOptions mRotationOptions;

  /** Range of bytes to request from the network */
  private final @Nullable BytesRange mBytesRange;

  /** Priority levels of this request. */
  private final Priority mRequestPriority;

  /** Lowest level that is permitted to fetch an image from */
  private final RequestLevel mLowestPermittedRequestLevel;

  /** Whether the disk cache should be used for this request */
  private final boolean mIsDiskCacheEnabled;

  /** Whether the memory cache should be used for this request */
  private final boolean mIsMemoryCacheEnabled;

  /**
   * Whether to decode prefetched images. true -> Cache both encoded image and bitmap. false ->
   * Cache only encoded image and do not decode until image is needed to be shown. null -> Use
   * pipeline's default
   */
  private final @Nullable Boolean mDecodePrefetches;

  /** Postprocessor to run on the output bitmap. */
  private final @Nullable Postprocessor mPostprocessor;

  /** Request listener to use for this image request */
  private final @Nullable RequestListener mRequestListener;

  /**
   * Controls whether resizing is allowed for this request. true -> allow for this request. false ->
   * disallow for this request. null -> use default pipeline's setting.
   */
  private final @Nullable Boolean mResizingAllowedOverride;

  public static @Nullable ImageRequest fromFile(@Nullable File file) {
    return (file == null) ? null : ImageRequest.fromUri(UriUtil.getUriForFile(file));
  }

  public static @Nullable ImageRequest fromUri(@Nullable Uri uri) {
    return (uri == null) ? null : ImageRequestBuilder.newBuilderWithSource(uri).build();
  }

  public static @Nullable ImageRequest fromUri(@Nullable String uriString) {
    return (uriString == null || uriString.length() == 0) ? null : fromUri(Uri.parse(uriString));
  }

  protected ImageRequest(ImageRequestBuilder builder) {
    mCacheChoice = builder.getCacheChoice();
    mSourceUri = builder.getSourceUri();
    mSourceUriType = getSourceUriType(mSourceUri);

    mProgressiveRenderingEnabled = builder.isProgressiveRenderingEnabled();
    mLocalThumbnailPreviewsEnabled = builder.isLocalThumbnailPreviewsEnabled();

    mImageDecodeOptions = builder.getImageDecodeOptions();

    mResizeOptions = builder.getResizeOptions();
    mRotationOptions =
        builder.getRotationOptions() == null
            ? RotationOptions.autoRotate()
            : builder.getRotationOptions();
    mBytesRange = builder.getBytesRange();

    mRequestPriority = builder.getRequestPriority();
    mLowestPermittedRequestLevel = builder.getLowestPermittedRequestLevel();
    mIsDiskCacheEnabled = builder.isDiskCacheEnabled();
    mIsMemoryCacheEnabled = builder.isMemoryCacheEnabled();
    mDecodePrefetches = builder.shouldDecodePrefetches();

    mPostprocessor = builder.getPostprocessor();

    mRequestListener = builder.getRequestListener();

    mResizingAllowedOverride = builder.getResizingAllowedOverride();
  }

  public CacheChoice getCacheChoice() {
    return mCacheChoice;
  }

  public Uri getSourceUri() {
    return mSourceUri;
  }

  public @SourceUriType int getSourceUriType() {
    return mSourceUriType;
  }

  public int getPreferredWidth() {
    return (mResizeOptions != null) ? mResizeOptions.width : (int) BitmapUtil.MAX_BITMAP_SIZE;
  }

  public int getPreferredHeight() {
    return (mResizeOptions != null) ? mResizeOptions.height : (int) BitmapUtil.MAX_BITMAP_SIZE;
  }

  public @Nullable ResizeOptions getResizeOptions() {
    return mResizeOptions;
  }

  public RotationOptions getRotationOptions() {
    return mRotationOptions;
  }

  /** @deprecated Use {@link #getRotationOptions()} */
  @Deprecated
  public boolean getAutoRotateEnabled() {
    return mRotationOptions.useImageMetadata();
  }

  @Nullable
  public BytesRange getBytesRange() {
    return mBytesRange;
  }

  public ImageDecodeOptions getImageDecodeOptions() {
    return mImageDecodeOptions;
  }

  public boolean getProgressiveRenderingEnabled() {
    return mProgressiveRenderingEnabled;
  }

  public boolean getLocalThumbnailPreviewsEnabled() {
    return mLocalThumbnailPreviewsEnabled;
  }

  public Priority getPriority() {
    return mRequestPriority;
  }

  public RequestLevel getLowestPermittedRequestLevel() {
    return mLowestPermittedRequestLevel;
  }

  public boolean isDiskCacheEnabled() {
    return mIsDiskCacheEnabled;
  }

  public boolean isMemoryCacheEnabled() {
    return mIsMemoryCacheEnabled;
  }

  public @Nullable Boolean shouldDecodePrefetches() {
    return mDecodePrefetches;
  }

  public @Nullable Boolean getResizingAllowedOverride() {
    return mResizingAllowedOverride;
  }

  public synchronized File getSourceFile() {
    if (mSourceFile == null) {
      mSourceFile = new File(mSourceUri.getPath());
    }
    return mSourceFile;
  }

  public @Nullable Postprocessor getPostprocessor() {
    return mPostprocessor;
  }

  public @Nullable RequestListener getRequestListener() {
    return mRequestListener;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ImageRequest)) {
      return false;
    }
    ImageRequest request = (ImageRequest) o;
    if (mLocalThumbnailPreviewsEnabled != request.mLocalThumbnailPreviewsEnabled) return false;
    if (mIsDiskCacheEnabled != request.mIsDiskCacheEnabled) return false;
    if (mIsMemoryCacheEnabled != request.mIsMemoryCacheEnabled) return false;
    if (!Objects.equal(mSourceUri, request.mSourceUri)
        || !Objects.equal(mCacheChoice, request.mCacheChoice)
        || !Objects.equal(mSourceFile, request.mSourceFile)
        || !Objects.equal(mBytesRange, request.mBytesRange)
        || !Objects.equal(mImageDecodeOptions, request.mImageDecodeOptions)
        || !Objects.equal(mResizeOptions, request.mResizeOptions)
        || !Objects.equal(mRequestPriority, request.mRequestPriority)
        || !Objects.equal(mLowestPermittedRequestLevel, request.mLowestPermittedRequestLevel)
        || !Objects.equal(mDecodePrefetches, request.mDecodePrefetches)
        || !Objects.equal(mResizingAllowedOverride, request.mResizingAllowedOverride)
        || !Objects.equal(mRotationOptions, request.mRotationOptions)) {
      return false;
    }
    final CacheKey thisPostprocessorKey =
        mPostprocessor != null ? mPostprocessor.getPostprocessorCacheKey() : null;
    final CacheKey thatPostprocessorKey =
        request.mPostprocessor != null ? request.mPostprocessor.getPostprocessorCacheKey() : null;
    return Objects.equal(thisPostprocessorKey, thatPostprocessorKey);
  }

  @Override
  public int hashCode() {
    final CacheKey postprocessorCacheKey =
        mPostprocessor != null ? mPostprocessor.getPostprocessorCacheKey() : null;
    return Objects.hashCode(
        mCacheChoice,
        mSourceUri,
        mLocalThumbnailPreviewsEnabled,
        mBytesRange,
        mRequestPriority,
        mLowestPermittedRequestLevel,
        mIsDiskCacheEnabled,
        mIsMemoryCacheEnabled,
        mImageDecodeOptions,
        mDecodePrefetches,
        mResizeOptions,
        mRotationOptions,
        postprocessorCacheKey,
        mResizingAllowedOverride);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("uri", mSourceUri)
        .add("cacheChoice", mCacheChoice)
        .add("decodeOptions", mImageDecodeOptions)
        .add("postprocessor", mPostprocessor)
        .add("priority", mRequestPriority)
        .add("resizeOptions", mResizeOptions)
        .add("rotationOptions", mRotationOptions)
        .add("bytesRange", mBytesRange)
        .add("resizingAllowedOverride", mResizingAllowedOverride)
        .add("progressiveRenderingEnabled", mProgressiveRenderingEnabled)
        .add("localThumbnailPreviewsEnabled", mLocalThumbnailPreviewsEnabled)
        .add("lowestPermittedRequestLevel", mLowestPermittedRequestLevel)
        .add("isDiskCacheEnabled", mIsDiskCacheEnabled)
        .add("isMemoryCacheEnabled", mIsMemoryCacheEnabled)
        .add("decodePrefetches", mDecodePrefetches)
        .toString();
  }

  /** An enum describing the cache choice. */
  public enum CacheChoice {

    /* Indicates that this image should go in the small disk cache, if one is being used */
    SMALL,

    /* Default */
    DEFAULT
  }

  /**
   * Level down to we are willing to go in order to find an image. E.g., we might only want to go
   * down to bitmap memory cache, and not check the disk cache or do a full fetch.
   */
  public enum RequestLevel {
    /* Fetch (from the network or local storage) */
    FULL_FETCH(1),

    /* Disk caching */
    DISK_CACHE(2),

    /* Encoded memory caching */
    ENCODED_MEMORY_CACHE(3),

    /* Bitmap caching */
    BITMAP_MEMORY_CACHE(4);

    private int mValue;

    private RequestLevel(int value) {
      mValue = value;
    }

    public int getValue() {
      return mValue;
    }

    public static RequestLevel getMax(RequestLevel requestLevel1, RequestLevel requestLevel2) {
      return requestLevel1.getValue() > requestLevel2.getValue() ? requestLevel1 : requestLevel2;
    }
  }

  /**
   * This is a utility method which returns the type of Uri
   *
   * @param uri The Uri to test
   * @return The type of the given Uri if available or SOURCE_TYPE_UNKNOWN if not
   */
  private static @SourceUriType int getSourceUriType(final Uri uri) {
    if (uri == null) {
      return SOURCE_TYPE_UNKNOWN;
    }
    if (UriUtil.isNetworkUri(uri)) {
      return SOURCE_TYPE_NETWORK;
    } else if (UriUtil.isLocalFileUri(uri)) {
      if (MediaUtils.isVideo(MediaUtils.extractMime(uri.getPath()))) {
        return SOURCE_TYPE_LOCAL_VIDEO_FILE;
      } else {
        return SOURCE_TYPE_LOCAL_IMAGE_FILE;
      }
    } else if (UriUtil.isLocalContentUri(uri)) {
      return SOURCE_TYPE_LOCAL_CONTENT;
    } else if (UriUtil.isLocalAssetUri(uri)) {
      return SOURCE_TYPE_LOCAL_ASSET;
    } else if (UriUtil.isLocalResourceUri(uri)) {
      return SOURCE_TYPE_LOCAL_RESOURCE;
    } else if (UriUtil.isDataUri(uri)) {
      return SOURCE_TYPE_DATA;
    } else if (UriUtil.isQualifiedResourceUri(uri)) {
      return SOURCE_TYPE_QUALIFIED_RESOURCE;
    } else {
      return SOURCE_TYPE_UNKNOWN;
    }
  }

  public static final Fn<ImageRequest, Uri> REQUEST_TO_URI_FN =
      new Fn<ImageRequest, Uri>() {
        @Override
        public @Nullable Uri apply(@Nullable ImageRequest arg) {
          return arg != null ? arg.getSourceUri() : null;
        }
      };
}
