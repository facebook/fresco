/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.datasource.FirstAvailableDataSourceSupplier;
import com.facebook.datasource.IncreasingQualityDataSourceSupplier;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.multiuri.MultiUri;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

public class ImagePipelineMultiUriHelper {

  private static final NullPointerException NO_REQUEST_EXCEPTION =
      new NullPointerException("No image request was specified!");

  public static Supplier<DataSource<CloseableReference<CloseableImage>>>
      getMultiUriDatasourceSupplier(
          final ImagePipeline imagePipeline,
          final MultiUri multiUri,
          final Object callerContext,
          final @Nullable RequestListener requestListener,
          final @Nullable String id) {

    Supplier<DataSource<CloseableReference<CloseableImage>>> supplier = null;

    // final image supplier;
    if (multiUri.getHighResImageRequest() != null) {
      supplier =
          getImageRequestDataSourceSupplier(
              imagePipeline, multiUri.getHighResImageRequest(), callerContext, requestListener, id);
    } else if (multiUri.getMultiImageRequests() != null) {
      supplier =
          getFirstAvailableDataSourceSupplier(
              imagePipeline,
              callerContext,
              requestListener,
              multiUri.getMultiImageRequests(),
              true,
              id);
    }

    // increasing-quality supplier; highest-quality supplier goes first
    if (supplier != null && multiUri.getLowResImageRequest() != null) {
      List<Supplier<DataSource<CloseableReference<CloseableImage>>>> suppliers = new LinkedList<>();
      suppliers.add(supplier);
      suppliers.add(
          getImageRequestDataSourceSupplier(
              imagePipeline, multiUri.getLowResImageRequest(), callerContext, requestListener, id));
      supplier = IncreasingQualityDataSourceSupplier.create(suppliers, false);
    }

    // no image requests; use null data source supplier
    if (supplier == null) {
      supplier = DataSources.getFailedDataSourceSupplier(NO_REQUEST_EXCEPTION);
    }

    return supplier;
  }

  public static DataSource<CloseableReference<CloseableImage>> getImageRequestDataSource(
      ImagePipeline imagePipeline,
      ImageRequest imageRequest,
      Object callerContext,
      @Nullable RequestListener requestListener,
      @Nullable String uiComponentId) {
    return imagePipeline.fetchDecodedImage(
        imageRequest,
        callerContext,
        ImageRequest.RequestLevel.FULL_FETCH,
        requestListener,
        uiComponentId);
  }

  private static Supplier<DataSource<CloseableReference<CloseableImage>>>
      getFirstAvailableDataSourceSupplier(
          final ImagePipeline imagePipeline,
          final Object callerContext,
          final @Nullable RequestListener requestListener,
          ImageRequest[] imageRequests,
          boolean tryBitmapCacheOnlyFirst,
          final @Nullable String uiComponentId) {
    List<Supplier<DataSource<CloseableReference<CloseableImage>>>> suppliers =
        new ArrayList<>(imageRequests.length * 2);
    if (tryBitmapCacheOnlyFirst) {
      // we first add bitmap-cache-only suppliers, then the full-fetch ones
      for (int i = 0; i < imageRequests.length; i++) {
        suppliers.add(
            getImageRequestDataSourceSupplier(
                imagePipeline,
                imageRequests[i],
                callerContext,
                ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE,
                requestListener,
                uiComponentId));
      }
    }
    for (int i = 0; i < imageRequests.length; i++) {
      suppliers.add(
          getImageRequestDataSourceSupplier(
              imagePipeline, imageRequests[i], callerContext, requestListener, uiComponentId));
    }
    return FirstAvailableDataSourceSupplier.create(suppliers);
  }

  private static Supplier<DataSource<CloseableReference<CloseableImage>>>
      getImageRequestDataSourceSupplier(
          final ImagePipeline imagePipeline,
          final ImageRequest imageRequest,
          final Object callerContext,
          final ImageRequest.RequestLevel requestLevel,
          final RequestListener requestListener,
          final @Nullable String uiComponentId) {
    return new Supplier<DataSource<CloseableReference<CloseableImage>>>() {
      @Override
      public DataSource<CloseableReference<CloseableImage>> get() {
        return getImageRequestDataSource(
            imagePipeline, imageRequest, callerContext, requestListener, uiComponentId);
      }
    };
  }

  private static Supplier<DataSource<CloseableReference<CloseableImage>>>
      getImageRequestDataSourceSupplier(
          final ImagePipeline imagePipeline,
          final ImageRequest imageRequest,
          final Object callerContext,
          final RequestListener requestListener,
          final @Nullable String uiComponentId) {
    return getImageRequestDataSourceSupplier(
        imagePipeline,
        imageRequest,
        callerContext,
        ImageRequest.RequestLevel.FULL_FETCH,
        requestListener,
        uiComponentId);
  }
}
