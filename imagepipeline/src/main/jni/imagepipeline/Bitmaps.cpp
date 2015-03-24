/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <algorithm>
#include <cstring>
#include <type_traits>

#include <android/bitmap.h>

#include "bitmap_utils.h"
#include "exceptions.h"
#include "logging.h"
#include "Bitmaps.h"

static void Bitmaps_pinBitmap(
    JNIEnv* env,
    jclass clazz,
    jobject bitmap) {
  uint8_t* pixelBufPtr;
  int result = AndroidBitmap_lockPixels(env, bitmap, (void**) &pixelBufPtr);
  THROW_AND_RETURN_IF(
      result != ANDROID_BITMAP_RESULT_SUCCESS,
      "Failed to pin bitmap");
}

static void Bitmaps_copyBitmap(
    JNIEnv* env,
    jclass clazz,
    jobject dest,
    jint destStride,
    jobject src,
    jint srcStride,
    jint rows) {
  facebook::imagepipeline::BitmapPixelsLock destLock{env, dest};
  uint8_t* destPtr = destLock.getPixelsPtr();
  THROW_AND_RETURN_IF(
      destPtr == nullptr,
      "Could not lock destination's pixels");

  facebook::imagepipeline::BitmapPixelsLock srcLock{env, src};
  uint8_t* srcPtr = srcLock.getPixelsPtr();
  THROW_AND_RETURN_IF(srcPtr == nullptr, "Could not lock source's pixels");

  if (destPtr == srcPtr) {
    return;
  }

  if (destStride == srcStride) {
    memcpy(destPtr, srcPtr, rows * destStride);
  } else {
    for (int row = 0; row < rows; ++row) {
      memcpy(destPtr, srcPtr, std::min(destStride, srcStride));
      destPtr += destStride;
      srcPtr += srcStride;
    }
  }
}



static JNINativeMethod gBitmapsMethods[] = {
  { "nativePinBitmap",
    "(Landroid/graphics/Bitmap;)V",
    (void*) Bitmaps_pinBitmap },
  { "nativeCopyBitmap",
    "(Landroid/graphics/Bitmap;ILandroid/graphics/Bitmap;II)V",
    (void*) Bitmaps_copyBitmap },
};

bool registerBitmapsMethods(JNIEnv* env) {
  auto bitmapsClass = env->FindClass(
      "com/facebook/imagepipeline/nativecode/Bitmaps");
  if (bitmapsClass == nullptr) {
    LOGE("could not find Bitmaps class");
    return false;
  }

  auto result = env->RegisterNatives(
      bitmapsClass,
      gBitmapsMethods,
      std::extent<decltype(gBitmapsMethods)>::value);

  if (result != 0) {
    LOGE("could not register Bitmaps methods");
    return false;
  }

  return true;
}
