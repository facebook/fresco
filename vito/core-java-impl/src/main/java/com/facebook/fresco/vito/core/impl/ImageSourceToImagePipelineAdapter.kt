/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import com.facebook.common.internal.Supplier
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSources
import com.facebook.datasource.FirstAvailableDataSourceSupplier
import com.facebook.datasource.IncreasingQualityDataSourceSupplier
import com.facebook.fresco.middleware.Extras
import com.facebook.fresco.vito.core.ImagePipelineUtils
import com.facebook.fresco.vito.core.impl.source.DataSourceImageSource
import com.facebook.fresco.vito.core.impl.source.ImagePipelineImageSource
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.EmptyImageSource
import com.facebook.fresco.vito.source.FirstAvailableImageSource
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.IncreasingQualityImageSource
import com.facebook.fresco.vito.source.SingleImageSource
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.request.ImageRequest
import java.lang.NullPointerException
import java.util.ArrayList

object ImageSourceToImagePipelineAdapter {

  val NO_REQUEST_EXCEPTION = NullPointerException("No image request was specified!")

  private val NO_REQUEST_SUPPLIER: Supplier<DataSource<CloseableReference<CloseableImage>>> =
      Supplier {
        DataSources.immediateFailedDataSource(NO_REQUEST_EXCEPTION)
      }

  /**
   * Get the final image request for the last image if known. In some cases, like for @{link
   * FirstAvailableImageSource}, we do not know the final image request, so this method will return
   * null.
   *
   * @param imagePipelineUtils util class to create the final image request
   * @param imageOptions the image options to use, important if for example rounding is done at
   *   decode time
   * @return the final image request or null if not possible to determine
   */
  @JvmStatic
  fun maybeExtractFinalImageRequest(
      imageSource: ImageSource,
      imagePipelineUtils: ImagePipelineUtils,
      imageOptions: ImageOptions
  ): ImageRequest? {
    return when (imageSource) {
      is SingleImageSource -> imageSource.extractSingleRequest(imagePipelineUtils, imageOptions)
      is EmptyImageSource -> null
      is FirstAvailableImageSource ->
          imageSource.extractFirstAvailableRequest(imagePipelineUtils, imageOptions)
      is IncreasingQualityImageSource ->
          maybeExtractFinalImageRequest(imageSource.highResSource, imagePipelineUtils, imageOptions)
      is ImagePipelineImageSource ->
          imageSource.maybeExtractFinalImageRequest(imagePipelineUtils, imageOptions)
      else -> null
    }
  }

  @JvmStatic
  fun createDataSourceSupplier(
      imageSource: ImageSource,
      imagePipeline: ImagePipeline,
      imagePipelineUtils: ImagePipelineUtils,
      imageOptions: ImageOptions,
      callerContext: Any?,
      requestListener: RequestListener?,
      uiComponentId: String,
      extras: MutableMap<String, Any>
  ): Supplier<DataSource<CloseableReference<CloseableImage>>> {
    return when (imageSource) {
      is SingleImageSource -> {
        return Supplier<DataSource<CloseableReference<CloseableImage>>> {
          val imageRequest = imageSource.extractSingleRequest(imagePipelineUtils, imageOptions)
          createDataSource(
              imageRequest,
              imagePipeline,
              callerContext,
              requestListener,
              uiComponentId,
              ImageRequest.RequestLevel.FULL_FETCH,
              extras)
        }
      }
      is ImagePipelineImageSource -> {
        return Supplier<DataSource<CloseableReference<CloseableImage>>> {
          val imageRequest =
              imageSource.maybeExtractFinalImageRequest(imagePipelineUtils, imageOptions)
          createDataSource(
              imageRequest,
              imagePipeline,
              callerContext,
              requestListener,
              uiComponentId,
              imageSource.getRequestLevelForFetch(),
              extras)
        }
      }
      is EmptyImageSource -> NO_REQUEST_SUPPLIER
      is FirstAvailableImageSource ->
          FirstAvailableDataSourceSupplier.create(
              imageSource.imageSources.mapTo(ArrayList(imageSource.imageSources.size)) {
                createDataSourceSupplier(
                    it,
                    imagePipeline,
                    imagePipelineUtils,
                    imageOptions,
                    callerContext,
                    requestListener,
                    uiComponentId,
                    extras)
              })
      is IncreasingQualityImageSource ->
          return IncreasingQualityDataSourceSupplier.create(
              arrayListOf(
                  createDataSourceSupplier(
                      imageSource.highResSource,
                      imagePipeline,
                      imagePipelineUtils,
                      imageOptions,
                      callerContext,
                      requestListener,
                      uiComponentId,
                      extras),
                  createDataSourceSupplier(
                      imageSource.lowResSource,
                      imagePipeline,
                      imagePipelineUtils,
                      imageOptions,
                      callerContext,
                      requestListener,
                      uiComponentId,
                      extras)))
      is DataSourceImageSource -> imageSource.dataSourceSupplier
      else -> NO_REQUEST_SUPPLIER
    }
  }

  @JvmStatic
  fun createDataSource(
      imageRequest: ImageRequest?,
      imagePipeline: ImagePipeline,
      callerContext: Any?,
      requestListener: RequestListener?,
      uiComponentId: String,
      requestLevel: ImageRequest.RequestLevel = ImageRequest.RequestLevel.FULL_FETCH,
      extras: Extras
  ): DataSource<CloseableReference<CloseableImage>> {
    return if (imageRequest != null) {
      imagePipeline.fetchDecodedImage(
          imageRequest,
          callerContext,
          requestLevel,
          requestListener, // TODO: Check if this is correct !!
          uiComponentId,
          extras)
    } else {
      NO_REQUEST_SUPPLIER.get()
    }
  }

  fun SingleImageSource.extractSingleRequest(
      imagePipelineUtils: ImagePipelineUtils,
      imageOptions: ImageOptions
  ): ImageRequest? = imagePipelineUtils.buildImageRequest(imageUri, imageOptions)

  fun FirstAvailableImageSource.extractFirstAvailableRequest(
      imagePipelineUtils: ImagePipelineUtils,
      imageOptions: ImageOptions
  ): ImageRequest? {
    imageSources.forEach {
      val request = maybeExtractFinalImageRequest(it, imagePipelineUtils, imageOptions)
      if (request != null) {
        return request
      }
    }
    return null
  }
}
