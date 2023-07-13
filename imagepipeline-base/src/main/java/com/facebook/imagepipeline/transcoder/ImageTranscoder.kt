/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder

import android.graphics.ColorSpace
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.EncodedImage
import java.io.IOException
import java.io.OutputStream

/** The abstraction for an image transcoder */
interface ImageTranscoder {

  /**
   * Transcodes an image to match the specified rotation and resize options. The most common-use
   * case is to create a resized version of an input image to make subsequent decodings faster.
   *
   * @param encodedImage The [EncodedImage] that will be transcoded.
   * @param outputStream The [OutputStream] where the newly created image is written to.
   * @param rotationOptions The [RotationOptions] used when transcoding the image.
   * @param resizeOptions The [ResizeOptions] used when transcoding the image.
   * @param outputFormat The desired [ImageFormat] of the newly created image. If this is null the
   *   same format as the input image will be used.
   * @param quality The desired quality of the newly created image. If this is null, the default
   *   quality of the transcoder will be applied.
   * @return The [ImageTranscodeResult] generated when encoding the image.
   * @throws IOException if I/O error happens when reading or writing the images.
   */
  @Throws(IOException::class)
  fun transcode(
      encodedImage: EncodedImage,
      outputStream: OutputStream,
      rotationOptions: RotationOptions?,
      resizeOptions: ResizeOptions?,
      outputFormat: ImageFormat?,
      quality: Int?,
      colorSpace: ColorSpace?
  ): ImageTranscodeResult

  /**
   * Whether the input image is resized to make subsequent decodings faster.
   *
   * @param encodedImage The [EncodedImage] that will be transcoded.
   * @param rotationOptions The [RotationOptions] used when transcoding the image.
   * @param resizeOptions The [ResizeOptions] used when transcoding the image.
   * @return true if the image is resized, else false.
   */
  fun canResize(
      encodedImage: EncodedImage,
      rotationOptions: RotationOptions?,
      resizeOptions: ResizeOptions?
  ): Boolean

  /**
   * Whether the input [ImageFormat] can be transcoded by the image transcoder.
   *
   * @param imageFormat The [ImageFormat] that will be transcoded.
   * @return true if this image format is handled by the image transcoder, else false.
   */
  fun canTranscode(imageFormat: ImageFormat): Boolean

  /**
   * Gets the identifier of the image transcoder. This is mostly used for logging purposes.
   *
   * @return the identifier of the image transcoder.
   */
  val identifier: String
}
