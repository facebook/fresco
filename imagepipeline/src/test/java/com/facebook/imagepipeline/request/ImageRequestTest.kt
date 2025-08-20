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
                }
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
                }
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
}
