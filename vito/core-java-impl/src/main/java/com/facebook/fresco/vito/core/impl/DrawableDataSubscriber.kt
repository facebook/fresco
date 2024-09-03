/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.imagepipeline.image.CloseableImage

interface DrawableDataSubscriber {

  fun onNewResult(
      drawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest,
      dataSource: DataSource<CloseableReference<CloseableImage>>
  )

  fun onFailure(
      drawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest,
      dataSource: DataSource<CloseableReference<CloseableImage>>
  )

  fun onProgressUpdate(
      drawable: FrescoDrawable2Impl,
      imageRequest: VitoImageRequest,
      dataSource: DataSource<CloseableReference<CloseableImage>>
  )

  fun onRelease(drawable: FrescoDrawable2Impl)
}
