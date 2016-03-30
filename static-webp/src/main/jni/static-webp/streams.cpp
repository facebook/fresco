/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <vector>
#include <stdint.h>

#include <jni.h>

#include "exceptions.h"
#include "java_globals.h"
#include "streams.h"

namespace facebook {
namespace imagepipeline {

std::vector<uint8_t> readStreamFully(JNIEnv* env, jobject is) {
  std::vector<uint8_t> read_buffer;
  jbyteArray java_buffer = env->NewByteArray(kDefaultBufferSize);
  RETURNVAL_IF_EXCEPTION_PENDING({});

  while (true) {
    const int chunk_size =
      env->CallIntMethod(is, midInputStreamRead, java_buffer);
    RETURNVAL_IF_EXCEPTION_PENDING({});

    if (chunk_size < 0) {
      return read_buffer;
    }

    if (chunk_size > 0) {
      jbyte* data = env->GetByteArrayElements(java_buffer, NULL);
      THROW_AND_RETURNVAL_IF(
          data == nullptr,
          "Could not get byte array region",
            {});
      read_buffer.insert(read_buffer.end(), data, data + chunk_size);
      env->ReleaseByteArrayElements(java_buffer, data, JNI_ABORT);
      RETURNVAL_IF_EXCEPTION_PENDING({});
      THROW_AND_RETURNVAL_IF(
          read_buffer.size() > kMaxAllowedImageSize,
          "content of input stream is too large",
          {});
    }
  }
}

} }
