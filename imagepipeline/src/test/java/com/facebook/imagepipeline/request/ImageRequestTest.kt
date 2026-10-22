/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request

import android.net.Uri
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.listener.RequestLoggingListener
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageRequestTest {

  @Test
  fun testCreatingRequestFromExistingRequest() {
    val original =
        ImageRequestBuilder.newBuilderWithSource(Uri.parse("http://frescolib.org/image.jpg"))
            .setCacheChoice(ImageRequest.CacheChoice.SMALL)
            .setImageDecodeOptions(ImageDecodeOptions.newBuilder().build())
            .setLocalThumbnailPreviewsEnabled(true)
            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
            .setPostprocessor(
                object : BasePostprocessor() {
                  override fun getName(): String {
                    return super.getName()
                  }
                },
            )
            .setProgressiveRenderingEnabled(true)
            .setRequestListener(RequestLoggingListener())
            .setResizeOptions(ResizeOptions(20, 20))
            .setRotationOptions(RotationOptions.forceRotation(RotationOptions.ROTATE_90))
            .setRequestPriority(Priority.HIGH)
            .build()

    val copy = ImageRequestBuilder.fromRequest(original).build()

    assertThat(copy).isEqualTo(original)
  }

  @Test
  fun testCreatingRequestWithDynamicCacheChoice_success() {
    val request =
        ImageRequestBuilder.newBuilderWithSource(Uri.parse("http://frescolib.org/image.jpg"))
            .setCacheChoice(ImageRequest.CacheChoice.DYNAMIC)
            .setDiskCacheId("dynamic_cache_id")
            .setImageDecodeOptions(ImageDecodeOptions.newBuilder().build())
            .setLocalThumbnailPreviewsEnabled(true)
            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
            .setPostprocessor(
                object : BasePostprocessor() {
                  override fun getName(): String {
                    return super.getName()
                  }
                },
            )
            .setProgressiveRenderingEnabled(true)
            .setRequestListener(RequestLoggingListener())
            .setResizeOptions(ResizeOptions(20, 20))
            .setRotationOptions(RotationOptions.forceRotation(RotationOptions.ROTATE_90))
            .setRequestPriority(Priority.HIGH)
            .build()

    assertThat(request).isNotNull()
    assertThat(request.cacheChoice).isEqualTo(ImageRequest.CacheChoice.DYNAMIC)
    assertThat(request.diskCacheId).isEqualTo("dynamic_cache_id")
  }

  @Test
  fun testImageRequestForLocalFile_normal() {
    val file = File("/foo/photos/penguin.jpg")
    val imageRequest = ImageRequest.fromFile(file)

    assertThat(imageRequest).isNotNull()
    assertThat(imageRequest?.sourceFile?.absolutePath).isEqualTo(file.absolutePath)
  }

  @Test
  fun testImageRequestForLocalFile_withSpaces() {
    val file = File("/foo/photos folder/penguin crowd.jpg")
    val imageRequest = ImageRequest.fromFile(file)

    assertThat(imageRequest).isNotNull()
    assertThat(imageRequest?.sourceFile?.absolutePath).isEqualTo(file.absolutePath)
  }

  @Test
  fun testImageRequestForLocalFile_withSpecialCharacters() {
    val file = File("/foo/photos#folder/with spaces/penguin?_&*-...jpg")
    val imageRequest = ImageRequest.fromFile(file)

    assertThat(imageRequest).isNotNull()
    assertThat(imageRequest?.sourceFile?.absolutePath).isEqualTo(file.absolutePath)
  }

  @Test
  fun testNewImageRequestFromExistingBuilder() {
    val originalImageRequest =
        ImageRequestBuilder.newBuilderWithSource(Uri.parse("http://frescolib.org/image.jpg"))
            .setCacheChoice(ImageRequest.CacheChoice.DYNAMIC)
            .setDiskCacheId("dynamic_cache_id")
            .setImageDecodeOptions(ImageDecodeOptions.newBuilder().build())
            .setLocalThumbnailPreviewsEnabled(true)
            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
            .setPostprocessor(
                object : BasePostprocessor() {
                  override fun getName(): String {
                    return super.getName()
                  }
                },
            )
            .setProgressiveRenderingEnabled(true)
            .setRequestListener(RequestLoggingListener())
            .setResizeOptions(ResizeOptions(20, 20))
            .setRotationOptions(RotationOptions.forceRotation(RotationOptions.ROTATE_90))
            .setRequestPriority(Priority.HIGH)
            .build()
    val newBuilder = ImageRequestBuilder.fromRequest(originalImageRequest)
    newBuilder.setLocalThumbnailPreviewsEnabled(false)
    assertThat(originalImageRequest.newImageRequestFromImageRequestBuilder(newBuilder))
        .isEqualTo(newBuilder.build())
  }

  @Test
  fun testExtras_areCarriedFromBuilderToRequest() {
    val request = newBuilder().setExtra(SIZE_EXTRA, 3).setExtra(LABEL_EXTRA, "feed").build()

    assertThat(request.getExtra(SIZE_EXTRA)).isEqualTo(3)
    assertThat(request.getExtra(LABEL_EXTRA)).isEqualTo("feed")
    assertThat(request.extras).hasSize(2)
  }

  @Test
  fun testExtras_nullValueRemovesExtra() {
    val request = newBuilder().setExtra(SIZE_EXTRA, 3).setExtra(SIZE_EXTRA, null).build()

    assertThat(request.getExtra(SIZE_EXTRA)).isNull()
    assertThat(request.extras).isNull()
  }

  @Test
  fun testExtras_areCopiedByFromRequest() {
    val original = newBuilder().setExtra(SIZE_EXTRA, 3).setExtra(LABEL_EXTRA, "feed").build()

    val copy = ImageRequestBuilder.fromRequest(original).build()

    assertThat(copy.getExtra(SIZE_EXTRA)).isEqualTo(3)
    assertThat(copy.getExtra(LABEL_EXTRA)).isEqualTo("feed")
    assertThat(copy).isEqualTo(original)
  }

  @Test
  fun testExtras_requestIsNotAffectedByLaterBuilderChanges() {
    val builder = newBuilder().setExtra(SIZE_EXTRA, 3)
    val request = builder.build()

    builder.setExtra(SIZE_EXTRA, 4)

    assertThat(request.getExtra(SIZE_EXTRA)).isEqualTo(3)
  }

  @Test(expected = UnsupportedOperationException::class)
  fun testExtras_requestExtrasAreReadOnly() {
    val request = newBuilder().setExtra(SIZE_EXTRA, 3).build()

    request.extras?.clear()
  }

  @Test
  fun testExtras_equalityExtraDistinguishesRequests() {
    val small = newBuilder().setExtra(SIZE_EXTRA, 1).build()
    val large = newBuilder().setExtra(SIZE_EXTRA, 2).build()
    val none = newBuilder().build()

    assertThat(small).isNotEqualTo(large)
    assertThat(small).isNotEqualTo(none)
    assertThat(none).isNotEqualTo(small)
  }

  @Test
  fun testExtras_nonEqualityExtraKeepsRequestsEqual() {
    val feed = newBuilder().setExtra(LABEL_EXTRA, "feed").build()
    val explore = newBuilder().setExtra(LABEL_EXTRA, "explore").build()
    val none = newBuilder().build()

    assertThat(feed).isEqualTo(explore)
    assertThat(feed).isEqualTo(none)
    assertThat(feed.hashCode()).isEqualTo(explore.hashCode())
    assertThat(feed.hashCode()).isEqualTo(none.hashCode())
  }

  @Test
  fun testExtras_equalRequestsHaveEqualHashCodes() {
    val first = newBuilder().setExtra(SIZE_EXTRA, 3).build()
    val second = newBuilder().setExtra(SIZE_EXTRA, 3).build()

    assertThat(first).isEqualTo(second)
    assertThat(first.hashCode()).isEqualTo(second.hashCode())
  }

  private fun newBuilder(): ImageRequestBuilder =
      ImageRequestBuilder.newBuilderWithSource(Uri.parse("http://frescolib.org/image.jpg"))

  private companion object {
    val SIZE_EXTRA = ImageRequestExtra<Int>("size")
    val LABEL_EXTRA = ImageRequestExtra<String>("label", affectsEquality = false)
  }
}
