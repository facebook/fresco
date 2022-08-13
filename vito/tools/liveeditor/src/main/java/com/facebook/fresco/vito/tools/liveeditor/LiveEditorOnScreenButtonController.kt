/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import com.facebook.common.internal.Supplier
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.impl.StringDebugDataProvider
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.tools.liveeditor.ImageOptionsSampleValues.Entry
import com.facebook.fresco.vito.tools.liveeditor.LiveEditorUiUtils.Companion.dpToPx
import com.facebook.fresco.vito.tools.liveeditor.LiveEditorUiUtils.Companion.dpToPxF

class LiveEditorOnScreenButtonController(
    private val isEnabled: Supplier<Boolean>,
    @ColorInt private val buttonTextColor: Int = Color.WHITE,
    @ColorInt private val buttonBackgroundColor: Int = Color.BLUE,
    @ColorInt private val editorBackgroundColor: Int = Color.WHITE,
    additionalButtonConfig: ButtonConfig? = null,
    private val customOptions: CustomOptions,
    private val debugDataProviders: List<StringDebugDataProvider> = emptyList()
) {

  /** Called from fblite java code */
  constructor(
      isEnabled: Supplier<Boolean>,
      @ColorInt buttonTextColor: Int = Color.WHITE,
      @ColorInt buttonBackgroundColor: Int = Color.BLUE,
      @ColorInt editorBackgroundColor: Int = Color.WHITE,
      additionalButtonConfig: ButtonConfig? = null
  ) : this(
      isEnabled,
      buttonTextColor,
      buttonBackgroundColor,
      editorBackgroundColor,
      additionalButtonConfig,
      CustomOptions(emptyList()))

  class ButtonConfig(val title: String, val action: View.OnClickListener)

  class CustomOptions(val entries: List<Entry<out Any>>)

  private val buttons =
      listOfNotNull(
          ButtonConfig("Prev img") { imageSelector?.selectPrevious(it.context) },
          ButtonConfig("Next img") { imageSelector?.selectNext(it.context) },
          ButtonConfig("Edit img") { showLiveEditor(it.context) },
          additionalButtonConfig)

  val imageTrackerListener =
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

        private inline fun ifEnabled(body: () -> Unit) {
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

  fun attachImageSelector(context: Context?) {
    if (isAttached || context == null) {
      // already attached or no context available to get the WindowManager from
      return
    }
    isAttached = true
    imageSelector =
        ImageSelector(
            imageTrackerListener,
            FrescoVitoProvider.getImagePipeline(),
            FrescoVitoProvider.getController())
    showImageToggleButtons(context)
  }

  fun removeImageSelector() {
    if (!isAttached) {
      return
    }
    val current = currentView ?: return

    val windowManager = current.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.removeView(current)
    currentView = null
    isAttached = false
  }

  private fun showImageToggleButtons(
      context: Context,
  ) {
    addWindow(context, createOnScreenButtons(context))
  }

  private fun showLiveEditor(context: Context) {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    addWindow(
        context,
        LiveEditorUiUtils(imageSelector?.currentEditor, debugDataProviders)
            .createView(context, customOptions.entries) { showImageToggleButtons(context) }
            .apply { background = ColorDrawable(editorBackgroundColor) },
        DisplayMetrics().apply { windowManager.defaultDisplay.getMetrics(this) }.heightPixels / 2)
  }

  private fun addWindow(
      context: Context,
      view: View,
      height: Int = WindowManager.LayoutParams.WRAP_CONTENT
  ) {
    if (!isEnabled.get()) {
      return
    }
    val padding = 16.dpToPx(context)
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    currentView?.apply { windowManager.removeView(this) }
    windowManager.addView(
        view,
        WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                height,
                WindowManager.LayoutParams.LAST_APPLICATION_WINDOW,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)
            .apply {
              gravity = Gravity.BOTTOM or Gravity.RIGHT
              x = padding
              y = view.height
            })
    currentView = view
  }

  private fun createOnScreenButtons(context: Context): View {
    return LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      val cornerRadius = 16.dpToPxF(context)
      val buttonLayoutParams: LinearLayout.LayoutParams =
          LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
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
                button.action))
      }
    }
  }

  private fun createButton(
      context: Context,
      paddingPx: Int,
      layoutParams: ViewGroup.LayoutParams,
      cornerRadius: Float,
      text: String,
      action: View.OnClickListener
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
}
