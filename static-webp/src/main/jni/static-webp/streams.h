/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef _STREAMS_H_
#define _STREAMS_H_

#include <vector>
#include <stdint.h>

#include <jni.h>

namespace facebook {
namespace imagepipeline {

/**
 * Size of buffer used for reading from stream.
 */
static const unsigned int kDefaultBufferSize = 8 * 1024;

/**
 * Limit total number of bytes read from a stream.
 */
static const unsigned int kMaxAllowedImageSize = 8 * 1024 * 1024;

/**
 * Reads all data from this input stream into a vector.
 *
 * @param env
 * @param is
 */
std::vector<uint8_t> readStreamFully(JNIEnv* env, jobject is);

} }

#endif /* _STREAMS_H_ */
