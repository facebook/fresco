/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
#include <math.h>
#include <string.h>

#include <android/bitmap.h>
#include <jni.h>

#define ARGB_PIXEL uint32_t

#define ARRAY_SIZE(a) (sizeof(a) / sizeof((a)[0]))
#define UNUSED(expr) ((void) (expr));

static jclass runtime_exception_class;

static void safe_throw_exception(JNIEnv* env, const char* msg) {
  if (!(*env)->ExceptionCheck(env)) {
    (*env)->ThrowNew(env, runtime_exception_class, msg);
  }
}

static void BlurFilter_iterativeBoxBlur(
    JNIEnv* env,
    jclass clazz,
    jobject bitmap,
    jint iterations,
    jint radius) {
  UNUSED(clazz);
  UNUSED(iterations);
  UNUSED(radius);

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

  ARGB_PIXEL* pixelPtr;

  // locking pixels such that they will not get moved around during processing
  rc = AndroidBitmap_lockPixels(env, bitmap, (void*) &pixelPtr);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    safe_throw_exception(env, "Failed to lock Bitmap pixels");
    return;
  }

  //
  // The algorithm will go here in the next change
  //

  rc = AndroidBitmap_unlockPixels(env, bitmap);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    safe_throw_exception(env, "Failed to unlock Bitmap pixels");
  }
}

static JNINativeMethod blur_filter_native_methods[] = {
  { "nativeIterativeBoxBlur",
    "(Landroid/graphics/Bitmap;II)V",
    (void*) BlurFilter_iterativeBoxBlur },
};

jint registerBlurFilterMethods(JNIEnv* env) {
  jclass runtime_exception = (*env)->FindClass(
      env,
      "java/lang/RuntimeException");
  if (!runtime_exception) {
    return JNI_ERR;
  }
  runtime_exception_class = (*env)->NewGlobalRef(env, runtime_exception);

  jclass blur_filter_class = (*env)->FindClass(
       env,
      "com/facebook/imagepipeline/nativecode/NativeBlurFilter");
  if (!blur_filter_class) {
    return JNI_ERR;
  }

  int rc = (*env)->RegisterNatives(
      env,
      blur_filter_class,
      blur_filter_native_methods,
      ARRAY_SIZE(blur_filter_native_methods));
  if (rc != JNI_OK) {
    return JNI_ERR;
  }

  return JNI_VERSION_1_6;
}
