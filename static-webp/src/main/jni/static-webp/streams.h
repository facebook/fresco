/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#ifndef _STREAMS_H_
#define _STREAMS_H_

#include <stdint.h>
#include <vector>

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

} // namespace imagepipeline
} // namespace facebook

#endif /* _STREAMS_H_ */
