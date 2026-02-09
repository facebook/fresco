/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display.DEFAULT_DISPLAY
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.ColorInt
import com.facebook.common.internal.Supplier
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.impl.debug.StringDebugDataProvider
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.tools.liveeditor.ImageOptionsSampleValues.Entry
import com.facebook.fresco.vito.tools.liveeditor.LiveEditorUiUtils.Companion.dpToPx
import com.facebook.fresco.vito.tools.liveeditor.LiveEditorUiUtils.Companion.dpToPxF
import kotlin.math.abs

class LiveEditorOnScreenButtonController(
    private val isEnabled: Supplier<Boolean>,
    @ColorInt private val buttonTextColor: Int = Color.WHITE,
    @ColorInt private val buttonBackgroundColor: Int = Color.BLUE,
    @ColorInt private val editorBackgroundColor: Int = Color.WHITE,
    additionalButtonConfig: ButtonConfig? = null,
    private val customOptions: CustomOptions,
    private val debugDataProviders: List<StringDebugDataProvider> = emptyList(),
    private val showOverlayPermissionMessage: ((Context) -> Unit) = {
      Toast.makeText(
              it,
              "In order to use Image Live Editing, you must allow your app to 'Display over other apps' via Android settings",
              Toast.LENGTH_LONG,
          )
          .show()
    },
) {

  companion object {
    // Minimum movement in pixels to consider as a drag gesture (vs a tap)
    private const val DRAG_TOUCH_SLOP = 10
  }

  /** Called from fblite java code */
  constructor(
      isEnabled: Supplier<Boolean>,
      @ColorInt buttonTextColor: Int = Color.WHITE,
      @ColorInt buttonBackgroundColor: Int = Color.BLUE,
      @ColorInt editorBackgroundColor: Int = Color.WHITE,
      additionalButtonConfig: ButtonConfig? = null,
  ) : this(
      isEnabled,
      buttonTextColor,
      buttonBackgroundColor,
      editorBackgroundColor,
      additionalButtonConfig,
      CustomOptions(emptyList()),
  )

  class ButtonConfig(val title: String, val action: View.OnClickListener)

  class CustomOptions(val entries: List<Entry<out Any>>)

  private val buttons =
      listOfNotNull(
          ButtonConfig("Prev img") { imageSelector?.selectPrevious(it.context) },
          ButtonConfig("Next img") { imageSelector?.selectNext(it.context) },
          ButtonConfig("Edit img") { showLiveEditor(it.context) },
          ButtonConfig("Info") { showImageInfo(it.context) },
          additionalButtonConfig,
      )

  val imageTrackerListener: ImageTracker =
      object : ImageTracker() {
        private var isTracking = false

        override fun onImageBind(drawable: FrescoDrawableInterface) {
          ifEnabled {
            super.onImageBind(drawable)
            attachImageSelector(maybeGetContext(drawable))
          }
        }

        override fun onImageUnbind(drawable: FrescoDrawableInterface) {
          ifEnabled {
            super.onImageUnbind(drawable)
            if (drawableCount <= 0) {
              removeImageSelector()
            }
          }
        }

        private fun ifEnabled(body: () -> Unit) {
          if (isEnabled.get()) {
            body()
            isTracking = true
          } else if (isTracking) {
            removeImageSelector()
            super.reset()
            isTracking = false
          }
        }
      }

  var imageSelector: ImageSelector? = null

  private var isAttached: Boolean = false
  private var currentView: View? = null
  private var overlayPermissionStatus: Boolean? = null

  // Draggable overlay state
  private var currentLayoutParams: WindowManager.LayoutParams? = null
  private var lastButtonPositionX: Int? = null
  private var lastButtonPositionY: Int? = null

  private fun saveCurrentButtonPosition() {
    currentLayoutParams?.let { params ->
      lastButtonPositionX = params.x
      lastButtonPositionY = params.y
    }
  }

  fun attachImageSelector(context: Context?) {
    if (isAttached || context == null || !canShowOverlays(context)) {
      // already attached or no context available to get the WindowManager from
      return
    }
    isAttached = true
    imageSelector =
        ImageSelector(
            imageTrackerListener,
            FrescoVitoProvider.getImagePipeline(),
            FrescoVitoProvider.getController(),
        )
    showImageToggleButtons(context)
  }

  fun removeImageSelector() {
    if (!isAttached) {
      return
    }
    val current = currentView ?: return

    if (!canShowOverlays(current.context)) {
      return
    }

    val windowManager = current.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.removeView(current)
    currentView = null
    isAttached = false
  }

  private fun showImageToggleButtons(
      context: Context,
  ) {
    addWindow(context, createOnScreenButtons(context), isDraggable = true)
  }

  private fun showImageInfo(context: Context) {
    // Save current button position before showing the modal
    saveCurrentButtonPosition()
    val windowContext = getWindowContext(context)
    val infoView =
        LiveEditorUiUtils(imageSelector?.currentEditor, debugDataProviders)
            .createImageInfoView(windowContext) { showImageToggleButtons(windowContext) }
            .apply { background = ColorDrawable(editorBackgroundColor) }

    // Wrap in OverlayHandlerView so Flipper UI Debugger excludes it from inspection
    addWindow(windowContext, OverlayHandlerView(windowContext).apply { addView(infoView) })
  }

  private fun showLiveEditor(context: Context) {
    // Save current button position before showing the modal
    saveCurrentButtonPosition()
    val windowContext = getWindowContext(context)
    val editorView =
        LiveEditorUiUtils(imageSelector?.currentEditor, debugDataProviders)
            .createView(windowContext, customOptions.entries) {
              showImageToggleButtons(windowContext)
            }
            .apply { background = ColorDrawable(editorBackgroundColor) }

    // Wrap in OverlayHandlerView so Flipper UI Debugger excludes it from inspection
    val (_, screenHeight) = getScreenDimensions(getWindowManager(context))
    addWindow(
        windowContext,
        OverlayHandlerView(windowContext).apply { addView(editorView) },
        screenHeight / 2,
    )
  }

  private fun getWindowContext(context: Context): Context {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val primaryDisplay = displayManager.getDisplay(DEFAULT_DISPLAY)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      context
          .createDisplayContext(primaryDisplay)
          .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    } else {
      context.createDisplayContext(primaryDisplay)
    }
  }

  private fun getWindowManager(context: Context): WindowManager =
      context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  /** Returns the screen dimensions (width, height) using the modern API on Android R+. */
  private fun getScreenDimensions(windowManager: WindowManager): Pair<Int, Int> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val bounds = windowManager.currentWindowMetrics.bounds
      Pair(bounds.width(), bounds.height())
    } else {
      val displayMetrics = android.content.res.Resources.getSystem().displayMetrics
      Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
  }

  private fun addWindow(
      context: Context,
      view: View,
      height: Int = WindowManager.LayoutParams.WRAP_CONTENT,
      isDraggable: Boolean = false,
  ) {
    if (!isEnabled.get()) {
      return
    }
    val padding = 16.dpToPx(context)
    val windowManager = getWindowManager(context)
    currentView?.apply { windowManager.removeView(this) }

    // Check if we have a saved position to restore for draggable windows
    val savedX = lastButtonPositionX
    val savedY = lastButtonPositionY

    @Suppress("DEPRECATION")
    val windowType =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
          // TYPE_SYSTEM_ALERT is deprecated but required for API 21-25 (no alternative exists)
          WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

    val layoutParams =
        WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                height,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            )
            .apply {
              if (isDraggable && savedX != null && savedY != null) {
                // Use absolute positioning to restore saved position
                gravity = Gravity.TOP or Gravity.LEFT
                x = savedX
                y = savedY
              } else {
                // Use gravity-based positioning for initial placement
                // This automatically adapts to any button layout size
                gravity = Gravity.BOTTOM or Gravity.END
                x = padding
                y = padding
              }
            }
    windowManager.addView(view, layoutParams)
    currentView = view

    if (isDraggable) {
      currentLayoutParams = layoutParams

      // Once the view is laid out, switch to absolute positioning for dragging
      // Only needed when we don't have a saved position
      if (savedX == null || savedY == null) {
        view.post {
          // After layout, convert gravity-based position to absolute position
          // This ensures dragging works correctly from the initial position
          val (screenWidth, screenHeight) = getScreenDimensions(windowManager)

          // Calculate absolute position based on current gravity placement
          layoutParams.gravity = Gravity.TOP or Gravity.LEFT
          layoutParams.x = screenWidth - view.width - padding
          layoutParams.y = screenHeight - view.height - padding
          windowManager.updateViewLayout(view, layoutParams)
        }
      }
    }
  }

  /**
   * Custom ViewGroup class to wrap the live editor overlay buttons. Uses the "OverlayHandlerView"
   * naming pattern that Flipper's UI Debugger already recognizes and excludes from inspection,
   * preventing this overlay from blocking the main UI view hierarchy during debugging.
   */
  private open class OverlayHandlerView(context: Context) : LinearLayout(context)

  @SuppressLint("ClickableViewAccessibility")
  private fun createOnScreenButtons(context: Context): View {
    val windowManager = getWindowManager(context)
    val controller = this

    return object : OverlayHandlerView(context) {
      private var initialX = 0
      private var initialY = 0
      private var initialTouchX = 0f
      private var initialTouchY = 0f
      private var isDragging = false

      init {
        orientation = VERTICAL
        val cornerRadius = 16.dpToPxF(context)
        val buttonLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        buttonLayoutParams.bottomMargin = 8.dpToPx(context)
        val buttonPadding = 4.dpToPx(context)
        for (button in buttons) {
          addView(
              createButton(
                  context,
                  buttonPadding,
                  buttonLayoutParams,
                  cornerRadius,
                  button.title,
                  button.action,
              )
          )
        }
      }

      override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val layoutParams = controller.currentLayoutParams ?: return false

        when (ev.action) {
          MotionEvent.ACTION_DOWN -> {
            initialX = layoutParams.x
            initialY = layoutParams.y
            initialTouchX = ev.rawX
            initialTouchY = ev.rawY
            isDragging = false
            return false // Don't intercept yet, let children handle it
          }
          MotionEvent.ACTION_MOVE -> {
            val deltaX = ev.rawX - initialTouchX
            val deltaY = ev.rawY - initialTouchY

            // Check if we've moved enough to consider this a drag
            if (!isDragging && (abs(deltaX) > DRAG_TOUCH_SLOP || abs(deltaY) > DRAG_TOUCH_SLOP)) {
              isDragging = true
              // Cancel touch on children
              val cancelEvent = MotionEvent.obtain(ev)
              cancelEvent.action = MotionEvent.ACTION_CANCEL
              super.dispatchTouchEvent(cancelEvent)
              cancelEvent.recycle()
              return true // Start intercepting
            }
            return isDragging
          }
          MotionEvent.ACTION_UP,
          MotionEvent.ACTION_CANCEL -> {
            isDragging = false
            return false
          }
        }
        return false
      }

      override fun onTouchEvent(event: MotionEvent): Boolean {
        val layoutParams = controller.currentLayoutParams ?: return false
        val (screenWidth, screenHeight) = controller.getScreenDimensions(windowManager)

        when (event.action) {
          MotionEvent.ACTION_DOWN -> {
            initialX = layoutParams.x
            initialY = layoutParams.y
            initialTouchX = event.rawX
            initialTouchY = event.rawY
            isDragging = false
            return true
          }
          MotionEvent.ACTION_MOVE -> {
            val deltaX = event.rawX - initialTouchX
            val deltaY = event.rawY - initialTouchY

            // Check if we've moved enough to consider this a drag
            if (!isDragging && (abs(deltaX) > DRAG_TOUCH_SLOP || abs(deltaY) > DRAG_TOUCH_SLOP)) {
              isDragging = true
            }

            if (isDragging) {
              // Calculate new position and clamp to screen bounds
              // Use actual view dimensions (always available during touch events)
              val newX = (initialX + deltaX.toInt()).coerceIn(0, screenWidth - width)
              val newY = (initialY + deltaY.toInt()).coerceIn(0, screenHeight - height)

              layoutParams.x = newX
              layoutParams.y = newY
              windowManager.updateViewLayout(this, layoutParams)
            }
            return true
          }
          MotionEvent.ACTION_UP,
          MotionEvent.ACTION_CANCEL -> {
            isDragging = false
            return true
          }
        }
        return super.onTouchEvent(event)
      }
    }
  }

  private fun createButton(
      context: Context,
      paddingPx: Int,
      layoutParams: ViewGroup.LayoutParams,
      cornerRadius: Float,
      text: String,
      action: View.OnClickListener,
  ): Button =
      Button(context).apply {
        setText(text)
        setTextColor(buttonTextColor)
        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
        setLayoutParams(layoutParams)
        setOnClickListener(action)
        background =
            GradientDrawable().apply {
              setColor(buttonBackgroundColor)
              shape = GradientDrawable.RECTANGLE
              setCornerRadius(cornerRadius)
            }
      }

  fun maybeGetContext(drawable: FrescoDrawableInterface): Context? =
      when (drawable) {
        is Drawable -> {
          when (val callback = drawable.callback) {
            is View -> callback.context
            else -> null
          }
        }
        else -> null
      }

  private fun canShowOverlays(context: Context): Boolean {
    overlayPermissionStatus?.let {
      return it
    }
    // Settings.canDrawOverlays requires API 23; on API 21-22, overlay permission is granted at
    // install time
    val canDraw =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          Settings.canDrawOverlays(context)
        } else {
          true
        }
    if (!canDraw) {
      showOverlayPermissionMessage(context)
      overlayPermissionStatus = false
    } else {
      overlayPermissionStatus = true
    }
    return overlayPermissionStatus == true
  }
}
