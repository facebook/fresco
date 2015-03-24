/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef _JPEG_CODEC_H_
#define _JPEG_CODEC_H_

#include <jni.h>

#include "decoded_image.h"
#include "transformations.h"

namespace facebook {
namespace imagepipeline {
namespace jpeg {

/**
 * Encodes given image using libjpeg and writtes encoded bytes
 * into provided output stream.
 *
 * @param env
 * @param decoded_image
 * @param os output stream to write data to
 * @param quality value passed to jpeg encoder
 */
void encodeJpegIntoOutputStream(
    JNIEnv* env,
    DecodedImage& decoded_image,
    jobject os,
    int quality);


/**
 * Downscales and rotates jpeg image
 *
 * @param env
 * @param is InputStream
 * @param os OutputStream
 * @param rotation_type
 * @param scale_factor
 * @param crop_info
 * @param quality
 */
void transformJpeg(
    JNIEnv* env,
    jobject is,
    jobject os,
    RotationType rotation_type,
    const ScaleFactor& scale_factor,
    int quality);

} } }

#endif /* _JPEG_CODEC_H_ */
