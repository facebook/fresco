/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request;

import static com.facebook.imagepipeline.request.ImageRequest.CacheChoice;
import static com.facebook.imagepipeline.request.ImageRequest.RequestLevel;

import android.net.Uri;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineExperiments;
import com.facebook.imagepipeline.listener.RequestListener;
import javax.annotation.Nullable;

/**
 * Builder class for {@link ImageRequest}s.
 */
public class ImageRequestBuilder {

  private Uri mSourceUri = null;
  private RequestLevel mLowestPermittedRequestLevel = RequestLevel.FULL_FETCH;
  private @Nullable ResizeOptions mResizeOptions = null;
  private @Nullable RotationOptions mRotationOptions = null;
  private ImageDecodeOptions mImageDecodeOptions = ImageDecodeOptions.defaults();
  private CacheChoice mCacheChoice = CacheChoice.DEFAULT;
  private boolean mProgressiveRenderingEnabled =
      ImagePipelineConfig.getDefaultImageRequestConfig().isProgressiveRenderingEnabled();
  private boolean mLocalThumbnailPreviewsEnabled = false;
  private Priority mRequestPriority = Priority.HIGH;
  private @Nullable Postprocessor mPostprocessor = null;
  private boolean mDiskCacheEnabled = true;
  private boolean mMemoryCacheEnabled = true;
  private @Nullable Boolean mDecodePrefetches = null;
  private @Nullable RequestListener mRequestListener;
  private @Nullable BytesRange mBytesRange = null;
  private @Nullable Boolean mResizingAllowedOverride = null;

  /**
   * Creates a new request builder instance. The setting will be done according to the source type.
   * @param uri the uri to fetch
   * @return a new request builder instance
   */
  public static ImageRequestBuilder newBuilderWithSource(Uri uri) {
    return new ImageRequestBuilder().setSource(uri);
  }

  /**
   * Creates a new request builder instance for a local resource image.
   *
   * <p>Only image resources can be used with the image pipeline (PNG, JPG, GIF). Other resource
   * types such as Strings or XML Drawables make no sense in the context of the image pipeline and
   * so cannot be supported. Attempts to do so will throw an
   * {@link java.lang.IllegalArgumentException} when the pipeline tries to decode the resource.
   *
   * <p>One potentially confusing case is drawable declared in XML (e.g. ShapeDrawable). This is not
   * an image. If you want to display an XML drawable as the main image, then set it as a
   * placeholder and do not set a URI.
   * <p/>
   *
   * @param resId local image resource id.
   * @return a new request builder instance.
   */
  public static ImageRequestBuilder newBuilderWithResourceId(int resId) {
    return newBuilderWithSource(UriUtil.getUriForResourceId(resId));
  }

  /**
   * Creates a new request builder instance with the same parameters as the imageRequest passed in.
   *
   * @param imageRequest the ImageRequest from where to copy the parameters to the builder.
   * @return a new request builder instance
   */
  public static ImageRequestBuilder fromRequest(ImageRequest imageRequest) {
    return ImageRequestBuilder.newBuilderWithSource(imageRequest.getSourceUri())
        .setImageDecodeOptions(imageRequest.getImageDecodeOptions())
        .setBytesRange(imageRequest.getBytesRange())
        .setCacheChoice(imageRequest.getCacheChoice())
        .setLocalThumbnailPreviewsEnabled(imageRequest.getLocalThumbnailPreviewsEnabled())
        .setLowestPermittedRequestLevel(imageRequest.getLowestPermittedRequestLevel())
        .setPostprocessor(imageRequest.getPostprocessor())
        .setProgressiveRenderingEnabled(imageRequest.getProgressiveRenderingEnabled())
        .setRequestPriority(imageRequest.getPriority())
        .setResizeOptions(imageRequest.getResizeOptions())
        .setRequestListener(imageRequest.getRequestListener())
        .setRotationOptions(imageRequest.getRotationOptions())
        .setShouldDecodePrefetches(imageRequest.shouldDecodePrefetches());
  }

  private ImageRequestBuilder() {
  }

  /**
   * Sets the source uri (both network and local uris are supported).
   * Note: this will enable disk caching for network sources, and disable it for local sources.
   * @param uri the uri to fetch the image from
   * @return the updated builder instance
   */
  public ImageRequestBuilder setSource(Uri uri) {
    Preconditions.checkNotNull(uri);

    mSourceUri = uri;
    return this;
  }

