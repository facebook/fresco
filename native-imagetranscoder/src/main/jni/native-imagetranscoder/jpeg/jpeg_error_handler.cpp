/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <stdio.h>
#include <setjmp.h>

#include <jni.h>
#include <jpeglib.h>

#include "java_globals.h"
#include "jpeg_error_handler.h"

namespace facebook {
namespace imagepipeline {
namespace jpeg {

JpegErrorHandler::JpegErrorHandler(JNIEnv* env)
    : env(env), dinfoPtr(nullptr), cinfoPtr(nullptr) {
  jpeg_std_error(&pub);
  pub.error_exit = jpegThrow;
}

void JpegErrorHandler::setDecompressStruct(jpeg_decompress_struct& dinfo) {
  dinfo.err = &pub;
  dinfoPtr = &dinfo;
}

void JpegErrorHandler::setCompressStruct(jpeg_compress_struct& cinfo) {
  cinfo.err = &pub;
  cinfoPtr = &cinfo;
}

/**
 * private method doing the actual cleanup:
 *  - free jpeg-turbo data structures
 *  - jump back to place determined by setjmpBuffer
 */
static void jpegCleanup(JpegErrorHandler* error_handler) {
  if (error_handler->dinfoPtr) {
    jpeg_destroy_decompress(error_handler->dinfoPtr);
    error_handler->dinfoPtr = nullptr;
  }
  if (error_handler->cinfoPtr) {
    jpeg_destroy_compress(error_handler->cinfoPtr);
    error_handler->cinfoPtr = nullptr;
  }

  longjmp(error_handler->setjmpBuffer, 1);
}

void jpegThrow(j_common_ptr cinfo) {
  // create and throw the jpeg-turbo error message
  char buffer[JMSG_LENGTH_MAX];
  (*cinfo->err->format_message) (cinfo, buffer);
  jpegSafeThrow(cinfo, buffer);
}

void jpegSafeThrow(
    j_common_ptr cinfo,
    const char* msg) {
  JpegErrorHandler* error_handler = (JpegErrorHandler*) cinfo->err;

  if (!error_handler->env->ExceptionCheck()) {
    error_handler->env->ThrowNew(jRuntimeExceptionclass, msg);
  }
  jpegCleanup(error_handler);
}

void jpegJumpOnException(j_common_ptr cinfo) {
  JpegErrorHandler* error_handler = (JpegErrorHandler*) cinfo->err;
  if (error_handler->env->ExceptionCheck()) {
    jpegCleanup(error_handler);
  }
}

} } }
