/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <string.h>

#include <android/bitmap.h>
#include <jni.h>

#define ARRAY_SIZE(a) (sizeof(a) / sizeof((a)[0]))

/**
 * Explicitly marks result of an expression as unused.
 *
 * We instruct the compiler to be very strict when it comes to code
 * like unused arguments that could indicate a bug. Because of that, handling
 * intentionally unused arguments requires tricks like casting to void. This
 * macro provides a readable name for this operation.
 */
#define UNUSED(expr) ((void) (expr));

static jclass runtime_exception_class;

static void safe_throw_exception(JNIEnv* env, const char* msg) {
  if (!(*env)->ExceptionCheck(env)) {
    (*env)->ThrowNew(env, runtime_exception_class, msg);
  }
}

/**
 * Safely unlocks bitmap's pixels even if java exception is pending
 */
static void unlock_pixels_safe(JNIEnv* env, jobject bitmap) {
  // "catch" pending exception, if any
  jthrowable stashed_exception = (*env)->ExceptionOccurred(env);
  if (stashed_exception) {
    (*env)->ExceptionClear(env);
  }

  // no pending exceptions, it is safe now to call unlockPixels
  AndroidBitmap_unlockPixels(env, bitmap);

  // rethrow exception
  if (stashed_exception) {
    // we don't expect unlockPixels to throw java exceptions, but since it takes
    // JNIEnv as a parameter we must be prepared to handle such scenario.
    // There is no way of chaining exceptions in java, so lets just print
    // unexpected exception, swallow it and rethrow the stashed one.
    if ((*env)->ExceptionCheck(env)) {
      (*env)->ExceptionDescribe(env);
      (*env)->ExceptionClear(env);
    }
    (*env)->Throw(env, stashed_exception);
  }
}

/**
 * Pins bitmap's pixels.
 *
 * <p> Throws RuntimeException if unable to pin.
 */
static void Bitmaps_pinBitmap(
    JNIEnv* env,
    jclass clazz,
    jobject bitmap) {
  UNUSED(clazz);
  int rc = AndroidBitmap_lockPixels(env, bitmap, 0);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    safe_throw_exception(env, "Failed to pin Bitmap");
  }
}

/**
 * Efficiently copies pixels between bitmaps.
 */
static void Bitmaps_copyBitmap(
    JNIEnv* env,
    jclass clazz,
    jobject dest,
    jint dest_stride,
    jobject src,
    jint src_stride,
    jint rows) {
  UNUSED(clazz);
  void* dest_ptr = 0;
  void* src_ptr = 0;

  int rc = AndroidBitmap_lockPixels(env, dest, &dest_ptr);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS || !dest_ptr) {
    safe_throw_exception(env, "Could not lock desination's pixels");
    return;
  }

  rc = AndroidBitmap_lockPixels(env, src, &src_ptr);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS || !src_ptr) {
    safe_throw_exception(env, "Could not lock source's pixels");
    goto unlock_destination;
  }

  if (dest_ptr == src_ptr) {
    goto unlock_source;
  }

  if (dest_stride == src_stride) {
    memcpy(dest_ptr, src_ptr, rows * dest_stride);
  } else {
    int min_stride = dest_stride < src_stride ? dest_stride : src_stride;
    for (int row = 0; row < rows; ++row) {
      memcpy(dest_ptr, src_ptr, min_stride);
      dest_ptr += dest_stride;
      src_ptr += src_stride;
    }
  }

 unlock_source:
  unlock_pixels_safe(env, src);
 unlock_destination:
  unlock_pixels_safe(env, dest);
}

static JNINativeMethod bitmaps_native_methods[] = {
  { "nativePinBitmap",
    "(Landroid/graphics/Bitmap;)V",
    (void*) Bitmaps_pinBitmap },
  { "nativeCopyBitmap",
    "(Landroid/graphics/Bitmap;ILandroid/graphics/Bitmap;II)V",
    (void*) Bitmaps_copyBitmap },
};

__attribute__((visibility("default")))
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
  UNUSED(reserved);
  JNIEnv* env;

  if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }

  jclass runtime_exception = (*env)->FindClass(
      env,
      "java/lang/RuntimeException");
  if (!runtime_exception) {
    return JNI_ERR;
  }
  runtime_exception_class = (*env)->NewGlobalRef(env, runtime_exception);

  jclass bitmaps_class = (*env)->FindClass(
       env,
      "com/facebook/imagepipeline/nativecode/Bitmaps");
  if (!bitmaps_class) {
    return JNI_ERR;
  }

  int rc = (*env)->RegisterNatives(
      env,
      bitmaps_class,
      bitmaps_native_methods,
      ARRAY_SIZE(bitmaps_native_methods));
  if (rc != JNI_OK) {
    return JNI_ERR;
  }

  return JNI_VERSION_1_6;
}
