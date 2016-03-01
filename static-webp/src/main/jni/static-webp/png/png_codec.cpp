/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <iterator>

#include <jni.h>
#include <png.h>

#include "exceptions.h"
#include "png_stream_wrappers.h"
#include "decoded_image.h"
#include "png_utils.h"
#include "png_codec.h"

namespace facebook {
namespace imagepipeline {
namespace png {

void encodePngIntoOutputStream(
    JNIEnv* env,
    DecodedImage& decoded_image,
    jobject os) {
  THROW_AND_RETURN_IF(
      decoded_image.getPixelFormat() != PixelFormat::RGBA,
      "png encode function expect ARGB pixel format");

  png_structp png_ptr = png_create_write_struct(
      PNG_LIBPNG_VER_STRING,
      nullptr,
      nullptr,
      nullptr);
  THROW_AND_RETURN_IF(png_ptr == nullptr, "could not create png struct");
  PngStructGuard png_guard{png_ptr};

  png_infop info_ptr = png_create_info_struct(png_ptr);
  THROW_AND_RETURN_IF(info_ptr == nullptr, "could not create png info");
  png_guard.setInfoPtr(info_ptr);

  PngOutputStreamWrapper os_wrapper{env, os};
  RETURN_IF_EXCEPTION_PENDING;

  // Create all png structs that needs releasing before this line.
  if (setjmp(png_jmpbuf(png_ptr))) {
    safeThrowJavaException(env, jRuntimeException_class, "error encoding png");
    return;
  }

  png_set_IHDR(
    png_ptr,
    info_ptr,
    decoded_image.getWidth(),
    decoded_image.getHeight(),
    8,
    PNG_COLOR_TYPE_RGBA,
    PNG_INTERLACE_NONE,
    PNG_COMPRESSION_TYPE_BASE,
    PNG_FILTER_TYPE_BASE
  );
  png_set_write_fn(
      png_ptr,
      &os_wrapper,
      pngWriteToJavaOutputStream,
      pngNoOpFlush);

  // write the image
  const int row_stride = decoded_image.getStride();
  png_bytep row_pointer = decoded_image.getPixelsPtr();
  png_write_info(png_ptr, info_ptr);
  for (unsigned i = 0; i < decoded_image.getHeight(); ++i) {
    png_write_row(png_ptr, row_pointer);
    std::advance(row_pointer, row_stride);
  }
  png_write_end(png_ptr, info_ptr);
}

} } }
