/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <type_traits>

#include <stdint.h>

#include <jni.h>

#include "exceptions.h"
#include "jpeg/jpeg_codec.h"
#include "logging.h"
#include "transformations.h"

using facebook::imagepipeline::getRotationTypeFromDegrees;
using facebook::imagepipeline::RotationType;
using facebook::imagepipeline::ScaleFactor;
using facebook::imagepipeline::jpeg::transformJpeg;

static void JpegTranscoder_transcodeJpeg(
    JNIEnv* env,
    jclass clzz,
    jobject is,
    jobject os,
    jint rotation_degrees,
    jint downscale_numerator,
    jint quality) {
  ScaleFactor scale_factor{(uint8_t) downscale_numerator, 8};
  RotationType rotation_type = getRotationTypeFromDegrees(
      env,
      rotation_degrees);
  RETURN_IF_EXCEPTION_PENDING;
  transformJpeg(
      env,
      is,
      os,
      rotation_type,
      scale_factor,
      quality);
}

static JNINativeMethod gJpegTranscoderMethods[] = {
  { "nativeTranscodeJpeg",
    "(Ljava/io/InputStream;Ljava/io/OutputStream;III)V",
    (void*) JpegTranscoder_transcodeJpeg },
};

bool registerJpegTranscoderMethods(JNIEnv* env) {
  auto jpegTranscoderClass = env->FindClass(
      "com/facebook/imagepipeline/nativecode/JpegTranscoder");
  if (jpegTranscoderClass == nullptr) {
    LOGE("could not find JpegTranscoder class");
    return false;
  }

  auto result = env->RegisterNatives(
      jpegTranscoderClass,
      gJpegTranscoderMethods,
      std::extent<decltype(gJpegTranscoderMethods)>::value);

  if (result != 0) {
    LOGE("could not register JpegTranscoder methods");
    return false;
  }

  return true;
}
