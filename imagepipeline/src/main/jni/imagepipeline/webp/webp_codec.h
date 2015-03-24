/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef _WEBP_CODEC_H_
#define _WEBP_CODEC_H_

#include <memory>

#include <jni.h>

#include "decoded_image.h"

namespace facebook {
namespace imagepipeline {
namespace webp {

/**
 * Decodes webp image from given input stream.
 *
 * @param env
 * @param is
 * @param pixel_format
 */
std::unique_ptr<DecodedImage> decodeWebpFromInputStream(
    JNIEnv* env,
    jobject is,
    PixelFormat pixel_format);

} } }

#endif /* _WEBP_CODEC_H_ */
