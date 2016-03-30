/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef _FB_JPEG_STREAM_WRAPPERS_H_
#define _FB_JPEG_STREAM_WRAPPERS_H_

#include <type_traits>

#include <stdio.h>

#include <jni.h>
#include <jpeglib.h>

namespace facebook {
namespace imagepipeline {
namespace jpeg {

/**
 * Java InputStream wrapper for libjpeg.
 *
 * <p> This is not a c++ class because the only purpose of the handler is to
 * be used with libjpeg which is a c library.
 */
struct JpegInputStreamWrapper {
  struct jpeg_source_mgr public_fields;
  jobject inputStream;
  jbyteArray javaBuffer;
  JOCTET* buffer;
  JNIEnv* env;
  boolean start;

  /**
   * Wraps given input stream.
   */
  JpegInputStreamWrapper(JNIEnv *env, jobject is);
};

/**
 * We cast pointers of type struct jpeg_source_mgr* pointing to public_fields
 * to a pointer of type struct JpegInputStreamWrapper* and expect that we
 * obtain a valid pointer to enclosing structure. Assertions below ensure
 * that this assumption is always true.
 */
static_assert(
    std::is_standard_layout<JpegInputStreamWrapper>::value,
    "JpegInputStreamWrapper has to be type of standard layout");
static_assert(
    offsetof(JpegInputStreamWrapper, public_fields) == 0,
    "offset of JpegInputStreamWrapper.public_fields should be 0");


/**
 * Java OutputStream wrapper for libjpeg.
 *
 * <p> This is not a c++ class because the only purpose of the handler is to
 * be used with libjpeg which is a c library.
 */
struct JpegOutputStreamWrapper {
  struct jpeg_destination_mgr public_fields;
  jobject outputStream;
  jbyteArray javaBuffer;
  JOCTET* buffer;
  JNIEnv * env;

  /**
   * Wraps given output stream.
   */
  JpegOutputStreamWrapper(JNIEnv *env, jobject os);
};

/**
 * We cast pointers of type struct jpeg_destination_mgr* pointing to public_fields
 * to a pointer of type struct JpegOutputStreamWrapper* and expect that we
 * obtain a valid pointer to enclosing structure. Assertions below ensure
 * that this assumption is always true.
 */
static_assert(
    std::is_standard_layout<JpegOutputStreamWrapper>::value,
    "fb_jpeg_error_handler has to be type of standard layout");
static_assert(
    offsetof(JpegOutputStreamWrapper, public_fields) == 0,
    "offset of JpegOutputStreamWrapper.public_fields should be 0");


} } }

#endif /* _FB_JPEG_STREAM_WRAPPERS_H_ */
