/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef _PNG_STREAM_WRAPPERS_H_
#define _PNG_STREAM_WRAPPERS_H_

#include <jni.h>
#include <png.h>

namespace facebook {
namespace imagepipeline {
namespace png {

const int kIOBufferSize = 8 * 1024;

/**
 * Wrapper for java OutputStream that makes it possible to forward output
 * of libpng directly to OutputStream.
 */
class PngOutputStreamWrapper {
 public:
  PngOutputStreamWrapper(
      JNIEnv* env,
      jobject os,
      const int bufferSize = kIOBufferSize);

  void write(
      png_structp png_ptr,
      png_bytep data,
      png_size_t length);

 private:
  JNIEnv* env_;
  jobject os_;
  jbyteArray buffer_;
  const int bufferSize_;
};

/**
 * Wrire function that should
 */
void pngWriteToJavaOutputStream(
    png_structp png_ptr,
    png_bytep data,
    png_size_t length);

/**
 * No op flush function to be used with java output stream wrappers
 */
void pngNoOpFlush(png_structp png_ptr);

} } }

#endif /* _PNG_STREAM_WRAPPERS_H_ */
