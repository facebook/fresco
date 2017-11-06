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
#define BITMAP_MAX_DIMENSION 32768
#define BITMAP_MAX_PIXELS (BITMAP_MAX_DIMENSION * BITMAP_MAX_DIMENSION)
// Transparent pixel color
#define TRANSPARENT_PIXEL_COLOR 0x00000000

static jclass runtime_exception_class;
static inline int min(const int a, const int b) { return a < b ? a : b;}

static void safe_throw_exception(JNIEnv* env, const char* msg) {
  if (!(*env)->ExceptionCheck(env)) {
    (*env)->ThrowNew(env, runtime_exception_class, msg);
  }
}

/**
 * Modified midpoint circle algorithm. Clears all pixels outside the circle.
 */
static void toCircle(
    JNIEnv* env,
    pixel_t* pixelPtr,
    const int w,
    const int h,
    const int centerX,
    const int centerY,
    const int radius) {
  if (radius < 1) {
    safe_throw_exception(env, "Circle radius too small!");
    return;
  }
  if (w <= 0 || h <= 0 || w > BITMAP_MAX_DIMENSION || h > BITMAP_MAX_DIMENSION) {
    safe_throw_exception(env, "Invalid bitmap dimensions!");
    return;
  }
  if (centerX < 0 || centerY < 0 || centerX >= w || centerY >= h) {
    safe_throw_exception(env, "Invalid circle center coordinates!");
    return;
  }

  int x = radius - 1;
  int y = 0;

  const int maxX = centerX + x;
  const int maxY = centerY + x;
  const int minX = centerX - x;
  const int minY = centerY - x;

  if (minX < 0 || minY < 0 || maxX >= w || maxY >= h) {
    safe_throw_exception(env, "Circle must be fully visible!");
    return;
  }

  int dx = 1;
  int dy = 1;

  const int rInc = - radius * 2;
  int err = dx + rInc;

  while (x >= y) {

    const int cXpX = centerX + x;
    const int cXmX = centerX - x;
    const int cXpY = centerX + y;
    const int cXmY = centerX - y;

    const int cYpX = centerY + x;
    const int cYmX = centerY - x;
    const int cYpY = centerY + y;
    const int cYmY = centerY - y;

    if (x < 0 || cXpY >= w || cXmY < 0 || cYpY >= h || cYmY < 0) {
      safe_throw_exception(env, "Invalid internal state!");
      return;
    }

    const int offA = w * cYpY;
    const int offB = w * cYmY;
    const int offC = w * cYpX;
    const int offD = w * cYmX;

    const size_t leftBytesX = sizeof(pixel_t) * cXmX;
    const size_t leftBytesY = sizeof(pixel_t) * cXmY;
    const size_t rightBytesX = sizeof(pixel_t) * (w-cXpX);
    const size_t rightBytesY = sizeof(pixel_t) * (w-cXpY);

    // clear left
    memset(pixelPtr + offA, TRANSPARENT_PIXEL_COLOR, leftBytesX);
    memset(pixelPtr + offB, TRANSPARENT_PIXEL_COLOR, leftBytesX);
    memset(pixelPtr + offC, TRANSPARENT_PIXEL_COLOR, leftBytesY);
    memset(pixelPtr + offD, TRANSPARENT_PIXEL_COLOR, leftBytesY);

    // clear right
    memset(pixelPtr + offA + cXpX, TRANSPARENT_PIXEL_COLOR, rightBytesX);
    memset(pixelPtr + offB + cXpX, TRANSPARENT_PIXEL_COLOR, rightBytesX);
    memset(pixelPtr + offC + cXpY, TRANSPARENT_PIXEL_COLOR, rightBytesY);
    memset(pixelPtr + offD + cXpY, TRANSPARENT_PIXEL_COLOR, rightBytesY);

    if (err <= 0) {
      y++;

      dy += 2;
      err += dy;
    }
    if (err > 0) {
      x--;

      dx += 2;
      err += dx + rInc;
    }
  }

  const size_t lineBytes = sizeof(pixel_t) * w;

  // clear top / bottom if height > width
  for (int i = centerY - radius; i >= 0; i--) {
    memset(pixelPtr + i * w, TRANSPARENT_PIXEL_COLOR, lineBytes);
  }

  for (int i = centerY + radius; i < h; i++) {
    memset(pixelPtr + i * w, TRANSPARENT_PIXEL_COLOR, lineBytes);
  }
}

/**
 * A native implementation for rounding a given bitmap to a circular shape.
 * The underlying implementation uses a modified midpoint circle algorithm but instead of
 * drawing a circle, it clears all pixels starting from the circle all the way to the bitmap edges.
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

  const int radius = min(w, h) / 2;
  toCircle(env, pixelPtr, w, h, w / 2, h / 2, radius);

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
