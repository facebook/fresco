/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <type_traits>

#include <stdint.h>

#include <jni.h>

#include "exceptions_handler.h"
#include "jpeg/jpeg_codec.h"
#include "logging.h"
#include "transformations.h"

using facebook::imagepipeline::getRotationTypeFromDegrees;
using facebook::imagepipeline::getRotationTypeFromRawExifOrientation;
using facebook::imagepipeline::RotationType;
using facebook::imagepipeline::ScaleFactor;
using facebook::imagepipeline::jpeg::transformJpeg;

static void JpegTranscoder_transcodeJpeg(
    JNIEnv* env,
    jclass /* clzz */,
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

static void JpegTranscoder_transcodeJpegWithExifOrientation(
    JNIEnv* env,
    jclass /* clzz */,
    jobject is,
    jobject os,
    jint exif_orientation,
    jint downscale_numerator,
    jint quality) {
  ScaleFactor scale_factor{(uint8_t) downscale_numerator, 8};
  RotationType rotation_type = getRotationTypeFromRawExifOrientation(
      env,
      exif_orientation);
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
  { "nativeTranscodeJpegWithExifOrientation",
    "(Ljava/io/InputStream;Ljava/io/OutputStream;III)V",
    (void*) JpegTranscoder_transcodeJpegWithExifOrientation },
};

bool registerJpegTranscoderMethods(JNIEnv* env) {
  auto nativeJpegTranscoderClass = env->FindClass(
      "com/facebook/imagepipeline/nativecode/NativeJpegTranscoder");
  if (nativeJpegTranscoderClass == nullptr) {
    LOGE("could not find NativeJpegTranscoder class");
    return false;
  }

  auto result = env->RegisterNatives(
      nativeJpegTranscoderClass,
      gJpegTranscoderMethods,
      std::extent<decltype(gJpegTranscoderMethods)>::value);

  if (result != 0) {
    LOGE("could not register JpegTranscoder methods");
    return false;
  }

  return true;
}
