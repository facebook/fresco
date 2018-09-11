/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
#define BLUR_MAX_ITERATIONS 65536
#define BLUR_MAX_RADIUS 65536

static jclass runtime_exception_class;

static inline int max(int a, int b) { return a > b ? a : b;}
static inline int bound(int x, int l, int h) {return x < l ? l : (x > h ? h : x);}

static void safe_throw_exception(JNIEnv* env, const char* msg) {
  if (!(*env)->ExceptionCheck(env)) {
    (*env)->ThrowNew(env, runtime_exception_class, msg);
  }
}

/**
 * Creates a blurred version of the given `row` of `pixelsIn`. It uses a moving average algorithm
 * such that it reads every pixel of the row just once. The edge pixels are repeated to avoid
 * artifacts.
 *
 * Requires a pre-computed `div` table of size `255 * diameter` that maps `x -> x / diameter` (can
 * be rounded)
 */
static inline void internalHorizontalBlur(
    pixel_t* pixelsIn,
    pixel_t* outRow,
    int w,
    int row,
    int diameter,
    uint8_t* div) {
  const int firstInByte = w * row;
  const int lastInByte = w * (row + 1) - 1;
  const int radius = diameter >> 1;

  int a = 0, r = 0, g = 0, b = 0;
  pixel_t p;

  // iterate over relative position to first pixel of row
  for (int i = -radius; i < w + radius; i++) {
    const int ii = bound(firstInByte + i, firstInByte, lastInByte);
    p = pixelsIn[ii];
    a += p.a;
    r += p.r;
    g += p.g;
    b += p.b;

    if (i >= radius) {
      const int outOffset = i - radius;
      p.a = div[a];
      p.r = div[r];
      p.g = div[g];
      p.b = div[b];
      outRow[outOffset] = p;

      const int j = i - (diameter - 1);
      const int jj = bound(firstInByte + j, firstInByte, lastInByte);
      p = pixelsIn[jj];
      a -= p.a;
      r -= p.r;
      g -= p.g;
      b -= p.b;
    }
  }
}

/**
 * Creates a blurred version of the given `col` of `pixelsIn`. It uses a moving average algorithm
 * such that it reads every pixel of the column just once. The edge pixels are repeated to avoid
 * artifacts.
 *
 * [ 0        ] [          ] [          ] [          ] [ col      ] [          ] [ w-1      ]
 * [          ] [          ] [          ] [          ] [ col+1w   ] [          ] [          ]
 * [          ] [          ] [          ] [          ] [          ] [          ] [          ]
 * [          ] [          ] [          ] [          ] [          ] [          ] [          ]
 * [          ] [          ] [          ] [          ] [          ] [          ] [          ]
 * [          ] [          ] [          ] [          ] [col+(h-1)w] [          ] [          ]
 *
 * Requires a pre-computed `div` table of size `255 * diameter` that maps `x -> x / diameter` (can
 * be rounded)
 */
static inline void internalVerticalBlur(
    pixel_t* pixelsIn,
    pixel_t* outCol,
    int w,
    int h,
    int col,
    int diameter,
    uint8_t* div) {
  const int firstInByte = col;
  const int lastInByte = w * (h - 1) + col;
  const int radiusTimesW = (diameter >> 1) * w;
  const int diameterMinusOneTimesW = (diameter - 1) * w;

  int a = 0, r = 0, g = 0, b = 0;
  pixel_t p;

  // iterate over absolute positions in `pixelsIn`; `w` is the step width for moving down one row
  for (int i = firstInByte - radiusTimesW; i <= lastInByte + radiusTimesW; i += w) {
    const int ii = bound(i, firstInByte, lastInByte);
    p = pixelsIn[ii];
    a += p.a;
    r += p.r;
    g += p.g;
    b += p.b;

    const int outPos = i - radiusTimesW;
    if (outPos >= firstInByte) {
      p.a = div[a];
      p.r = div[r];
      p.g = div[g];
      p.b = div[b];
      *(outCol++) = p;

      const int j = i - diameterMinusOneTimesW;
      const int jj = bound(j, firstInByte, lastInByte);
      p = pixelsIn[jj];
      a -= p.a;
      r -= p.r;
      g -= p.g;
      b -= p.b;
    }
  }
}

/**
 * A native implementation of an iterative box blur algorithm that runs fast and with little extra
 * memory. It requires bitmap's format to be ARGB_8888 and works in-place (see actual memory usage
 * below).
 *
 * The individual box blurs are split up in vertical and horizontal direction. That allows us to
 * use a moving average implementation for blurring individual rows and columns.
 *
 * The runtime is: O(iterations * width * height) and therefore linear in the number of pixels
 *
 * The required memory is: 2 * radius * 256 * 1 Bytes + max(width, height) * 4 Bytes (+constant)
 */
static void BlurFilter_iterativeBoxBlur(
    JNIEnv* env,
    jclass clazz,
    jobject bitmap,
    jint iterations,
    jint radius) {
  UNUSED(clazz);

  if (iterations <= 0 || iterations > BLUR_MAX_ITERATIONS) {
    safe_throw_exception(env, "Iterations argument out of bounds");
    return;
  }

  if (radius <= 0 || radius > BLUR_MAX_RADIUS) {
    safe_throw_exception(env, "Blur radius argument out of bounds");
    return;
  }

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

  // the information written to an output pixels `x` are from `[x-radius, x+radius]` (inclusive)
  const int diameter = radius + 1 + radius;

  // pre-compute division table: speed-up by factor 5(!)
  uint8_t* div = (uint8_t*) malloc(256 * diameter * sizeof(uint8_t));
  if (!div) {
    safe_throw_exception(env, "Failed to allocate memory: div");
    return;
  }

  // the following lines will fill-up at least the first `255 * diameter` entries with the mapping
  // `div[x] = (x + r) / d` (i.e. division of x by d rounded to the nearest number).
  uint8_t* ptr = div;
  for (int r = 0; r <= radius; r++) {
    *(ptr++) = 0;
  }
  for (int b = 1; b <= 255; b++) {
    for (int d = 0; d < diameter; d++) {
      *(ptr++) = b;
    }
  }

  // temporary array for the output of the currently blurred row OR column
  pixel_t* tempRowOrColumn = (pixel_t*) malloc(max(w, h) * sizeof(pixel_t));
  if (!tempRowOrColumn) {
    free(div);
    safe_throw_exception(env, "Failed to allocate memory: tempRowOrColumn");
    return;
  }

  for (int i = 0; i < iterations; i++) {
    // blur rows one-by-one
    for (int row = 0; row < h; row++) {
      internalHorizontalBlur(pixelPtr, tempRowOrColumn, w, row, diameter, div);

      // copy output row pixels back to bitmap
      memcpy(pixelPtr + w * row, tempRowOrColumn, w * sizeof(pixel_t));
    }

    // blur columns one-by-one
    for (int col = 0; col < w; col++) {
      internalVerticalBlur(pixelPtr, tempRowOrColumn, w, h, col, diameter, div);

      // copy output column pixels back to bitmap
      pixel_t* ptr = pixelPtr + col;
      for (int row = 0; row < h; row++) {
        *ptr = tempRowOrColumn[row];
        ptr += w;
      }
    }
  }

  free(tempRowOrColumn);
  free(div);

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
