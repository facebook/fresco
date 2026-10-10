/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import com.facebook.common.callercontext.ContextChain
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.AbstractDataSource
import com.facebook.datasource.DataSource
import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.urimod.Dimensions
import com.facebook.fresco.urimod.FetchStrategy
import com.facebook.fresco.vito.core.BaseVitoImageRequestListener
import com.facebook.fresco.vito.core.DefaultFrescoVitoConfig
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.ImageOptionsDrawableFactory
import com.facebook.fresco.vito.renderer.BitmapImageDataModel
import com.facebook.fresco.vito.renderer.ColorIntImageDataModel
import com.facebook.fresco.vito.source.EmptyImageSource
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.imagepipeline.image.BaseCloseableImage
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.image.ImmutableQualityInfo
import com.facebook.imagepipeline.listener.RequestListener
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BitmapPreparingImageFetchSubscriberTest {
  private val ui = QueuedExecutor()
  private val worker = QueuedExecutor()
  private val source = ImageDataSource()
  private val drawable = KFrescoVitoDrawable()
  private lateinit var requestListener: BaseVitoImageRequestListener
  private var finalCalls = 0
  private var intermediateCalls = 0
  private var failureCalls = 0
  private var intermediateFailureCalls = 0
  private var finalCallbackException: RuntimeException? = null
  private var failureCause: Throwable? = null
  private var mappings = 0
  private var expectWorkerMapping = true
  private var failMapping = false
  private var nullMapping = false

  @Before
  fun setUp() {
    drawable._imageId = 1L
    drawable.imageRequest =
        VitoImageRequest(
            RuntimeEnvironment.getApplication().resources,
            EmptyImageSource("test"),
            ImageOptions.create().errorColor(Color.BLUE).build(),
            finalImageRequest = null,
            finalImageCacheKey = null,
        )
    requestListener =
        object : BaseVitoImageRequestListener() {
          override fun onFinalImageSet(
              id: Long,
              imageRequest: VitoImageRequest,
              imageOrigin: Int,
              imageInfo: ImageInfo?,
              extras: ControllerListener2.Extras?,
              drawable: Drawable?,
          ) {
            assertThat(Looper.myLooper()).isSameAs(Looper.getMainLooper())
            finalCalls++
            finalCallbackException?.let { throw it }
          }

          override fun onIntermediateImageSet(
              id: Long,
              imageRequest: VitoImageRequest,
              imageInfo: ImageInfo?,
          ) {
            assertThat(Looper.myLooper()).isSameAs(Looper.getMainLooper())
            intermediateCalls++
          }

          override fun onIntermediateImageFailed(
              id: Long,
              imageRequest: VitoImageRequest,
              throwable: Throwable?,
          ) {
            assertThat(Looper.myLooper()).isSameAs(Looper.getMainLooper())
            intermediateFailureCalls++
            failureCause = throwable
          }

          override fun onFailure(
              id: Long,
              imageRequest: VitoImageRequest,
              error: Drawable?,
              throwable: Throwable?,
              extras: ControllerListener2.Extras?,
          ) {
            assertThat(Looper.myLooper()).isSameAs(Looper.getMainLooper())
            failureCalls++
            failureCause = throwable
          }
        }
    drawable.listenerManager.setLocalVitoImageRequestListener(requestListener)
  }

  @After
  fun tearDown() {
    worker.reject = false
    ui.reject = false
    source.close()
    worker.drain()
    ui.drain()
    drawable.reset()
  }

  private fun subscribe(background: Boolean = true) {
    source.subscribe(
        BitmapPreparingImageFetchSubscriber(
            1L,
            drawable,
            { _, image, _ ->
              assertThat(worker.running).isEqualTo(expectWorkerMapping)
              if (!expectWorkerMapping) {
                assertThat(ui.running).isTrue()
              }
              mappings++
              check(!failMapping) { "mapping failed" }
              if (nullMapping) {
                null
              } else if (image is CloseableBitmap) {
                BitmapImageDataModel(checkNotNull(image.underlyingBitmap))
              } else {
                ColorIntImageDataModel(Color.RED)
              }
            },
            uiThreadExecutor = ui,
            preparationExecutor = if (background) worker else null,
        ),
        ui,
    )
  }

  private fun bitmapImage(): CloseableStaticBitmap =
      CloseableStaticBitmap.of(
          Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888),
          {},
          ImmutableQualityInfo.FULL_QUALITY,
          0,
      )

  @Test
  fun bitmapIsPreparedOnWorkerAndAppliedOnUiWithReferenceAlive() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    source.close()
    assertThat(image.isClosed).isFalse()
    assertThat(drawable.hasImage()).isFalse()

    worker.runNext()
    assertThat(mappings).isEqualTo(1)
    assertThat(drawable.hasImage()).isFalse()
    assertThat(finalCalls).isZero()

    ui.runNext()
    assertThat(drawable.hasImage()).isTrue()
    assertThat(finalCalls).isEqualTo(1)
    assertThat(image.isClosed).isFalse()
    drawable.reset()
    assertThat(image.isClosed).isTrue()
  }

  @Test
  fun resetBetweenPreparationAndApplicationClosesPendingReference() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    worker.runNext()
    source.close()
    drawable.reset()

    ui.runNext()

    assertThat(image.isClosed).isTrue()
    assertThat(drawable.hasImage()).isFalse()
    assertThat(finalCalls).isZero()
  }

  @Test
  fun newerResultWinsWhenWorkersFinishOutOfOrder() {
    subscribe()
    val first = bitmapImage()
    val last = bitmapImage()
    source.emit(first, false)
    ui.runNext()
    source.emit(last, true)
    ui.runNext()
    source.close()

    worker.runNext(1)
    ui.runNext()
    worker.runNext()
    ui.runNext()

    assertThat(first.isClosed).isTrue()
    assertThat((drawable.actualImageLayer.getDataModel() as BitmapImageDataModel).bitmap)
        .isSameAs(last.underlyingBitmap)
    assertThat(finalCalls).isEqualTo(1)
    assertThat(intermediateCalls).isZero()
  }

  @Test
  fun failurePreventsPendingIntermediateResultFromReplacingError() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, false)
    ui.runNext()
    source.fail()
    ui.runNext()
    source.close()
    worker.runNext()
    ui.runNext()

    assertThat(failureCalls).isEqualTo(1)
    assertThat(finalCalls).isZero()
    assertThat(intermediateCalls).isZero()
    assertThat((drawable.actualImageLayer.getDataModel() as ColorIntImageDataModel).colorInt)
        .isEqualTo(Color.BLUE)
    assertThat(image.isClosed).isTrue()
  }

  @Test
  fun cancellationDiscardsPendingResult() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, false)
    ui.runNext()
    source.close()
    ui.runNext()
    worker.runNext()
    ui.runNext()

    assertThat(image.isClosed).isTrue()
    assertThat(drawable.hasImage()).isFalse()
    assertThat(intermediateCalls).isZero()
  }

  @Test
  fun disabledBackgroundPreparationAppliesSynchronouslyOnUi() {
    expectWorkerMapping = false
    subscribe(background = false)
    source.emit(bitmapImage(), true)
    ui.runNext()

    assertThat(drawable.hasImage()).isTrue()
    assertThat(finalCalls).isEqualTo(1)
    assertThat(worker.tasks).isEmpty()
  }

  @Test
  fun nonBitmapResultStaysOnUi() {
    expectWorkerMapping = false
    subscribe()
    source.emit(FakeCloseableImage(), true)
    ui.runNext()

    assertThat(drawable.hasImage()).isTrue()
    assertThat(finalCalls).isEqualTo(1)
    assertThat(worker.tasks).isEmpty()
  }

  @Test
  fun customDrawableFactoryStaysOnUiEvenForBitmap() {
    expectWorkerMapping = false
    val request = checkNotNull(drawable.imageRequest)
    drawable.imageRequest =
        VitoImageRequest(
            request.resources,
            request.imageSource,
            ImageOptions.create()
                .customDrawableFactory(
                    object : ImageOptionsDrawableFactory {
                      override fun createDrawable(
                          resources: Resources,
                          closeableImage: CloseableImage,
                          imageOptions: ImageOptions,
                      ): Drawable = ColorDrawable(Color.RED)
                    },
                )
                .build(),
            finalImageRequest = null,
            finalImageCacheKey = null,
        )
    subscribe()
    source.emit(bitmapImage(), true)
    ui.runNext()

    assertThat(finalCalls).isEqualTo(1)
    assertThat(worker.tasks).isEmpty()
  }

  @Test
  fun rejectedPreparationNotifiesFailureOnUiAndClosesReference() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    worker.reject = true
    ui.runNext()
    source.close()

    assertThat(failureCause).isInstanceOf(RejectedExecutionException::class.java)
    assertThat(failureCalls).isEqualTo(1)
    assertThat(finalCalls).isZero()
    assertThat(image.isClosed).isTrue()
  }

  @Test
  fun rejectedUiApplicationNotifiesFailureOnMainAndClosesReference() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    ui.reject = true
    worker.runNext()
    source.close()
    assertThat(image.isClosed).isTrue()
    assertThat(failureCalls).isZero()
    shadowOf(Looper.getMainLooper()).idle()

    assertThat(failureCause).isInstanceOf(RejectedExecutionException::class.java)
    assertThat(failureCalls).isEqualTo(1)
    assertThat(finalCalls).isZero()
  }

  @Test
  fun rejectedUiHandoffFromRealWorkerDeliversFailureOnMain() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    ui.reject = true
    val workerFailure = AtomicReference<Throwable?>()
    val preparationThread = Thread {
      workerFailure.set(runCatching { worker.runNext() }.exceptionOrNull())
    }
    preparationThread.start()
    preparationThread.join(5000)

    assertThat(preparationThread.isAlive).isFalse()
    assertThat(workerFailure.get()).isNull()
    source.close()
    assertThat(image.isClosed).isTrue()
    assertThat(failureCalls).isZero()
    shadowOf(Looper.getMainLooper()).idle()

    assertThat(failureCalls).isEqualTo(1)
    assertThat(failureCause).isInstanceOf(RejectedExecutionException::class.java)
    assertThat(finalCalls).isZero()
  }

  @Test
  fun rejectedUiHandoffFailureIsDiscardedAfterRebind() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    ui.reject = true
    worker.runNext()
    source.close()
    rebindDrawable()
    shadowOf(Looper.getMainLooper()).idle()

    assertThat(image.isClosed).isTrue()
    assertThat(failureCalls).isZero()
    assertThat(drawable.placeholderLayer.getAlpha()).isEqualTo(255)
  }

  @Test
  fun rejectedIntermediateUiHandoffNotifiesIntermediateFailure() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, false)
    ui.runNext()
    ui.reject = true
    worker.runNext()
    shadowOf(Looper.getMainLooper()).idle()
    ui.reject = false
    source.close()

    assertThat(image.isClosed).isTrue()
    assertThat(failureCause).isInstanceOf(RejectedExecutionException::class.java)
    assertThat(intermediateFailureCalls).isEqualTo(1)
    assertThat(failureCalls).isZero()
  }

  @Test
  fun inlineListenerRejectionIsNotMistakenForExecutorRejection() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    ui.inline = true
    val exception = RejectedExecutionException("listener failed")
    finalCallbackException = exception
    val failure = runCatching { worker.runNext() }.exceptionOrNull()
    source.close()
    shadowOf(Looper.getMainLooper()).idle()

    assertThat(failure).isSameAs(exception)
    assertThat(finalCalls).isEqualTo(1)
    assertThat(failureCalls).isZero()
    assertThat(image.isClosed).isFalse()
    drawable.reset()
    assertThat(image.isClosed).isTrue()
  }

  @Test
  fun failedPreparationNotifiesFailureOnUiAndClosesReference() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    failMapping = true
    worker.runNext()
    ui.runNext()
    source.close()

    assertThat(image.isClosed).isTrue()
    assertThat(finalCalls).isZero()
    assertThat(failureCalls).isEqualTo(1)
    assertThat(failureCause?.message).isEqualTo("mapping failed")
    assertThat((drawable.actualImageLayer.getDataModel() as ColorIntImageDataModel).colorInt)
        .isEqualTo(Color.BLUE)
  }

  @Test
  fun failedIntermediatePreparationNotifiesIntermediateFailure() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, false)
    ui.runNext()
    failMapping = true
    worker.runNext()
    ui.runNext()
    source.close()
    ui.drain()

    assertThat(image.isClosed).isTrue()
    assertThat(intermediateFailureCalls).isEqualTo(1)
    assertThat(failureCalls).isZero()
  }

  @Test
  fun obsoletePreparationFailureDoesNotFailNewerResult() {
    subscribe()
    val first = bitmapImage()
    source.emit(first, false)
    ui.runNext()
    source.emit(bitmapImage(), true)
    ui.runNext()
    failMapping = true
    worker.runNext()
    ui.runNext()
    failMapping = false
    worker.runNext()
    ui.runNext()
    source.close()

    assertThat(first.isClosed).isTrue()
    assertThat(finalCalls).isEqualTo(1)
    assertThat(failureCalls).isZero()
    assertThat(intermediateFailureCalls).isZero()
  }

  @Test
  fun inlineUiListenerExceptionDoesNotCloseDrawableOwnedReference() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    ui.inline = true
    finalCallbackException = IllegalStateException("listener failed")

    val failure = runCatching { worker.runNext() }.exceptionOrNull()
    source.close()

    assertThat(failure).isInstanceOf(IllegalStateException::class.java)
    assertThat(finalCalls).isEqualTo(1)
    assertThat(image.isClosed).isFalse()
    drawable.reset()
    assertThat(image.isClosed).isTrue()
  }

  @Test
  fun listenerRejectionAfterRealWorkerPreparationPropagatesOnUiOnly() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    val exception = RejectedExecutionException("listener failed")
    finalCallbackException = exception
    val workerFailure = AtomicReference<Throwable?>()
    val preparationThread = Thread {
      workerFailure.set(runCatching { worker.runNext() }.exceptionOrNull())
    }
    preparationThread.start()
    preparationThread.join(5000)

    assertThat(preparationThread.isAlive).isFalse()
    assertThat(workerFailure.get()).isNull()
    assertThat(finalCalls).isZero()
    val uiFailure = runCatching { ui.runNext() }.exceptionOrNull()
    source.close()

    assertThat(uiFailure).isSameAs(exception)
    assertThat(finalCalls).isEqualTo(1)
    assertThat(failureCalls).isZero()
    assertThat(image.isClosed).isFalse()
    drawable.reset()
    assertThat(image.isClosed).isTrue()
  }

  @Test
  fun datasourceCallbackOnWorkerIsRejectedBeforeReadingDrawable() {
    val subscriber = BitmapPreparingImageFetchSubscriber(1L, drawable, { _, _, _ -> null })
    source.emit(bitmapImage(), true)
    val failure = AtomicReference<Throwable?>()
    val callbackThread = Thread {
      failure.set(runCatching { subscriber.onNewResult(source) }.exceptionOrNull())
    }
    callbackThread.start()
    callbackThread.join(5000)

    assertThat(callbackThread.isAlive).isFalse()
    assertThat(failure.get()).isInstanceOf(IllegalStateException::class.java)
    assertThat(drawable.hasImage()).isFalse()
    assertThat(finalCalls).isZero()
  }

  @Test
  fun nullModelNotifiesFailureRatherThanSuccess() {
    subscribe()
    val image = bitmapImage()
    source.emit(image, true)
    ui.runNext()
    nullMapping = true
    worker.runNext()
    ui.runNext()
    source.close()

    assertThat(finalCalls).isZero()
    assertThat(failureCalls).isEqualTo(1)
    assertThat(image.isClosed).isTrue()
  }

  @Test
  fun immediateFadeCallbackRebindStopsOldApplicationAndRetainsLocalImage() {
    subscribe()
    val image = bitmapImage()
    rebindFromFadeCallback {
      source.close()
      rebindDrawable()
      assertThat(image.isClosed).isFalse()
    }
    source.emit(image, true)
    ui.runNext()
    worker.runNext()
    ui.runNext()

    assertThat(finalCalls).isZero()
    assertThat(image.isClosed).isTrue()
    assertThat(drawable.placeholderLayer.getAlpha()).isEqualTo(255)
  }

  @Test
  fun fadeStartedCallbackRebindStopsOldAnimationAndNotification() {
    val request = checkNotNull(drawable.imageRequest)
    drawable.imageRequest =
        VitoImageRequest(
            request.resources,
            request.imageSource,
            ImageOptions.create().fadeDurationMs(100).build(),
            finalImageRequest = null,
            finalImageCacheKey = null,
        )
    subscribe()
    val image = bitmapImage()
    rebindFromFadeCallback {
      source.close()
      rebindDrawable()
    }
    source.emit(image, true)
    ui.runNext()
    worker.runNext()
    ui.runNext()

    assertThat(finalCalls).isZero()
    assertThat(image.isClosed).isTrue()
    assertThat(drawable.placeholderLayer.getAlpha()).isEqualTo(255)
  }

  @Test
  fun failureFadeCallbackRebindStopsOldFailureNotification() {
    subscribe()
    val image = bitmapImage()
    rebindFromFadeCallback {
      source.close()
      rebindDrawable()
      assertThat(image.isClosed).isFalse()
    }
    source.emit(image, true)
    ui.runNext()
    failMapping = true
    worker.runNext()
    ui.runNext()

    assertThat(failureCalls).isZero()
    assertThat(image.isClosed).isTrue()
    assertThat(drawable.placeholderLayer.getAlpha()).isEqualTo(255)
  }

  private fun rebindFromFadeCallback(onFade: () -> Unit) {
    drawable.onFadeListener =
        object : OnFadeListener {
          override fun onFadeStarted() = onFade()

          override fun onShownImmediately() = onFade()

          override fun onFadeFinished() = Unit
        }
  }

  private fun rebindDrawable() {
    val request = checkNotNull(drawable.imageRequest)
    drawable.reset()
    drawable._imageId = 2L
    drawable.imageRequest = request
    drawable.placeholderLayer.setPlaceholder(
        request.resources,
        ImageOptions.create().placeholderColor(Color.GREEN).build(),
    )
    drawable.placeholderLayer.fadeIn(0)
  }

  @Test
  fun controllerGateOffUsesOriginalUiDeliveryWithoutPreparation() {
    fetchThroughController(background = false)
    source.emit(bitmapImage(), true)

    ui.runNext()

    assertThat(drawable.hasImage()).isTrue()
    assertThat(finalCalls).isEqualTo(1)
    assertThat(worker.tasks).isEmpty()
  }

  @Test
  fun controllerGateOnPreparesBeforeApplyingOnUi() {
    fetchThroughController(background = true)
    source.emit(bitmapImage(), true)
    ui.runNext()
    assertThat(drawable.hasImage()).isFalse()
    assertThat(finalCalls).isZero()

    worker.runNext()
    assertThat(drawable.hasImage()).isFalse()
    ui.runNext()

    assertThat(drawable.hasImage()).isTrue()
    assertThat(finalCalls).isEqualTo(1)
  }

  private fun fetchThroughController(background: Boolean) {
    val request = checkNotNull(drawable.imageRequest)
    drawable.imageRequest = null
    val pipeline =
        object : VitoImagePipeline {
          override fun createImageRequest(
              resources: Resources,
              imageSource: ImageSource,
              options: ImageOptions?,
              logWithHighSamplingRate: Boolean,
              viewport: Rect?,
              callerContext: Any?,
              contextChain: ContextChain?,
              fetchStrategy: FetchStrategy?,
          ): VitoImageRequest = request

          override fun getCachedImage(
              imageRequest: VitoImageRequest,
          ): CloseableReference<CloseableImage>? = null

          override fun fetchDecodedImage(
              imageRequest: VitoImageRequest,
              callerContext: Any?,
              requestListener: RequestListener?,
              uiComponentId: Long,
              viewport: Dimensions?,
          ): DataSource<CloseableReference<CloseableImage>> = source

          override fun isInDiskCacheSync(imageRequest: VitoImageRequest): Boolean = false
        }
    val controller = KFrescoController(
        object : DefaultFrescoVitoConfig() {
          override fun handleImageResultInBackground(): Boolean = background
        },
        pipeline,
        ui,
        worker,
    )
    ui.execute {
      controller.fetch(
          drawable,
          request,
          callerContext = "test",
          contextChain = null,
          listener = null,
          onFadeListener = null,
          viewportDimensions = null,
          vitoImageRequestListener = requestListener,
      )
    }
    ui.runNext()
    worker.runNext()
  }

  private class QueuedExecutor : Executor {
    val tasks = mutableListOf<Runnable>()
    var running = false
    var reject = false
    var inline = false

    override fun execute(command: Runnable) {
      if (reject) {
        throw RejectedExecutionException()
      }
      tasks.add(command)
      if (inline) {
        runNext()
      }
    }

    fun runNext(index: Int = 0) {
      val task = tasks.removeAt(index)
      running = true
      try {
        task.run()
      } finally {
        running = false
      }
    }

    fun drain() {
      while (tasks.isNotEmpty()) {
        runNext()
      }
    }
  }

  private class ImageDataSource : AbstractDataSource<CloseableReference<CloseableImage>>() {
    @Synchronized
    override fun getResult(): CloseableReference<CloseableImage>? =
        CloseableReference.cloneOrNull(super.getResult())

    override fun closeResult(result: CloseableReference<CloseableImage>?) {
      CloseableReference.closeSafely(result)
    }

    fun emit(image: CloseableImage, finished: Boolean) {
      setResult(CloseableReference.of(image), finished, mapOf("origin" to "network"))
    }

    fun fail() {
      setFailure(IllegalStateException("fetch failed"))
    }
  }

  private class FakeCloseableImage : BaseCloseableImage() {
    private var closed = false

    override fun getSizeInBytes(): Int = 0

    override fun getWidth(): Int = 10

    override fun getHeight(): Int = 10

    override fun isClosed(): Boolean = closed

    override fun close() {
      closed = true
    }
  }
}
