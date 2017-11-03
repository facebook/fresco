/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
#include <math.h>
#include <stdlib.h>
#include <string.h>

#include <android/bitmap.h>
#include <jni.h>

typedef struct {
  uint8_t b, g, r, a;
} pixel_t;

#define ARRAY_SIZE(a) (sizeof(a) / sizeof((a)[0]))
#define UNUSED(expr) ((void) (expr));

// These values are chosen arbitrarily but small enough to avoid integer-overflows
#define BITMAP_MAX_DIMENSION 65536

static jclass runtime_exception_class;

static void safe_throw_exception(JNIEnv* env, const char* msg) {
  if (!(*env)->ExceptionCheck(env)) {
    (*env)->ThrowNew(env, runtime_exception_class, msg);
  }
}

/**
 * A native implementation for rounding a given bitmap to a circular shape.
 */
static void RoundingFilter_toCircle(
    JNIEnv* env,
    jclass clazz,
    jobject bitmap) {
  UNUSED(clazz);

  AndroidBitmapInfo bitmapInfo;

  int rc = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    safe_throw_exception(env, "Failed to get Bitmap info");
    return;
  }

  if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
    safe_throw_exception(env, "Unexpected bitmap format");
    return;
  }

  pixel_t* pixelPtr;

  const int w = bitmapInfo.width;
  const int h = bitmapInfo.height;

  if (w > BITMAP_MAX_DIMENSION || h > BITMAP_MAX_DIMENSION) {
    safe_throw_exception(env, "Bitmap dimensions too large");
    return;
  }

  // locking pixels such that they will not get moved around during processing
  rc = AndroidBitmap_lockPixels(env, bitmap, (void*) &pixelPtr);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    safe_throw_exception(env, "Failed to lock Bitmap pixels");
    return;
  }

  // TODO (oprisnik): The actual rounding code will be here in a subsequent diff.

  rc = AndroidBitmap_unlockPixels(env, bitmap);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    safe_throw_exception(env, "Failed to unlock Bitmap pixels");
  }
}

static JNINativeMethod rounding_native_methods[] = {
  { "nativeToCircleFilter",
    "(Landroid/graphics/Bitmap;)V",
    (void*) RoundingFilter_toCircle },
};

jint registerRoundingFilterMethods(JNIEnv* env) {
  jclass runtime_exception = (*env)->FindClass(
      env,
      "java/lang/RuntimeException");
  if (!runtime_exception) {
    return JNI_ERR;
  }
  runtime_exception_class = (*env)->NewGlobalRef(env, runtime_exception);

  jclass rounding_filter_class = (*env)->FindClass(
       env,
      "com/facebook/imagepipeline/nativecode/NativeRoundingFilter");
  if (!rounding_filter_class) {
    return JNI_ERR;
  }

  int rc = (*env)->RegisterNatives(
      env,
      rounding_filter_class,
      rounding_native_methods,
      ARRAY_SIZE(rounding_native_methods));
  if (rc != JNI_OK) {
    return JNI_ERR;
  }

  return JNI_VERSION_1_6;
}
