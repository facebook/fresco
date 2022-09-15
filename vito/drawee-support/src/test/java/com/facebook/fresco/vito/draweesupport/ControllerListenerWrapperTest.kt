/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.draweesupport

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.drawee.controller.ControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.verifyZeroInteractions

class ControllerListenerWrapperTest {

  private lateinit var controllerListener: ControllerListener<ImageInfo>
  private lateinit var controllerListenerWrapper: ControllerListenerWrapper

  @Before
  fun setup() {
    controllerListener = mock()
    controllerListenerWrapper = ControllerListenerWrapper(controllerListener)
  }

  @Test
  fun testOnSubmit() {
    controllerListenerWrapper.onSubmit(ID.toLong(), CALLER_CONTEXT)
    verify(controllerListener).onSubmit(eq(STRING_ID), eq(CALLER_CONTEXT))
    verifyNoMoreInteractions(controllerListener)
  }

  @Test
  fun testOnPlaceholderSet() {
    val placeholder = mock<Drawable>()
    controllerListenerWrapper.onPlaceholderSet(ID.toLong(), placeholder)
    verifyZeroInteractions(controllerListener)
  }

  @Test
  fun testOnIntermediateImageFailed() {
    val throwable = Throwable("Message")
    controllerListenerWrapper.onIntermediateImageFailed(ID.toLong(), throwable)
    verify(controllerListener).onIntermediateImageFailed(eq(STRING_ID), eq(throwable))
    verifyNoMoreInteractions(controllerListener)
  }

  @Test
  fun testOnFailure() {
    val throwable = Throwable("Message")
    val errorDrawable = mock<Drawable>()
    controllerListenerWrapper.onFailure(ID.toLong(), errorDrawable, throwable)
    verify(controllerListener).onFailure(eq(STRING_ID), eq(throwable))
    verifyNoMoreInteractions(controllerListener)
  }

  @Test
  fun testOnRelease() {
    controllerListenerWrapper.onRelease(ID.toLong())
    verify(controllerListener).onRelease(eq(STRING_ID))
    verifyNoMoreInteractions(controllerListener)
  }

  @Test
  fun testOnIntermediateImageSet() {
    val imageInfo = mock<ImageInfo>()
    controllerListenerWrapper.onIntermediateImageSet(ID.toLong(), imageInfo)
    verify(controllerListener).onIntermediateImageSet(eq(STRING_ID), eq(imageInfo))
    verifyNoMoreInteractions(controllerListener)
  }

  @Test
  fun testOnFinalImageSetNormalDrawable() {
    val drawable = mock<Drawable>()
    val imageInfo = mock<ImageInfo>()
    controllerListenerWrapper.onFinalImageSet(ID.toLong(), ImageOrigin.DISK, imageInfo, drawable)
    verify(controllerListener).onFinalImageSet(eq(STRING_ID), eq(imageInfo), isNull())
    verifyNoMoreInteractions(controllerListener)
  }

  @Test
  fun testOnFinalImageSetAnimatedDrawable() {
    val drawable = mock<AnimatedDrawable>()
    val imageInfo = mock<ImageInfo>()
    controllerListenerWrapper.onFinalImageSet(ID.toLong(), ImageOrigin.DISK, imageInfo, drawable)
    verify(controllerListener).onFinalImageSet(eq(STRING_ID), eq(imageInfo), eq(drawable))
    verifyNoMoreInteractions(controllerListener)
  }

  @Test
  fun testCreateWrapper() {
    val wrapper = ControllerListenerWrapper.create(controllerListener)
    assertThat(wrapper).isNotNull
  }

  @Test
  fun testCreateNullWrapper() {
    val wrapper = ControllerListenerWrapper.create(null)
    assertThat(wrapper).isNull()
  }

  companion object {
    private const val ID = 123
    private const val STRING_ID = "v123"
    private const val CALLER_CONTEXT = "caller-context"
  }

  abstract class AnimatedDrawable : Drawable(), Animatable
}