  /** Gets the source Uri. */
  public Uri getSourceUri() {
    return mSourceUri;
  }

  /**
   * Sets the lowest level that is permitted to request the image from.
   * @param requestLevel the lowest request level that is allowed
   * @return the updated builder instance
   */
  public ImageRequestBuilder setLowestPermittedRequestLevel(RequestLevel requestLevel) {
    mLowestPermittedRequestLevel = requestLevel;
    return this;
  }

  /** Gets the lowest permitted request level. */
  public RequestLevel getLowestPermittedRequestLevel() {
    return mLowestPermittedRequestLevel;
  }

  /**
   * Enables or disables auto-rotate for the image in case image has orientation.
   * @return the updated builder instance
   * @param enabled
   * @deprecated Use #setRotationOptions(RotationOptions)
   */
  @Deprecated
  public ImageRequestBuilder setAutoRotateEnabled(boolean enabled) {
    if (enabled) {
      return setRotationOptions(RotationOptions.autoRotate());
    } else {
      return setRotationOptions(RotationOptions.disableRotation());
    }
  }

  /**
   * Sets resize options in case resize should be performed.
   * @param resizeOptions resize options
   * @return the modified builder instance
   */
  public ImageRequestBuilder setResizeOptions(@Nullable ResizeOptions resizeOptions) {
    mResizeOptions = resizeOptions;
    return this;
  }

  /** Gets the resize options if set, null otherwise. */
  public @Nullable ResizeOptions getResizeOptions() {
    return mResizeOptions;
  }

  /**
   * Sets rotation options for the image, whether to rotate by a multiple of 90 degrees, use the
   * EXIF metadata (relevant to JPEGs only) or to not rotate. This also specifies whether the
   * rotation should be left until the bitmap is rendered (as the GPU can do this more efficiently
   * than the effort to change the bitmap object).
   *
   * @param rotationOptions rotation options
   * @return the modified builder instance
   */
  public ImageRequestBuilder setRotationOptions(@Nullable RotationOptions rotationOptions) {
    mRotationOptions = rotationOptions;
    return this;
  }

  /** Gets the rotation options if set, null otherwise. */
  public @Nullable RotationOptions getRotationOptions() {
    return mRotationOptions;
  }

  /**
   * Set the range of bytes to request from the network (in the case of an HTTP request). This is
   * only used if {@link ImagePipelineExperiments#isPartialImageCachingEnabled()} is true and your
   * {@link com.facebook.imagepipeline.producers.NetworkFetcher} makes use of it.
   *
   * <p> Even where this is supported, there is no contract that this must be followed. The response
   * may contain the full image data, more than is requested or less, depending on what's already in
   * cache and external factors.
   *
   * @param bytesRange the range of bytes
   * @return the modified builder instance
   */
  public ImageRequestBuilder setBytesRange(@Nullable BytesRange bytesRange) {
    mBytesRange = bytesRange;
    return this;
  }

  /** Gets the range of bytes if set, null otherwise. */
  @Nullable
  public BytesRange getBytesRange() {
    return mBytesRange;
  }

  public ImageRequestBuilder setImageDecodeOptions(ImageDecodeOptions imageDecodeOptions) {
    mImageDecodeOptions = imageDecodeOptions;
    return this;
  }

  public ImageDecodeOptions getImageDecodeOptions() {
    return mImageDecodeOptions;
  }

  /**
   * Sets the cache option. Pipeline might use different caches and eviction policies for each
   * image type.
   * @param cacheChoice the cache choice to set
   * @return the modified builder instance
   */
  public ImageRequestBuilder setCacheChoice(ImageRequest.CacheChoice cacheChoice) {
    mCacheChoice = cacheChoice;
    return this;
  }

  /** Gets the cache choice (profile image or default). */
  public CacheChoice getCacheChoice() {
    return mCacheChoice;
  }

  /**
   * Enables or disables progressive rendering.
   * @param enabled
   * @return the modified builder instance
   */
  public ImageRequestBuilder setProgressiveRenderingEnabled(boolean enabled) {
    mProgressiveRenderingEnabled = enabled;
    return this;
  }

  /** Returns whether progressive loading is enabled. */
  public boolean isProgressiveRenderingEnabled() {
    return mProgressiveRenderingEnabled;
  }

