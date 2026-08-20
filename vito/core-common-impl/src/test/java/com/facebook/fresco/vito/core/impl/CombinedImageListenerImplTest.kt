/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.fresco.ui.common.DimensionsInfo
import com.facebook.fresco.ui.common.ImageLoadStatus
import com.facebook.fresco.ui.common.ImagePerfNotifier
import com.facebook.fresco.ui.common.ImagePerfNotifierHolder
import com.facebook.fresco.ui.common.ImagePerfState
import com.facebook.fresco.ui.common.VisibilityState
import com.facebook.fresco.vito.core.ImagePerfLoggingListener
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.VitoImageRequestListener
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.imagepipeline.image.ImageInfo
import java.io.Closeable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CombinedImageListenerImplTest {

  @Test
  fun onFinalImageSet_dispatchesToEveryListenerWithMatchingPayload() {
    val subject = CombinedImageListenerImpl()
    val globalVitoListener = RecordingVitoImageRequestListener("global")
    val localVitoListener = RecordingVitoImageRequestListener("local")
    val imageListener = RecordingImageListener()
    val controllerListener = RecordingControllerListener()
    val perfListener = RecordingImagePerfLoggingListener()
    val request = createImageRequest()
    val extras = ControllerListener2.Extras.of(mapOf("source" to "test"))
    val drawable = ColorDrawable(0xff102030.toInt())

    subject.setVitoImageRequestListener(globalVitoListener)
    subject.setLocalVitoImageRequestListener(localVitoListener)
    subject.imageListener = imageListener
    subject.setControllerListener2(controllerListener)
    subject.setImagePerfLoggingListener(perfListener)
    subject.onFinalImageSet(42L, request, ImageOrigin.MEMORY_BITMAP, null, extras, drawable)

    assertEquals(
        listOf("global:final:42:${ImageOrigin.MEMORY_BITMAP}:true:true"),
        globalVitoListener.events,
    )
    assertEquals(
        listOf("local:final:42:${ImageOrigin.MEMORY_BITMAP}:true:true"),
        localVitoListener.events,
    )
    assertSame(request, globalVitoListener.finalImageRequests.single())
    assertSame(request, localVitoListener.finalImageRequests.single())
    assertEquals(listOf("image:final:42:${ImageOrigin.MEMORY_BITMAP}:true"), imageListener.events)
    assertEquals(listOf("controller:final:v42:true"), controllerListener.events)
    assertEquals(listOf("perf:final:v42:true"), perfListener.events)
  }

  @Test
  fun onEmptyEvent_skipsLegacyImageListener() {
    val subject = CombinedImageListenerImpl()
    val globalVitoListener = RecordingVitoImageRequestListener("global")
    val localVitoListener = RecordingVitoImageRequestListener("local")
    val imageListener = RecordingImageListener()
    val controllerListener = RecordingControllerListener()
    val perfListener = RecordingImagePerfLoggingListener()
    val callerContext = Any()

    subject.setVitoImageRequestListener(globalVitoListener)
    subject.setLocalVitoImageRequestListener(localVitoListener)
    subject.imageListener = imageListener
    subject.setControllerListener2(controllerListener)
    subject.setImagePerfLoggingListener(perfListener)
    subject.onEmptyEvent(callerContext)

    assertEquals(listOf("global:empty:true"), globalVitoListener.events)
    assertEquals(listOf("local:empty:true"), localVitoListener.events)
    assertEquals(listOf("controller:empty:true"), controllerListener.events)
    assertEquals(listOf("perf:empty:true"), perfListener.events)
    assertEquals(emptyList<String>(), imageListener.events)
  }

  @Test
  fun onReset_withSelectiveFlagsKeepsConfiguredVitoAndControllerListeners() {
    val subject = CombinedImageListenerImpl()
    val globalVitoListener = RecordingVitoImageRequestListener("global")
    val localVitoListener = RecordingVitoImageRequestListener("local")
    val imageListener = RecordingImageListener()
    val controllerListener = RecordingControllerListener()
    val perfListener = RecordingImagePerfLoggingListener()
    val request = createImageRequest()

    subject.setVitoImageRequestListener(globalVitoListener)
    subject.setLocalVitoImageRequestListener(localVitoListener)
    subject.imageListener = imageListener
    subject.setControllerListener2(controllerListener)
    subject.setImagePerfLoggingListener(perfListener)
    subject.onReset(
        resetVitoImageRequestListener = false,
        resetLocalVitoImageRequestListener = false,
        resetLocalImagePerfStateListener = true,
        resetControllerListener2 = false,
    )
    subject.onRelease(7L, request, null)

    assertEquals(listOf("global:release:7:true"), globalVitoListener.events)
    assertEquals(listOf("local:release:7:true"), localVitoListener.events)
    assertSame(request, globalVitoListener.releaseRequests.single())
    assertSame(request, localVitoListener.releaseRequests.single())
    assertEquals(listOf("controller:release:v7:false"), controllerListener.events)
    assertEquals(emptyList<String>(), imageListener.events)
    assertTrue(perfListener.closed)
  }

  @Test
  fun onReset_withListenerResetFlagsRemovesVitoAndControllerListeners() {
    val subject = CombinedImageListenerImpl()
    val globalVitoListener = RecordingVitoImageRequestListener("global")
    val localVitoListener = RecordingVitoImageRequestListener("local")
    val controllerListener = RecordingControllerListener()
    val request = createImageRequest()

    subject.setVitoImageRequestListener(globalVitoListener)
    subject.setLocalVitoImageRequestListener(localVitoListener)
    subject.setControllerListener2(controllerListener)
    subject.onReset(
        resetVitoImageRequestListener = true,
        resetLocalVitoImageRequestListener = true,
        resetLocalImagePerfStateListener = true,
        resetControllerListener2 = true,
    )
    subject.onRelease(7L, request, null)

    assertEquals(emptyList<String>(), globalVitoListener.events)
    assertEquals(emptyList<String>(), localVitoListener.events)
    assertEquals(emptyList<String>(), controllerListener.events)
  }

  @Test
  fun setLocalImagePerfStateListener_withoutNotifierHolderThrows() {
    val subject = CombinedImageListenerImpl()

    subject.setImagePerfLoggingListener(LoggingListenerWithoutNotifierHolder())

    assertThrows(NullPointerException::class.java) {
      subject.setLocalImagePerfStateListener(RecordingImagePerfNotifier())
    }
  }

  @Test
  fun setLocalImagePerfStateListener_withNotifierHolderPublishesNotifier() {
    val subject = CombinedImageListenerImpl()
    val perfListener = RecordingImagePerfLoggingListener()
    val notifier = RecordingImagePerfNotifier()

    subject.setImagePerfLoggingListener(perfListener)
    subject.setLocalImagePerfStateListener(notifier)

    assertSame(notifier, perfListener.notifier)
  }

  private fun createImageRequest(): VitoImageRequest = VitoImageRequest(
      RuntimeEnvironment.getApplication().resources,
      TestImageSource,
      ImageOptions.defaults(),
      false,
      null,
      null,
  )

  private object TestImageSource : ImageSource {
    override fun getClassNameString(): String = "test"
  }

  private class RecordingVitoImageRequestListener(private val name: String) :
      VitoImageRequestListener {

    val events = mutableListOf<String>()
    val finalImageRequests = mutableListOf<VitoImageRequest>()
    val releaseRequests = mutableListOf<VitoImageRequest>()

    override fun onSubmit(
        id: Long,
        imageRequest: VitoImageRequest,
        callerContext: Any?,
        extras: ControllerListener2.Extras?,
    ) {
      events.add("$name:submit:$id:${callerContext != null}:${extras != null}")
    }

    override fun onPlaceholderSet(
        id: Long,
        imageRequest: VitoImageRequest,
        placeholder: Drawable?,
    ) {
      events.add("$name:placeholder:$id:${placeholder != null}")
    }

    override fun onFinalImageSet(
        id: Long,
        imageRequest: VitoImageRequest,
        imageOrigin: Int,
        imageInfo: ImageInfo?,
        extras: ControllerListener2.Extras?,
        drawable: Drawable?,
    ) {
      finalImageRequests.add(imageRequest)
      events.add("$name:final:$id:$imageOrigin:${extras != null}:${drawable != null}")
    }

    override fun onIntermediateImageSet(
        id: Long,
        imageRequest: VitoImageRequest,
        imageInfo: ImageInfo?,
    ) {
      events.add("$name:intermediate:$id:${imageInfo != null}")
    }

    override fun onIntermediateImageFailed(
        id: Long,
        imageRequest: VitoImageRequest,
        throwable: Throwable?,
    ) {
      events.add("$name:intermediateFailure:$id:${throwable != null}")
    }

    override fun onFailure(
        id: Long,
        imageRequest: VitoImageRequest,
        error: Drawable?,
        throwable: Throwable?,
        extras: ControllerListener2.Extras?,
    ) {
      events.add("$name:failure:$id:${error != null}:${throwable != null}:${extras != null}")
    }

    override fun onRelease(
        id: Long,
        imageRequest: VitoImageRequest,
        extras: ControllerListener2.Extras?,
    ) {
      releaseRequests.add(imageRequest)
      events.add("$name:release:$id:${extras == null}")
    }

    override fun onEmptyEvent(callerContext: Any?) {
      events.add("$name:empty:${callerContext != null}")
    }
  }

  private class RecordingImageListener : ImageListener {

    val events = mutableListOf<String>()

    override fun onSubmit(id: Long, callerContext: Any?) {
      events.add("image:submit:$id:${callerContext != null}")
    }

    override fun onPlaceholderSet(id: Long, placeholder: Drawable?) {
      events.add("image:placeholder:$id:${placeholder != null}")
    }

    override fun onFinalImageSet(
        id: Long,
        imageOrigin: Int,
        imageInfo: ImageInfo?,
        drawable: Drawable?,
    ) {
      events.add("image:final:$id:$imageOrigin:${drawable != null}")
    }

    override fun onRelease(id: Long) {
      events.add("image:release:$id")
    }

    override fun onImageDrawn(id: String, imageInfo: ImageInfo, dimensionsInfo: DimensionsInfo) =
        Unit
  }

  private class RecordingControllerListener : ControllerListener2<ImageInfo> {

    val events = mutableListOf<String>()

    override fun onSubmit(
        id: String,
        callerContext: Any?,
        extraData: ControllerListener2.Extras?,
    ) {
      events.add("controller:submit:$id:${callerContext != null}:${extraData != null}")
    }

    override fun onFinalImageSet(
        id: String,
        imageInfo: ImageInfo?,
        extraData: ControllerListener2.Extras?,
    ) {
      events.add("controller:final:$id:${extraData != null}")
    }

    override fun onIntermediateImageSet(id: String, imageInfo: ImageInfo?) {
      events.add("controller:intermediate:$id:${imageInfo != null}")
    }

    override fun onIntermediateImageFailed(id: String) {
      events.add("controller:intermediateFailure:$id")
    }

    override fun onFailure(
        id: String,
        throwable: Throwable?,
        extraData: ControllerListener2.Extras?,
    ) {
      events.add("controller:failure:$id:${throwable != null}:${extraData != null}")
    }

    override fun onRelease(id: String, extraData: ControllerListener2.Extras?) {
      events.add("controller:release:$id:${extraData != null}")
    }

    override fun onEmptyEvent(callerContext: Any?) {
      events.add("controller:empty:${callerContext != null}")
    }
  }

  private class RecordingImagePerfLoggingListener :
      ImagePerfLoggingListener, ImagePerfNotifierHolder, Closeable {

    val events = mutableListOf<String>()
    var closed = false
    var notifier: ImagePerfNotifier? = null

    override fun setImagePerfNotifier(imagePerfNotifier: ImagePerfNotifier?) {
      notifier = imagePerfNotifier
    }

    override fun close() {
      closed = true
    }

    override fun reportVisible(visible: Boolean) = Unit

    override fun onSubmit(
        id: String,
        callerContext: Any?,
        extraData: ControllerListener2.Extras?,
    ) {
      events.add("perf:submit:$id:${callerContext != null}:${extraData != null}")
    }

    override fun onFinalImageSet(
        id: String,
        imageInfo: ImageInfo?,
        extraData: ControllerListener2.Extras?,
    ) {
      events.add("perf:final:$id:${extraData != null}")
    }

    override fun onIntermediateImageSet(id: String, imageInfo: ImageInfo?) {
      events.add("perf:intermediate:$id:${imageInfo != null}")
    }

    override fun onIntermediateImageFailed(id: String) {
      events.add("perf:intermediateFailure:$id")
    }

    override fun onFailure(
        id: String,
        throwable: Throwable?,
        extraData: ControllerListener2.Extras?,
    ) {
      events.add("perf:failure:$id:${throwable != null}:${extraData != null}")
    }

    override fun onRelease(id: String, extraData: ControllerListener2.Extras?) {
      events.add("perf:release:$id:${extraData != null}")
    }

    override fun onEmptyEvent(callerContext: Any?) {
      events.add("perf:empty:${callerContext != null}")
    }
  }

  private class LoggingListenerWithoutNotifierHolder : ImagePerfLoggingListener {

    override fun reportVisible(visible: Boolean) = Unit

    override fun onSubmit(
        id: String,
        callerContext: Any?,
        extraData: ControllerListener2.Extras?,
    ) = Unit

    override fun onFinalImageSet(
        id: String,
        imageInfo: ImageInfo?,
        extraData: ControllerListener2.Extras?,
    ) = Unit

    override fun onIntermediateImageSet(id: String, imageInfo: ImageInfo?) = Unit

    override fun onIntermediateImageFailed(id: String) = Unit

    override fun onFailure(
        id: String,
        throwable: Throwable?,
        extraData: ControllerListener2.Extras?,
    ) = Unit

    override fun onRelease(id: String, extraData: ControllerListener2.Extras?) = Unit

    override fun onEmptyEvent(callerContext: Any?) = Unit
  }

  private class RecordingImagePerfNotifier : ImagePerfNotifier {

    override fun notifyVisibilityUpdated(state: ImagePerfState, visibilityState: VisibilityState) =
        Unit

    override fun notifyStatusUpdated(state: ImagePerfState, imageLoadStatus: ImageLoadStatus) = Unit
  }
}
