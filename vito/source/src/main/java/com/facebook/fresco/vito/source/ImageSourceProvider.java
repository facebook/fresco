/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source;

import android.net.Uri;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Create image sources that can be passed to Fresco's image components. For example, to create a
 * single image source for a given URI, call {@link #forUri(Uri)} or {@link #forUri(String)}.
 *
 * <p>It is also possible to set your own provider by calling {@link
 * #setImplementation(Implementation)}
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class ImageSourceProvider {

  /** @return an empty image source if no image URI is available to pass to the UI component */
  public static ImageSource emptySource() {
    return get().emptySource();
  }

  /**
   * Create a single image source for a given image request.
   *
   * @param imageRequest the image request to use
   * @return the ImageSource to be passed to the UI component
   */
  public static ImageSource forImageRequest(@Nullable ImageRequest imageRequest) {
    if (imageRequest == null) {
      return emptySource();
    }
    return get().singleImageRequest(imageRequest);
  }

  /**
   * Create a single image source for a given image request.
   *
   * @param imageRequest the image request to use
   * @param requestLevelForFetch request level for fetch
   * @return the ImageSource to be passed to the UI component
   */
  public static ImageSource forImageRequest(
      @Nullable ImageRequest imageRequest, ImageRequest.RequestLevel requestLevelForFetch) {
    if (imageRequest == null) {
      return emptySource();
    }
    return get().singleImageRequest(imageRequest, requestLevelForFetch);
  }

  /**
   * Create a single image source for a given image URI.
   *
   * @param uri the image URI to use
   * @return the ImageSource to be passed to the UI component
   */
  public static ImageSource forUri(@Nullable Uri uri) {
    if (uri == null) {
      return emptySource();
    }
    return get().singleUri(uri);
  }

  /**
   * Create a single image source for a given image URI.
   *
   * @param uriString the image URI String to use
   * @return the ImageSource to be passed to the UI component
   */
  public static ImageSource forUri(@Nullable String uriString) {
    if (uriString == null) {
      return emptySource();
    }
    return get().singleUri(Uri.parse(uriString));
  }

  /**
   * Create a multi image source for a given set of sources. Image sources are obtained in order.
   * Only if the current source fails, or if it finishes without a result, the next one will be
   * tried.
   *
   * @param imageSources the list of image sources to be used
   * @return the ImageSource to be passed to the UI component
   */
  public static ImageSource firstAvailable(ImageSource... imageSources) {
    return get().firstAvailable(imageSources);
  }

  /**
   * Create a multi image source for a low- and high resolution image. Both requests will be sent
   * off, the low resolution will be used as an intermediate image until the high resolution one is
   * available.
   *
   * @param lowResImageSource the low resolution image source to be used
   * @param highResImageSource the high resolution image source to be used
   * @return the ImageSource to be passed to the UI component
   */
  public static ImageSource increasingQuality(
      ImageSource lowResImageSource, ImageSource highResImageSource) {
    return get().increasingQuality(lowResImageSource, highResImageSource);
  }

  /**
   * Create a multi image source for a low- and high resolution image. Both requests will be sent
   * off, the low resolution will be used as an intermediate image until the high resolution one is
   * available.
   *
   * @param lowResImageUri the low resolution image URI to be used
   * @param highResImageUri the high resolution image URI to be used
   * @return the ImageSource to be passed to the UI component
   */
  public static ImageSource increasingQuality(Uri lowResImageUri, Uri highResImageUri) {
    return get().increasingQuality(forUri(lowResImageUri), forUri(highResImageUri));
  }

  private static @Nullable Implementation sImplementation;

  private ImageSourceProvider() {}

  public static void setImplementation(Implementation implementation) {
    sImplementation = implementation;
  }

  public static void shutdown() {
    sImplementation = null;
  }

  private static Implementation get() {
    if (sImplementation == null) {
      throw new RuntimeException("ImageSourceProvider must be initialized first!");
    }
    return sImplementation;
  }

  public interface Implementation {
    ImageSource emptySource();

    ImageSource singleImageRequest(ImageRequest imageRequest);

    ImageSource singleImageRequest(
        ImageRequest imageRequest, ImageRequest.RequestLevel requestLevelForFetch);

    ImageSource singleUri(Uri uri);

    ImageSource firstAvailable(ImageSource... imageSources);

    ImageSource increasingQuality(ImageSource lowResImageSource, ImageSource highResImageSource);
  }
}
