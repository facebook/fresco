/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <type_traits>

#include <jni.h>

#include "exceptions.h"
#include "jpeg/jpeg_codec.h"
#include "logging.h"
#include "decoded_image.h"
#include "png/png_codec.h"
#include "webp/webp_codec.h"
#include "WebpTranscoder.h"

using facebook::imagepipeline::PixelFormat;
using facebook::imagepipeline::jpeg::encodeJpegIntoOutputStream;
using facebook::imagepipeline::png::encodePngIntoOutputStream;
using facebook::imagepipeline::webp::decodeWebpFromInputStream;

static void WebpTranscoder_transcodeToJpeg(
    JNIEnv* env,
    jclass clzz,
    jobject is,
    jobject os,
    jint quality) {
  auto decodedImagePtr = decodeWebpFromInputStream(env, is, PixelFormat::RGB);
  RETURN_IF_EXCEPTION_PENDING;
  encodeJpegIntoOutputStream(env, *decodedImagePtr, os, quality);
}

static void WebpTranscoder_transcodeToPng(
    JNIEnv* env,
    jclass clzz,
    jobject is,
    jobject os) {
  using namespace facebook::imagepipeline;
  auto decodedImagePtr = decodeWebpFromInputStream(env, is, PixelFormat::RGBA);
  RETURN_IF_EXCEPTION_PENDING;
  encodePngIntoOutputStream(env, *decodedImagePtr, os);
}

static JNINativeMethod gWebpTranscoderMethods[] = {
  { "nativeTranscodeWebpToJpeg",
    "(Ljava/io/InputStream;Ljava/io/OutputStream;I)V",
    (void*) WebpTranscoder_transcodeToJpeg },
  { "nativeTranscodeWebpToPng",
    "(Ljava/io/InputStream;Ljava/io/OutputStream;)V",
    (void*) WebpTranscoder_transcodeToPng },
};

bool registerWebpTranscoderMethods(JNIEnv* env){
  auto webPTranscoderClass = env->FindClass(
      "com/facebook/imagepipeline/nativecode/WebpTranscoder");
  if (webPTranscoderClass == nullptr) {
    LOGE("could not find WebpTranscoder class");
    return false;
  }

  auto result = env->RegisterNatives(
      webPTranscoderClass,
      gWebpTranscoderMethods,
      std::extent<decltype(gWebpTranscoderMethods)>::value);

  if (result != 0) {
    LOGE("could not register WebpTranscoder methods");
    return false;
  }

  return true;
}