  /**
   * Enables or disables the use of local thumbnails as previews.
   * @param enabled
   * @return the modified builder instance
   */
  public ImageRequestBuilder setLocalThumbnailPreviewsEnabled(boolean enabled) {
    mLocalThumbnailPreviewsEnabled = enabled;
    return this;
  }

  /** Returns whether the use of local thumbnails for previews is enabled. */
  public boolean isLocalThumbnailPreviewsEnabled() {
    return mLocalThumbnailPreviewsEnabled;
  }

  /** Disables disk cache for this request, regardless where the image will come from. */
  public ImageRequestBuilder disableDiskCache() {
    mDiskCacheEnabled = false;
    return this;
  }

  /** Returns whether the use of the disk cache is enabled, which is partly dependent on the URI. */
  public boolean isDiskCacheEnabled() {
    return mDiskCacheEnabled && UriUtil.isNetworkUri(mSourceUri);
  }

  /** Disables memory cache for this request. */
  public ImageRequestBuilder disableMemoryCache() {
    mMemoryCacheEnabled = false;
    return this;
  }

  /** Returns whether the use of the memory cache is enabled. */
  public boolean isMemoryCacheEnabled() {
    return mMemoryCacheEnabled;
  }

  /**
   * Set priority for the request.
   * @param requestPriority
   * @return the modified builder instance
   */
  public ImageRequestBuilder setRequestPriority(Priority requestPriority) {
    mRequestPriority = requestPriority;
    return this;
  }

  /** Returns the request priority. */
  public Priority getRequestPriority() {
    return mRequestPriority;
  }

  /**
   * Sets the postprocessor.
   * @param postprocessor postprocessor to postprocess the output bitmap with.
   * @return the modified builder instance
   */
  public ImageRequestBuilder setPostprocessor(Postprocessor postprocessor) {
    mPostprocessor = postprocessor;
    return this;
  }

  /** Gets postprocessor if set, null otherwise. */
  public @Nullable Postprocessor getPostprocessor() {
    return mPostprocessor;
  }

  /**
   * Sets a request listener to use for just this image request
   *
   * @param requestListener a request listener to use in addition to the global ones set in the
   * {@link com.facebook.imagepipeline.core.ImagePipelineConfig}
   * @return the modified builder instance
   */
  public ImageRequestBuilder setRequestListener(RequestListener requestListener) {
    mRequestListener = requestListener;
    return this;
  }

  /**
   * @return the additional request listener to use for this image request
   */
  public @Nullable RequestListener getRequestListener() {
    return mRequestListener;
  }

  /**
   * Builds the Request.
   * @return a valid image request
   */
  public ImageRequest build() {
    validate();
    return new ImageRequest(this);
  }

  public @Nullable Boolean shouldDecodePrefetches() {
    return mDecodePrefetches;
  }

  public ImageRequestBuilder setShouldDecodePrefetches(@Nullable Boolean shouldDecodePrefetches) {
    mDecodePrefetches = shouldDecodePrefetches;
    return this;
  }

  public ImageRequestBuilder setResizingAllowedOverride(@Nullable Boolean resizingAllowedOverride) {
    mResizingAllowedOverride = resizingAllowedOverride;
    return this;
  }

  public @Nullable Boolean getResizingAllowedOverride() {
    return mResizingAllowedOverride;
  }

  /** An exception class for builder methods. */
  public static class BuilderException extends RuntimeException {
    public BuilderException(String message) {
      super("Invalid request builder: " + message);
    }
  }

  /** Performs validation. */
  protected void validate() {
    // make sure that the source uri is set correctly.
    if (mSourceUri == null) {
      throw new BuilderException("Source must be set!");
    }

    // For local resource we require caller to specify statically generated resource id as a path.
    if (UriUtil.isLocalResourceUri(mSourceUri)) {
      if (!mSourceUri.isAbsolute()) {
        throw new BuilderException("Resource URI path must be absolute.");
      }
      if (mSourceUri.getPath().isEmpty()) {
        throw new BuilderException("Resource URI must not be empty");
      }
      try {
        Integer.parseInt(mSourceUri.getPath().substring(1));
      } catch (NumberFormatException ignored) {
        throw new BuilderException("Resource URI path must be a resource id.");
      }
    }

    // For local asset we require caller to specify absolute path of an asset, which will be
    // resolved by AssetManager relative to configured asset folder of an app.
    if (UriUtil.isLocalAssetUri(mSourceUri) && !mSourceUri.isAbsolute()) {
      throw new BuilderException("Asset URI path must be absolute.");
    }
  }
}
