/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef _JPEG_ERROR_HANDLER_H_
#define _JPEG_ERROR_HANDLER_H_

#include <type_traits>

#include <stdio.h>
#include <setjmp.h>

#include <jni.h>
#include <jpeglib.h>

namespace facebook {
namespace imagepipeline {
namespace jpeg {

/**
 * Custom error handler for libjpeg-turbo.
 *
 * <p> By default libjpeg handles errors by terminating running process.
 * This one uses setjmp / longjmp instead.
 *
 * <p> In addition to calling longjmp when error occurs, the error handler
 * will throw instance of RuntimeException encapsulating error message
 * passed by libjpeg.
 *
 * <p> This is not a c++ class because the only purpose of the handler is to
 * be used with libjpeg which is a c library.
 */
struct JpegErrorHandler {

  struct jpeg_error_mgr pub;      // default fields defined by libjpeg
  jmp_buf setjmpBuffer;           // return point
  JNIEnv* env;                    // JNI environment

  jpeg_decompress_struct* dinfoPtr;
  jpeg_compress_struct* cinfoPtr;

  /**
   * Constructs JpegErrorHanlder with given jni environment.
   *
   * <p> To use it with given compress struct or decompress strunct call
   * one of setDecompressStruct/setCompressStruct methods.
   */
  JpegErrorHandler(JNIEnv* env);

  /**
   * Sets error handling for jpeg decompressing.
   */
  void setDecompressStruct(jpeg_decompress_struct& dinfo);

  /**
   * Sets error handling for jpeg compressing.
   */
  void setCompressStruct(jpeg_compress_struct& cinfo);
};

/**
 * We cast pointers of type struct jpeg_error_mgr* pointing to pub field
 * to a pointer of type struct fb_jpeg_error_handler* and expect that we
 * obtain a valid pointer to enclosing structure. Assertions below ensure
 * that this assumption is always true.
 */
static_assert(
    std::is_standard_layout<JpegErrorHandler>::value,
    "fb_jpeg_error_handler has to be type of standard layout");
static_assert(
    offsetof(JpegErrorHandler, pub) == 0,
    "offset of fb_jpeg_error_handler.pub should be 0");

/**
 * Throws RuntimeException with message formatted by libjpeg and jumps
 * to the place pointed by setjmp buffer of associated fb_jpeg_error_handler
 */
void jpegThrow(j_common_ptr cinfo);

/**
 * If there is no exception pending, throws RuntimeException initialized
 * with passed message. In any case, returns control to the place poitned
 * by setjmp buffer of associated JpegErrorHandler structure.
 */
void jpegSafeThrow(
    j_common_ptr cinfo,
    const char* msg);

/**
 * Checks for pending java exception and if one occured
 * frees jpeg-turbo resources and jumps to the place pointed by
 * setjmp buffer of associated JpegErrorHandler structure
 */
void jpegJumpOnException(j_common_ptr cinfo);

} } }

#endif /* _JPEG_ERROR_HANDLER_H_ */
