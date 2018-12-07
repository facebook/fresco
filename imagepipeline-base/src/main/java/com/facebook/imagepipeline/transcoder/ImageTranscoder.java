/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.transcoder;

import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nullable;

/** The abstraction for an image transcoder */
public interface ImageTranscoder {

  /**
   * Transcodes an image to match the specified rotation and resize options. The most common-use
   * case is to create a resized version of an input image to make subsequent decodings faster.
   *
   * @param encodedImage The {@link EncodedImage} that will be transcoded.
   * @param outputStream The {@link OutputStream} where the newly created image is written to.
   * @param rotationOptions The {@link RotationOptions} used when transcoding the image.
   * @param resizeOptions The {@link ResizeOptions} used when transcoding the image.
   * @param outputFormat The desired {@link ImageFormat} of the newly created image. If this is null
   *     the same format as the input image will be used.
   * @param quality The desired quality of the newly created image. If this is null, the default
   *     quality of the transcoder will be applied.
   * @return The {@link ImageTranscodeResult} generated when encoding the image.
   * @throws IOException if I/O error happens when reading or writing the images.
   */
  ImageTranscodeResult transcode(
      EncodedImage encodedImage,
      OutputStream outputStream,
      @Nullable RotationOptions rotationOptions,
      @Nullable ResizeOptions resizeOptions,
      @Nullable ImageFormat outputFormat,
      @Nullable Integer quality)
      throws IOException;

  /**
   * Whether the input image is resized to make subsequent decodings faster.
   *
   * @param encodedImage The {@link EncodedImage} that will be transcoded.
   * @param rotationOptions The {@link RotationOptions} used when transcoding the image.
   * @param resizeOptions The {@link ResizeOptions} used when transcoding the image.
   * @return true if the image is resized, else false.
   */
  boolean canResize(
      EncodedImage encodedImage,
      @Nullable RotationOptions rotationOptions,
      @Nullable ResizeOptions resizeOptions);

  /**
   * Whether the input {@link ImageFormat} can be transcoded by the image transcoder.
   *
   * @param imageFormat The {@link ImageFormat} that will be transcoded.
   * @return true if this image format is handled by the image transcoder, else false.
   */
  boolean canTranscode(ImageFormat imageFormat);

  /**
   * Gets the identifier of the image transcoder. This is mostly used for logging purposes.
   *
   * @return the identifier of the image transcoder.
   */
  String getIdentifier();
}
