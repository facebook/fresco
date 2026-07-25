/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.renderer.ColorIntImageDataModel
import com.facebook.fresco.vito.renderer.DrawableImageDataModel
import com.facebook.fresco.vito.renderer.ImageDataModel
import com.facebook.imagepipeline.image.BaseCloseableImage
import com.facebook.imagepipeline.image.CloseableImage
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Unit tests for the `ImageLayerDataModel` extension functions in ActualImageHandler.kt. */
@RunWith(RobolectricTestRunner::class)
class ActualImageHandlerTest {

  private lateinit var resources: Resources
  private lateinit var layer: ImageLayerDataModel

  @Before
  fun setUp() {
    resources = RuntimeEnvironment.getApplication().resources
    layer = ImageLayerDataModel()
  }

  @Test
  fun testSetActualImage_usesMapperResultAndForwardsArguments() {
    val closeableImage: CloseableImage = FakeCloseableImage()
    val options = ImageOptions.create().build()
    val expectedModel = ColorIntImageDataModel(Color.RED)
    var seenResources: Resources? = null
    var seenImage: CloseableImage? = null
    var seenOptions: ImageOptions? = null

    layer.setActualImage(resources, options, closeableImage) { r, img, opts ->
      seenResources = r
      seenImage = img
      seenOptions = opts
      expectedModel
    }

    assertThat(layer.getDataModel()).isSameAs(expectedModel)
    assertThat(seenResources).isSameAs(resources)
    assertThat(seenImage).isSameAs(closeableImage)
    assertThat(seenOptions).isSameAs(options)
    assertThat(layer.canvasTransformationHandler.canvasTransformation).isNotNull()
  }

  @Test
  fun testSetActualImage_nullMapperResult_clearsDataModel() {
    layer.configure(dataModel = ColorIntImageDataModel(Color.RED))
    val options = ImageOptions.create().build()

    layer.setActualImage(resources, options, FakeCloseableImage()) { _, _, _ -> null }

    assertThat(layer.getDataModel()).isNull()
  }

  @Test
  fun testSetActualImageDrawable_wrapsDrawableInDrawableModel() {
    val drawable = ColorDrawable(Color.BLUE)
    val options = ImageOptions.create().build()

    layer.setActualImageDrawable(options, drawable)

    val model: ImageDataModel? = layer.getDataModel()
    assertThat(model).isInstanceOf(DrawableImageDataModel::class.java)
    assertThat((model as DrawableImageDataModel).drawable).isSameAs(drawable)
    assertThat(layer.canvasTransformationHandler.canvasTransformation).isNotNull()
  }

  @Test
  fun testSetPlaceholder_noPlaceholder_resetsLayer() {
    layer.setPlaceholder(
        resources,
        ImageOptions.create()
            .placeholderColor(Color.RED)
            .placeholderScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
            .build(),
    )
    assertThat(layer.getDataModel()).isNotNull()
    assertThat(layer.canvasTransformationHandler.canvasTransformation).isNotNull()

    layer.setPlaceholder(resources, ImageOptions.create().build())

    assertThat(layer.getDataModel()).isNull()
    assertThat(layer.canvasTransformationHandler.canvasTransformation).isNull()
  }

  @Test
  fun testSetOverlay_noOverlay_clearsModelButKeepsTransformation() {
    layer.setPlaceholder(
        resources,
        ImageOptions.create()
            .placeholderColor(Color.RED)
            .placeholderScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
            .build(),
    )
    val transformation = layer.canvasTransformationHandler.canvasTransformation
    assertThat(transformation).isNotNull()

    layer.setOverlay(resources, ImageOptions.create().build())

    assertThat(layer.getDataModel()).isNull()
    assertThat(layer.canvasTransformationHandler.canvasTransformation).isSameAs(transformation)
  }

  @Test
  fun testSetError_noError_resetsLayer() {
    layer.setError(
        resources,
        ImageOptions.create()
            .errorColor(Color.YELLOW)
            .errorScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
            .build(),
    )
    assertThat(layer.getDataModel()).isNotNull()
    assertThat(layer.canvasTransformationHandler.canvasTransformation).isNotNull()

    layer.setError(resources, ImageOptions.create().build())

    assertThat(layer.getDataModel()).isNull()
    assertThat(layer.canvasTransformationHandler.canvasTransformation).isNull()
  }

  private class FakeCloseableImage : BaseCloseableImage() {
    private var closed = false

    override fun getSizeInBytes(): Int = 0

    override fun close() {
      closed = true
    }

    override fun isClosed(): Boolean = closed

    override fun getWidth(): Int = 0

    override fun getHeight(): Int = 0
  }
}
