/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSubscriber
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.renderer.ImageDataModel
import com.facebook.imagepipeline.image.CloseableImage
import java.util.concurrent.Executor

class ImageFetchSubscriber(
    private val imageId: Long,
    private val drawable: KFrescoVitoDrawable,
    private val imageToDataModelMapper: (CloseableImage, ImageOptions) -> ImageDataModel?,
    private var debugOverlayHandler: DebugOverlayHandler? = null,
    private val invalidationExecutor: Executor? = null,
) : DataSubscriber<CloseableReference<CloseableImage>> {
  override fun onNewResult(dataSource: DataSource<CloseableReference<CloseableImage>>) {
    if (imageId != drawable.imageId) {
      return
    }
    val request = drawable.imageRequest ?: return

    val result: CloseableReference<CloseableImage>? = dataSource.result

    if (result == null || !result.isValid) {
      onFailure(dataSource)
      result?.close()
      return
    }

    // We avoid cloning result and closing the original for performance reasons
    drawable.setCloseable(result)
    val image = result.get()
    drawable.actualImageLayer.setActualImage(request.imageOptions, image, imageToDataModelMapper)
    // Remove the progress image
    drawable.placeholderLayer.reset()
    if (dataSource.isFinished) {
      drawable.hideProgressLayer()
    }
    if (notifyFinalResult(dataSource)) {
      drawable.listenerManager.onFinalImageSet(
          imageId,
          request,
          ImageOrigin.UNKNOWN,
          image,
          drawable.obtainExtras(dataSource, result),
          drawable.actualImageDrawable)
    } else {
      drawable.listenerManager.onIntermediateImageSet(imageId, request, image)
    }
    invalidate(drawable)
  }

  override fun onFailure(dataSource: DataSource<CloseableReference<CloseableImage>>) {
    if (imageId != drawable.imageId) {
      return
    }
    val request = drawable.imageRequest ?: return
    drawable.actualImageLayer.setError(request.resources, request.imageOptions)
    if (dataSource.isFinished) {
      drawable.hideProgressLayer()
    }
    if (notifyFinalResult(dataSource)) {
      dataSource.result.use { result ->
        drawable.listenerManager.onFailure(
            imageId,
            request,
            drawable.actualImageLayer.getDataModel().maybeGetDrawable(),
            dataSource.failureCause,
            drawable.obtainExtras(dataSource, result))
      }
    } else {
      drawable.listenerManager.onIntermediateImageFailed(
          imageId,
          request,
          dataSource.failureCause,
      )
    }
    drawable.imagePerfListener.onImageError(drawable)
    invalidate(drawable)
  }

  override fun onCancellation(dataSource: DataSource<CloseableReference<CloseableImage>>) = Unit

  override fun onProgressUpdate(dataSource: DataSource<CloseableReference<CloseableImage>>) {
    drawable.updateProgress(dataSource)
    debugOverlayHandler?.update(drawable)
  }

  private fun invalidate(drawable: KFrescoVitoDrawable) {
    invalidationExecutor?.execute { drawable.invalidateSelf() } ?: drawable.invalidateSelf()
    debugOverlayHandler?.update(drawable)
  }

  private fun notifyFinalResult(
      dataSource: DataSource<CloseableReference<CloseableImage>>?
  ): Boolean {
    return dataSource == null || dataSource.isFinished || dataSource.hasMultipleResults()
  }
}
