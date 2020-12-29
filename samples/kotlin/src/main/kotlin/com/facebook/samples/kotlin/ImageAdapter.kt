/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.kotlin

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.backends.pipeline.info.ImagePerfData
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener
import com.facebook.drawee.backends.pipeline.info.ImagePerfUtils
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder

data class ImageHolder(private val view: View, private val resizeOptions: ResizeOptions?) :
    RecyclerView.ViewHolder(view) {
  companion object {
    private const val DEFAULT_IMAGE_SIZE = 360
  }

  init {
    val width = resizeOptions?.width ?: DEFAULT_IMAGE_SIZE
    val height = resizeOptions?.height ?: DEFAULT_IMAGE_SIZE
    itemView.layoutParams = ViewGroup.LayoutParams(width, height)
  }

  val logImagePerf =
      object : ImagePerfDataListener {
        override fun onImageLoadStatusUpdated(imagePerfData: ImagePerfData?, imageLoadStatus: Int) {
          Log.d(
              "ImagePerf",
              "state=${ImagePerfUtils.toString(imageLoadStatus)}, data=${imagePerfData?.createDebugString()}")
        }

        override fun onImageVisibilityUpdated(imagePerfData: ImagePerfData?, visibilityState: Int) {
          // ignore
        }
      }

  fun bind(uri: Uri) {
    itemView as? SimpleDraweeView ?: return
    itemView.controller =
        Fresco.newDraweeControllerBuilder()
            .setImageRequest(
                ImageRequestBuilder.newBuilderWithSource(uri)
                    .setResizeOptions(resizeOptions)
                    .build())
            .setOldController(itemView.controller)
            .setAutoPlayAnimations(true)
            .setPerfDataListener(logImagePerf)
            .build()
  }
}

class ImageAdapter(
    private val placeholderDrawable: Drawable,
    private val failureDrawable: Drawable,
    squareDim: Int
) : RecyclerView.Adapter<ImageHolder>() {
  private var uris = listOf<Uri>()
  private val imageResizeOptions = ResizeOptions.forSquareSize(squareDim)

  fun setUris(uris: List<Uri>) {
    this.uris = uris
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
    val context = parent.context
    val hierarchy =
        GenericDraweeHierarchyBuilder(context.resources)
            .setPlaceholderImage(placeholderDrawable)
            .setFailureImage(failureDrawable)
            .setProgressBarImage(ProgressBarDrawable())
            .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP)
            .build()
    return ImageHolder(SimpleDraweeView(context, hierarchy), imageResizeOptions)
  }

  override fun onBindViewHolder(holder: ImageHolder, position: Int) {
    holder.bind(uris[position])
  }

  override fun getItemCount() = uris.size
}
