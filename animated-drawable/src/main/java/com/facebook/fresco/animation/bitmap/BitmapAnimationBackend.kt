/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import com.facebook.common.logging.FLog
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck.InactivityListener
import com.facebook.fresco.animation.backend.AnimationInformation
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparationStrategy
import com.facebook.fresco.animation.bitmap.preparation.BitmapFramePreparer
import com.facebook.fresco.ui.common.DimensionsInfo
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.AnimatedOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.secure.uriparser.SecureUriParser

/**
 * Bitmap animation backend that renders bitmap frames.
 *
 * The given [BitmapFrameCache] is used to cache frames and create new bitmaps.
 * [AnimationInformation] defines the main animation parameters, like frame and loop count.
 * [BitmapFrameRenderer] is used to render frames to the bitmaps acquired from the
 * [BitmapFrameCache].
 */
class BitmapAnimationBackend
@JvmOverloads
constructor(
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val bitmapFrameCache: BitmapFrameCache,
    private val animationInformation: AnimationInformation,
    private val bitmapFrameRenderer: BitmapFrameRenderer,
    private val isNewRenderImplementation: Boolean,
    private val bitmapFramePreparationStrategy: BitmapFramePreparationStrategy?,
    private val bitmapFramePreparer: BitmapFramePreparer?,
    val roundingOptions: RoundingOptions? = null,
    val animatedOptions: AnimatedOptions? = null,
) : AnimationBackend, InactivityListener {

  private val isCircular: Boolean = roundingOptions?.isCircular == true
  private val isAntiAliased: Boolean = roundingOptions?.isAntiAliased == true

  val cornerRadii: FloatArray? =
      roundingOptions?.let { roundingOptions ->
        if (isCircular) {
          null
        } else if (roundingOptions.cornerRadius != RoundingOptions.CORNER_RADIUS_UNSET) {
          val corners = FloatArray(8)
          corners.fill(roundingOptions.cornerRadius)
          corners
        } else {
          roundingOptions.cornerRadii
        }
      }

  interface FrameListener {
    /**
     * Called when the backend started drawing the given frame.
     *
     * @param backend the backend
     * @param frameNumber the frame number to be drawn
     */
    fun onDrawFrameStart(backend: BitmapAnimationBackend, frameNumber: Int)

    /**
     * Called when the given frame has been drawn.
     *
     * @param backend the backend
     * @param frameNumber the frame number that has been drawn
     * @param frameType the [FrameType] that has been drawn
     */
    fun onFrameDrawn(backend: BitmapAnimationBackend, frameNumber: Int, @FrameType frameType: Int)

    /**
     * Called when no bitmap could be drawn by the backend for the given frame number.
     *
     * @param backend the backend
     * @param frameNumber the frame number that could not be drawn
     */
    fun onFrameDropped(backend: BitmapAnimationBackend, frameNumber: Int)
  }

  /** Frame type that has been drawn. Can be used for logging. */
  @Retention(AnnotationRetention.SOURCE)
  @IntDef(
      FRAME_TYPE_UNKNOWN,
      FRAME_TYPE_CACHED,
      FRAME_TYPE_REUSED,
      FRAME_TYPE_CREATED,
      FRAME_TYPE_FALLBACK,
  )
  annotation class FrameType

  private val bitmapConfig = Bitmap.Config.ARGB_8888

  private val paint: Paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
  private var bounds: Rect? = null
  private var bitmapWidth = 0
  private var bitmapHeight = 0

  private val path: Path = Path()
  private val matrix: Matrix = Matrix()
  private var pathFrameNumber: Int = -1

  private var frameListener: FrameListener? = null
  private var animationListener: AnimationBackend.Listener? = null

  // Thumbnail fallback functionality
  private var thumbnailDrawable: FrescoDrawableInterface? = null
  private var animationCompleted: Boolean = false
  private var totalFramesProcessed: Int = 0
  private var lastFrameNumber: Int = -1

  init {
    initializeThumbnailDrawable()
    updateBitmapDimensions()
  }

  fun setFrameListener(frameListener: FrameListener?) {
    this.frameListener = frameListener
  }

  override fun getFrameCount(): Int = animationInformation.frameCount

  override fun getFrameDurationMs(frameNumber: Int): Int =
      animationInformation.getFrameDurationMs(frameNumber)

  override fun width(): Int = animationInformation.width()

  override fun height(): Int = animationInformation.height()

  override fun getLoopDurationMs(): Int = animationInformation.loopDurationMs

  override fun getLoopCount(): Int {
    // If no animated options are set, use the default loop count
    if (animatedOptions == null) {
      return animationInformation.loopCount
    }

    return when (animatedOptions.loopCount) {
      AnimatedOptions.LOOP_COUNT_INFINITE -> AnimationInformation.LOOP_COUNT_INFINITE
      AnimatedOptions.LOOP_COUNT_STATIC -> 1
      else -> animatedOptions.loopCount
    }
  }

  override fun drawFrame(parent: Drawable, canvas: Canvas, frameNumber: Int): Boolean {
    frameListener?.onDrawFrameStart(this, frameNumber)

    // Check if we should show thumbnail instead of animation frame
    if (showThumbnail()) {
      val thumbnailDrawn = drawThumbnail(canvas)
      if (thumbnailDrawn) {
        return true
      }
    }

    val drawn = drawFrameOrFallback(canvas, frameNumber, FRAME_TYPE_CACHED)

    // Track animation progress for thumbnail fallback
    trackAnimationProgress(frameNumber)

    // We could not draw anything
    if (!drawn) {
      frameListener?.onFrameDropped(this, frameNumber)
    }

    // Prepare next frames
    if (!isNewRenderImplementation && bitmapFramePreparer != null) {
      bitmapFramePreparationStrategy?.prepareFrames(
          bitmapFramePreparer,
          bitmapFrameCache,
          this,
          frameNumber,
      )
    }
    return drawn
  }

  private fun drawFrameOrFallback(
      canvas: Canvas,
      frameNumber: Int,
      @FrameType frameType: Int,
  ): Boolean {
    var bitmapReference: CloseableReference<Bitmap>? = null
    val drawn: Boolean
    var nextFrameType = FRAME_TYPE_UNKNOWN

    try {
      if (isNewRenderImplementation) {
        bitmapReference =
            bitmapFramePreparationStrategy?.getBitmapFrame(frameNumber, canvas.width, canvas.height)

        if (bitmapReference != null && bitmapReference.isValid) {
          drawBitmap(frameNumber, bitmapReference.get(), canvas)
          return true
        }

        // If bitmap could not be drawn, then fetch frames
        bitmapFramePreparationStrategy?.prepareFrames(canvas.width, canvas.height, null)
        return false
      }

      when (frameType) {
        FRAME_TYPE_CACHED -> {
          bitmapReference = bitmapFrameCache.getCachedFrame(frameNumber)
          drawn = drawBitmapAndCache(frameNumber, bitmapReference, canvas, FRAME_TYPE_CACHED)
          nextFrameType = FRAME_TYPE_REUSED
        }
        FRAME_TYPE_REUSED -> {
          bitmapReference =
              bitmapFrameCache.getBitmapToReuseForFrame(frameNumber, bitmapWidth, bitmapHeight)
          // Try to render the frame and draw on the canvas immediately after
          drawn =
              (renderFrameInBitmap(frameNumber, bitmapReference) &&
                  drawBitmapAndCache(frameNumber, bitmapReference, canvas, FRAME_TYPE_REUSED))
          nextFrameType = FRAME_TYPE_CREATED
        }
        FRAME_TYPE_CREATED -> {
          bitmapReference =
              try {
                platformBitmapFactory.createBitmap(bitmapWidth, bitmapHeight, bitmapConfig)
              } catch (e: RuntimeException) {
                // Failed to create the bitmap for the frame, return and report that we could not
                // draw the frame.
                FLog.w(TAG, "Failed to create frame bitmap", e)
                return false
              }
          // Try to render the frame and draw on the canvas immediately after
          drawn =
              (renderFrameInBitmap(frameNumber, bitmapReference) &&
                  drawBitmapAndCache(frameNumber, bitmapReference, canvas, FRAME_TYPE_CREATED))
          nextFrameType = FRAME_TYPE_FALLBACK
        }
        FRAME_TYPE_FALLBACK -> {
          bitmapReference = bitmapFrameCache.getFallbackFrame(frameNumber)
          drawn = drawBitmapAndCache(frameNumber, bitmapReference, canvas, FRAME_TYPE_FALLBACK)
        }
        else -> return false
      }
    } finally {
      CloseableReference.closeSafely(bitmapReference)
    }
    return if (drawn || nextFrameType == FRAME_TYPE_UNKNOWN) {
      drawn
    } else {
      drawFrameOrFallback(canvas, frameNumber, nextFrameType)
    }
  }

  override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
    paint.alpha = alpha
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    paint.colorFilter = colorFilter
  }

  override fun setBounds(bounds: Rect?) {
    this.bounds = bounds
    bitmapFrameRenderer.setBounds(bounds)
    updateBitmapDimensions()

    // Set bounds on thumbnail drawable when backend bounds change
    thumbnailDrawable?.let { drawable ->
      val thumbnailBounds = bounds ?: Rect(0, 0, width(), height())
      (drawable as Drawable).setBounds(thumbnailBounds)
    }
  }

  override fun getIntrinsicWidth(): Int = bitmapWidth

  override fun getIntrinsicHeight(): Int = bitmapHeight

  override fun getSizeInBytes(): Int = bitmapFrameCache.sizeInBytes

  override fun clear() {
    if (isNewRenderImplementation) {
      bitmapFramePreparationStrategy?.clearFrames()
    } else {
      bitmapFrameCache.clear()
    }
  }

  override fun preloadAnimation() {
    if (!isNewRenderImplementation && bitmapFramePreparer != null) {
      bitmapFramePreparationStrategy?.prepareFrames(
          bitmapFramePreparer,
          bitmapFrameCache,
          this,
          0,
      ) {
        animationListener?.onAnimationLoaded()
      }
    } else {
      bitmapFramePreparationStrategy?.prepareFrames(
          animationInformation.width(),
          animationInformation.height(),
      ) {
        animationListener?.onAnimationLoaded()
      }
    }
  }

  override fun onInactive() {
    if (isNewRenderImplementation) {
      bitmapFramePreparationStrategy?.onStop()
    } else {
      clear()
    }
    thumbnailDrawable?.let { FrescoVitoProvider.getController().releaseImmediately(it) }
    thumbnailDrawable = null
  }

  override fun setAnimationListener(listener: AnimationBackend.Listener?) {
    animationListener = listener
  }

  private fun updateBitmapDimensions() {
    // Calculate the correct bitmap dimensions
    bitmapWidth = bitmapFrameRenderer.intrinsicWidth
    if (bitmapWidth == AnimationBackend.INTRINSIC_DIMENSION_UNSET) {
      bitmapWidth = bounds?.width() ?: AnimationBackend.INTRINSIC_DIMENSION_UNSET
    }
    bitmapHeight = bitmapFrameRenderer.intrinsicHeight
    if (bitmapHeight == AnimationBackend.INTRINSIC_DIMENSION_UNSET) {
      bitmapHeight = bounds?.height() ?: AnimationBackend.INTRINSIC_DIMENSION_UNSET
    }
  }

  /**
   * Try to render the frame to the given target bitmap. If the rendering fails, the target bitmap
   * reference will be closed and false is returned. If rendering succeeds, the target bitmap
   * reference can be drawn and has to be manually closed after drawing has been completed.
   *
   * @param frameNumber the frame number to render
   * @param targetBitmap the target bitmap
   * @return true if rendering successful
   */
  private fun renderFrameInBitmap(
      frameNumber: Int,
      targetBitmap: CloseableReference<Bitmap>?,
  ): Boolean {
    if (targetBitmap == null || !targetBitmap.isValid) {
      return false
    }
    // Render the image
    val frameRendered = bitmapFrameRenderer.renderFrame(frameNumber, targetBitmap.get())
    if (!frameRendered) {
      CloseableReference.closeSafely(targetBitmap)
    }
    return frameRendered
  }

  private fun updatePath(
      frameNumber: Int,
      bitmap: Bitmap,
      currentBoundsWidth: Float,
      currentBoundsHeight: Float,
  ): Boolean {
    if (!isCircular && cornerRadii == null) {
      return false
    }
    if (frameNumber == pathFrameNumber) {
      return true
    }

    val bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val src = RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())
    val dst = RectF(0f, 0f, currentBoundsWidth, currentBoundsHeight)
    matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL)
    bitmapShader.setLocalMatrix(matrix)
    paint.shader = bitmapShader
    paint.isAntiAlias = isAntiAliased

    path.reset()

    if (isCircular) {
      val centerX = currentBoundsWidth / 2f
      val centerY = currentBoundsHeight / 2f
      val radius = minOf(centerX, centerY)
      path.addCircle(centerX, centerY, radius, Path.Direction.CW)
    } else {
      path.addRoundRect(
          RectF(0f, 0f, currentBoundsWidth, currentBoundsHeight),
          cornerRadii ?: floatArrayOf(),
          Path.Direction.CW,
      )
    }

    pathFrameNumber = frameNumber
    return true
  }

  private fun drawBitmap(frameNumber: Int, bitmap: Bitmap, canvas: Canvas) {
    val currentBounds = bounds

    if (currentBounds == null) {
      canvas.drawBitmap(bitmap, 0f, 0f, paint)
    } else {
      if (updatePath(
          frameNumber,
          bitmap,
          currentBounds.width().toFloat(),
          currentBounds.height().toFloat(),
      )) {
        canvas.drawPath(path, paint)
      } else {
        canvas.drawBitmap(bitmap, null, currentBounds, paint)
      }
    }
  }

  /**
   * Helper method that draws the given bitmap on the canvas respecting the bounds (if set).
   *
   * If rendering was successful, it notifies the cache that the frame has been rendered with the
   * given bitmap. In addition, it will notify the [FrameListener] if set.
   *
   * @param frameNumber the current frame number passed to the cache
   * @param bitmapReference the bitmap to draw
   * @param canvas the canvas to draw an
   * @param frameType the [FrameType] to be rendered
   * @return true if the bitmap has been drawn
   */
  private fun drawBitmapAndCache(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>?,
      canvas: Canvas,
      @FrameType frameType: Int,
  ): Boolean {
    if (bitmapReference == null || !CloseableReference.isValid(bitmapReference)) {
      return false
    }

    this.drawBitmap(frameNumber, bitmapReference.get(), canvas)

    // Notify the cache that a frame has been rendered.
    // We should not cache fallback frames since they do not represent the actual frame.
    if (frameType != FRAME_TYPE_FALLBACK && !isNewRenderImplementation) {
      bitmapFrameCache.onFrameRendered(frameNumber, bitmapReference, frameType)
    }
    frameListener?.onFrameDrawn(this, frameNumber, frameType)

    return true
  }

  private fun initializeThumbnailDrawable() {
    val options = animatedOptions
    if (options?.useFallbackThumbnail() == true && !options.thumbnailUrl.isNullOrEmpty()) {
      try {
        thumbnailDrawable =
            FrescoVitoProvider.getController().createDrawable("bitmap-animation-thumbnail")

        // Load the thumbnail through Fresco's pipeline
        options.thumbnailUrl?.let { url -> loadFrescoThumbnail(url) }
      } catch (e: Exception) {
        FLog.w(TAG, "Failed to initialize thumbnail drawable", e)
        thumbnailDrawable = null
      }
    }
  }

  private fun loadFrescoThumbnail(thumbnailUrl: String) {
    val drawable = thumbnailDrawable ?: return

    try {
      val uri = SecureUriParser.parseEncodedRFC2396(thumbnailUrl)

      val imageOptions = ImageOptions.defaults().extend().round(roundingOptions).build()

      val imageRequest =
          FrescoVitoProvider.getImagePipeline()
              .createImageRequest(
                  Resources.getSystem(),
                  ImageSourceProvider.forUri(uri),
                  imageOptions,
                  callerContext = CALLER_CONTEXT,
              )

      val imageListener =
          object : ImageListener {
            override fun onFinalImageSet(
                id: Long,
                imageOrigin: Int,
                imageInfo: ImageInfo?,
                drawable: Drawable?,
            ) = Unit

            override fun onFailure(id: Long, error: Drawable?, throwable: Throwable?) {
              FLog.w(TAG, "Failed to load thumbnail from URL: $thumbnailUrl", throwable)
              thumbnailDrawable = null
            }

            override fun onImageDrawn(
                id: String,
                imageInfo: ImageInfo,
                dimensionsInfo: DimensionsInfo,
            ) {
              // Image drawn
            }
          }

      FrescoVitoProvider.getController()
          .fetch(
              drawable = drawable,
              imageRequest = imageRequest,
              callerContext = CALLER_CONTEXT,
              contextChain = null,
              listener = imageListener,
              onFadeListener = null,
              viewportDimensions = null,
          )
    } catch (e: Exception) {
      FLog.w(TAG, "Failed to load thumbnail through Fresco: $thumbnailUrl", e)
      // Release the image
      thumbnailDrawable?.let { FrescoVitoProvider.getController().releaseImmediately(it) }
      thumbnailDrawable = null
    }
  }

  private fun showThumbnail(): Boolean {
    val options = animatedOptions ?: return false

    // Only show thumbnail if we should use fallback thumbnail and we have a thumbnail drawable
    if (!options.useFallbackThumbnail() || thumbnailDrawable?.hasImage() != true) {
      return false
    }

    if (animationCompleted) {
      return true
    }
    return false
  }

  private fun drawThumbnail(canvas: Canvas): Boolean {
    val frescoDrawable = thumbnailDrawable ?: return false

    if (!frescoDrawable.hasImage()) {
      return false
    }

    try {
      val drawable = frescoDrawable as Drawable
      drawable.draw(canvas)
      return true
    } catch (e: Exception) {
      FLog.w(TAG, "Failed to draw thumbnail drawable", e)
      return false
    }
  }

  // Track animation progress for thumbnail fallback
  private fun trackAnimationProgress(frameNumber: Int) {
    val options = animatedOptions ?: return

    if (!options.useFallbackThumbnail()) {
      return
    }

    val totalLoops = getLoopCount()
    val framesPerLoop = getFrameCount()

    if (totalLoops == AnimationInformation.LOOP_COUNT_INFINITE) {
      return
    }

    if (frameNumber < lastFrameNumber) {
      totalFramesProcessed += framesPerLoop
    }
    lastFrameNumber = frameNumber

    // Calculate current loop and frame within loop
    val currentLoop = totalFramesProcessed / framesPerLoop
    val frameInLoop = frameNumber

    // Last loop reached, mark animation as completed
    if (currentLoop >= totalLoops - 1 && frameInLoop == framesPerLoop - 1) {
      animationCompleted = true
    }
  }

  companion object {
    const val FRAME_TYPE_UNKNOWN: Int = -1
    const val FRAME_TYPE_CACHED = 0
    const val FRAME_TYPE_REUSED = 1
    const val FRAME_TYPE_CREATED = 2
    const val FRAME_TYPE_FALLBACK = 3
    private val TAG = BitmapAnimationBackend::class.java
    private const val CALLER_CONTEXT = "BitmapAnimationBackend"
  }
}
