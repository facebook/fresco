/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.samples.kotlin

import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder

data class ImageHolder(private val view: View,
                       private val resizeOptions: ResizeOptions?) : RecyclerView.ViewHolder(view) {
  companion object {
    private const val DEFAULT_IMAGE_SIZE = 360
  }

  init {
    val width = resizeOptions?.width ?: DEFAULT_IMAGE_SIZE
    val height = resizeOptions?.height ?: DEFAULT_IMAGE_SIZE
    itemView.layoutParams = ViewGroup.LayoutParams(width, height)
  }

  fun bind(uri: Uri) {
    itemView as? SimpleDraweeView ?: return
    itemView.controller = Fresco.newDraweeControllerBuilder()
        .setImageRequest(
            ImageRequestBuilder.newBuilderWithSource(uri)
                .setResizeOptions(resizeOptions)
                .build())
        .setOldController(itemView.controller)
        .setAutoPlayAnimations(true)
        .build()
  }
}

class ImageAdapter(private val placeholderDrawable: Drawable,
                   private val failureDrawable: Drawable,
                   squareDim: Int) : RecyclerView.Adapter<ImageHolder>() {
  private var uris = listOf<Uri>()
  private val imageResizeOptions = ResizeOptions.forSquareSize(squareDim)

  fun setUris(uris: List<Uri>) {
    this.uris = uris
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
    val context = parent.context
    val hierarchy = GenericDraweeHierarchyBuilder(context.resources)
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
