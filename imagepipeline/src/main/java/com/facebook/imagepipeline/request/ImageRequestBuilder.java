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

import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.util.TypedValue;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.common.ResizeOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.facebook.imagepipeline.request.ImageRequest.ImageType;
import static com.facebook.imagepipeline.request.ImageRequest.RequestLevel;

/**
 * Builder class for {@link ImageRequest}s.
 */
public class ImageRequestBuilder {

  /**
   * Local resource supported extensions
   */
  private static final Set<String> LOCAL_RESOURCE_EXT
          = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(".png", ".jpg", ".gif")));

  private Uri mSourceUri = null;
  private RequestLevel mLowestPermittedRequestLevel = RequestLevel.FULL_FETCH;
  private boolean mAutoRotateEnabled = false;
  private @Nullable ResizeOptions mResizeOptions = null;
  private ImageDecodeOptions mImageDecodeOptions = ImageDecodeOptions.defaults();
  private ImageType mImageType = ImageType.DEFAULT;
  private boolean mProgressiveRenderingEnabled = false;
  private boolean mLocalThumbnailPreviewsEnabled = false;
  private Priority mRequestPriority = Priority.HIGH;
  private @Nullable Postprocessor mPostprocessor = null;

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
   * <p/>
   * Only image resources can be used with the image pipeline (PNG, JPG, GIF). Other resource
   * types such as Strings or XML Drawables make no sense in the context of the image pipeline and
   * so cannot be supported by definition. One potentially confusing case is drawable declared in
   * XML (e.g. ShapeDrawable). Important thing to note is that this is not an image. If you want
   * to display an XML drawable as the main image, then set it as a placeholder and use the null uri.
   * <p/>
   *
   * @param resources application resources.
   * @param resId     local image resource id.
   * @return a new request builder instance.
   */
  public static ImageRequestBuilder newBuilderWithResourceId(Resources resources,
                                                             @DrawableRes int resId) {
    TypedValue value = new TypedValue();
    resources.getValue(resId, value, true);
    String resourceName = value.string.toString();
    String resourceExt = resourceName.substring(resourceName.lastIndexOf('.')).toLowerCase();

    if (!LOCAL_RESOURCE_EXT.contains(resourceExt)) {
      throw new IllegalArgumentException("Only image resources can be used with"
          + " the image pipeline. " + resourceExt + " files are not supported.");
    }

    Uri uri = new Uri.Builder().scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
        .path(String.valueOf(resId)).build();

    return newBuilderWithSource(uri);
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
   */
  public ImageRequestBuilder setAutoRotateEnabled(boolean enabled) {
    mAutoRotateEnabled = enabled;
    return this;
  }

  /** Returns whether auto-rotate is enabled. */
  public boolean isAutoRotateEnabled() {
    return mAutoRotateEnabled;
  }

  /**
   * Sets resize options in case resize should be performed.
   * @param resizeOptions resize options
   * @return the modified builder instance
   */
  public ImageRequestBuilder setResizeOptions(ResizeOptions resizeOptions) {
    mResizeOptions = resizeOptions;
    return this;
  }

  /** Gets the resize options if set, null otherwise. */
  public @Nullable ResizeOptions getResizeOptions() {
    return mResizeOptions;
  }

  public ImageRequestBuilder setImageDecodeOptions(ImageDecodeOptions imageDecodeOptions) {
    mImageDecodeOptions = imageDecodeOptions;
    return this;
  }

  public ImageDecodeOptions getImageDecodeOptions() {
    return mImageDecodeOptions;
  }

  /**
   * Sets the image type. Pipeline might use different caches and eviction policies for each
   * image type.
   * @param imageType the image type to set
   * @return the modified builder instance
   */
  public ImageRequestBuilder setImageType(ImageType imageType) {
    mImageType = imageType;
    return this;
  }

  /** Gets the image type (profile image or default). */
  public ImageType getImageType() {
    return mImageType;
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

  /** Returns whether the use of local thumbnails for previews is enabled */
  public boolean isLocalThumbnailPreviewsEnabled() {
    return mLocalThumbnailPreviewsEnabled;
  }

  /** Returns whether the use of the disk cache is enabled */
  public boolean isDiskCacheEnabled() {
    return UriUtil.isNetworkUri(mSourceUri);
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

  /** Returns the request priority */
  public Priority getRequestPriority() {
    return mRequestPriority;
  }

  /**
   * Sets the postprocessor.
   * @param postprocessor postprocessor to postprocess the output bitmap with.
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
   * Builds the Request.
   * @return a valid image request
   */
  public ImageRequest build() {
    validate();
    return new ImageRequest(this);
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
