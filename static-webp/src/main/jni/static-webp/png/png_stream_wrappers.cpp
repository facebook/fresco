/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <algorithm>
#include <iterator>

#include <jni.h>
#include <png.h>

#include "exceptions.h"
#include "java_globals.h"
#include "png_stream_wrappers.h"

namespace facebook {
namespace imagepipeline {
namespace png {

void pngNoOpFlush(png_structp png_ptr) {
}

void pngWriteToJavaOutputStream(
    png_structp png_ptr,
    png_bytep data,
    png_size_t length) {
  PngOutputStreamWrapper* os_wrapper =
    reinterpret_cast<PngOutputStreamWrapper*>(png_get_io_ptr(png_ptr));
  os_wrapper->write(png_ptr, data, length);
}

PngOutputStreamWrapper::PngOutputStreamWrapper(
    JNIEnv* env,
    jobject os,
    const int bufferSize)
    : env_(env), os_(os), bufferSize_(bufferSize) {
  buffer_ = env_->NewByteArray(bufferSize_);
  RETURN_IF_EXCEPTION_PENDING;
}

void PngOutputStreamWrapper::write(
    png_structp png_ptr,
    png_bytep data,
    png_size_t length) {
  while (length > 0) {
    const int portion_length = std::min<int>(bufferSize_, length);
    env_->SetByteArrayRegion(
        buffer_,
        0,
        reinterpret_cast<jsize>(portion_length),
        reinterpret_cast<jbyte*>(data));
    if (env_->ExceptionCheck()) {
      png_error(png_ptr, "Error when copying data to java array.");
    }
    env_->CallVoidMethod(
        os_,
        midOutputStreamWriteWithBounds,
        buffer_,
        0,
        reinterpret_cast<jint>(portion_length));
    if (env_->ExceptionCheck()) {
      png_error(png_ptr, "Error when writing data to OutputStream.");
    }
    std::advance(data, portion_length);
    length -= portion_length;
  }
}

} } }
