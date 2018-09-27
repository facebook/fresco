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
  uint8_t r, g, b, a;
} pixel_t;

#define ARRAY_SIZE(a) (sizeof(a) / sizeof((a)[0]))
#define UNUSED(expr) ((void) (expr));
#define POW2(x) ((x) * (x))

// These values are chosen arbitrarily but small enough to avoid integer-overflows
#define BITMAP_MAX_DIMENSION 32768
#define BITMAP_MAX_PIXELS (BITMAP_MAX_DIMENSION * BITMAP_MAX_DIMENSION)
// Transparent pixel color
#define TRANSPARENT_PIXEL_COLOR 0x00000000
// Number of extra pixels to consider for anti-aliasing
#define ANTI_ALIASING_PIXELS 4

static jclass runtime_exception_class;
static inline int min(const int a, const int b) { return a < b ? a : b;}
static inline int max(const int a, const int b) { return a > b ? a : b;}

static void safe_throw_exception(JNIEnv* env, const char* msg) {
  if (!(*env)->ExceptionCheck(env)) {
    (*env)->ThrowNew(env, runtime_exception_class, msg);
  }
}

/**
  * Step function that returns in [0,1]. If the pixel is in the circle it returns 1. Else it returns
  * 0 if the pixel is too far away or a value in ]0,1[ otherwise.
  */
static float getPixelAlpha(const int x, const int y, const int centerX, const int centerY, const float radius) {
  const float distance = POW2(centerX - x) + POW2(centerY - y);
  const float targetDistUpper = POW2(radius + (ANTI_ALIASING_PIXELS / 2.0));
  const float targetDistLower = POW2(max(0.0, radius - (ANTI_ALIASING_PIXELS / 2.0)));
  if (distance >= targetDistUpper) {
    return 0.0;
  } else if (distance < targetDistLower) {
    return 1.0;
  }
  return 1.0 - ((distance - targetDistLower) / (targetDistUpper - targetDistLower));
}

/**
  * Modify the transparency of a pixel.
  */
static void setPixelAlpha(const float alpha, pixel_t* p) {
  // We need to multiply the colors also by alpha, because they are using premultiplied alpha.
  p->a = p->a * alpha;
  p->r = p->r * alpha;
  p->g = p->g * alpha;
  p->b = p->b * alpha;
}

/**
  * This require an image of at least 6x6 pixels to have enough margin for the antialiasing and at
  * the same time, having a radius > 1.
  */
static void toAntiAliasedCircle(
    JNIEnv* env,
    pixel_t* pixelPtr,
    const int w,
    const int h) {
  // Distance from border of real circle
  const int blend_distance = ANTI_ALIASING_PIXELS / 2.0;
  // Radius of the circle with margin for antialiasing.
  const float radius = (min(w,h) / 2.0) - blend_distance;
  // Imaginary center of the circle.
  const float centerX = (w - 1.0L) / 2.0;
  const float centerY = (h - 1.0L) / 2.0;

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

  // Clear top/bottom if height > width
  const size_t line_bytes = sizeof(pixel_t) * w;
  const int top_boundary = max(centerY - (radius + blend_distance), 0);
  const int bottom_boundary = min(centerY + (radius + blend_distance), h);
  for (int i = top_boundary; i >= 0; i--) {
    memset(pixelPtr + i * w, TRANSPARENT_PIXEL_COLOR, line_bytes);
  }
  for (int i = bottom_boundary; i < h; i++) {
    memset(pixelPtr + i * w, TRANSPARENT_PIXEL_COLOR, line_bytes);
  }

  int left_boundary = 0;
  int right_boundary = 0;
  float alpha = 0.0;
  int x_offset = 0;
  const float delta = 2 * blend_distance;
  const float r_square = POW2(radius);
  for (int y = top_boundary; y < bottom_boundary; y++) {
    // Square formula: (x-cx)^2 + (y-cy)^2 = r^2  <=>  (x-cx) = -cx^2 + 2cy*y - y^2 + r^2
    x_offset = -POW2(centerY) + (2*centerY*y) - POW2(y) + r_square;
    // Rows on top/bottom not in the actual circle but rather in the antialiasing 'ring'
    // are traversed entirely for better looking images at top/bottom.
    // There are ANTI_ALIASING_PIXELS lines that will be traversed entirely.
    if (x_offset < 0) {
      for (int x = 0; x < w; x++) {
        alpha = getPixelAlpha(x, y, centerX, centerY, radius);
        setPixelAlpha(alpha, &pixelPtr[(y * w) + x]);

        alpha = getPixelAlpha(x, y, centerX, centerY, radius);
        setPixelAlpha(alpha, &pixelPtr[(y * w) + x]);
      }
    } else {
      // Compute the correct position
      x_offset = sqrt(x_offset);

      // Clear left part of the image keeping an delta margin with the circle
      left_boundary = max(centerX - x_offset, 0);
      left_boundary = max(left_boundary - delta, 0);
      memset(pixelPtr + y * w, TRANSPARENT_PIXEL_COLOR, sizeof(pixel_t) * left_boundary);

      // Clear right part of the image keeping an delta margin with the circle
      right_boundary = min(centerX + x_offset, w);
      right_boundary = min(right_boundary + delta , w);
      memset(pixelPtr + y * w + right_boundary, TRANSPARENT_PIXEL_COLOR, sizeof(pixel_t) * (w - right_boundary));

      // Visit the pixels at the left of the circle at row y to apply antialiasing
      for (int x = left_boundary; x < left_boundary + (2 * delta); x++) {
        alpha = getPixelAlpha(x, y, centerX, centerY, radius);
        setPixelAlpha(alpha, &pixelPtr[(y * w) + x]);
      }
      // Visit the pixels at the right of the circle at row y to apply antialiasing
      for (int x = right_boundary - (2*delta); x < right_boundary; x++) {
        alpha = getPixelAlpha(x, y, centerX, centerY, radius);
        setPixelAlpha(alpha, &pixelPtr[(y * w) + x]);
      }
    }
  }
}

/**
 * Modified midpoint circle algorithm. Clears all pixels outside the circle.
 */
static void toCircle(
    JNIEnv* env,
    pixel_t* pixelPtr,
    const int w,
    const int h) {
  const int centerX = w / 2;
  const int centerY = h / 2;
  const int radius = min(w,h) / 2;

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

static float getBorderPixelWeight(const int x, const int y, const int centerX, const int centerY, const float innerRadius) {
  const float distance = POW2(centerX - x) + POW2(centerY - y);
  const float targetDistUpper = POW2(innerRadius);
  const float targetDistLower = POW2(max(0.0, innerRadius - (ANTI_ALIASING_PIXELS / 2.0)));
  if (distance >= targetDistUpper) {
    return 1.0;
  } else if (distance < targetDistLower) {
    return 0.0;
  }
  return ((distance - targetDistLower) / (targetDistUpper - targetDistLower));
}

static pixel_t colorABGRtoPixel(const int32_t colorABGR) {
  pixel_t pixel;
  pixel.a = (colorABGR >> (3 * 8)) & 0xFF;
  pixel.b = (colorABGR >> (2 * 8)) & 0xFF;
  pixel.g = (colorABGR >> (1 * 8)) & 0xFF;
  pixel.r = (colorABGR >> (0 * 8)) & 0xFF;
  return pixel;
}

static void antialiasBorderPixel(
    pixel_t* pixel,
    const float weight,
    const pixel_t borderPixel) {
  const float weightWithAlpha = weight * (borderPixel.a / 255.0);
  pixel->a = min(255, pixel->a + borderPixel.a * weight);
  pixel->r = (pixel->r * (1.0 - weightWithAlpha) + borderPixel.r * weightWithAlpha);
  pixel->g = (pixel->g * (1.0 - weightWithAlpha) + borderPixel.g * weightWithAlpha);
  pixel->b = (pixel->b * (1.0 - weightWithAlpha) + borderPixel.b * weightWithAlpha);
}

static void paintRowSegment(pixel_t* start, int pixels, int32_t colorABGR) {
  for (int i = 0; i < pixels; i++) {
    memcpy(start + i, &colorABGR, sizeof(colorABGR));
  }
}

static void antialiasInternalBorder(
    pixel_t* pixelPtr,
    pixel_t borderColorPixel,
    const int row,
    const int width,
    const float centerX,
    const float centerY,
    const float inner_x_offset,
    const float innerRadius) {
  int distanceFromCenter = (int) inner_x_offset - 1;

  while(distanceFromCenter >= 0) {
    float weight = getBorderPixelWeight((int) centerX - distanceFromCenter, row, centerX, centerY, innerRadius);
    if(weight == 0){
      return;
    }

    antialiasBorderPixel(&pixelPtr[row * width + (int) centerX - distanceFromCenter], weight, borderColorPixel);
    if (distanceFromCenter == 0) { // avoid antialiasing central pixel twice
      return;
    }
    antialiasBorderPixel(&pixelPtr[row * width + (int) centerX + distanceFromCenter], weight, borderColorPixel);

    distanceFromCenter--;
  }
}

static void drawBorder(
    JNIEnv* env,
    pixel_t* pixelPtr,
    const int w,
    const int h,
    int32_t colorABGR,
    int borderWidth) {
  // Radius of the circle
  const float radius = min(w,h) / 2.0;
  //const float borderSize = radius * 0.038;
  const float borderSize = min(borderWidth, radius-1);
  const float innerRadius = radius - borderSize;
  // increase radius to avoid outer border on the sides

  // Imaginary center of the circle.
  const float centerX = (w - 1.0L) / 2.0;
  const float centerY = (h - 1.0L) / 2.0;

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

  // Clear top/bottom if height > width
  const int top_boundary = max(centerY - radius, 0);
  const int bottom_boundary = min(centerY + radius, h);
  const float outer_r_square = POW2(radius);
  const float inner_r_square = POW2(innerRadius);

  int inner_x_offset = 0;
  float inner_x_offset_sq = 0;
  int outer_x_offset = 0;
  float outer_x_offset_sq = 0;

  pixel_t borderColorPixel = colorABGRtoPixel(colorABGR);

  for (int y = top_boundary; y < bottom_boundary; y++) {
      // Square formula: (x-cx)^2 + (y-cy)^2 = r^2  <=>  (x-cx) = -cx^2 + 2cy*y - y^2 + r^2
      outer_x_offset_sq = -POW2(centerY) + (2*centerY*y) - POW2(y) + outer_r_square;
      inner_x_offset_sq = -POW2(centerY) + (2*centerY*y) - POW2(y) + inner_r_square;

      if(outer_x_offset_sq > 0 && inner_x_offset_sq > 0) {
        outer_x_offset = ceil(sqrt(outer_x_offset_sq));
        inner_x_offset = ceil(sqrt(inner_x_offset_sq));
        int borderSizeInLine = outer_x_offset - inner_x_offset + 1;

        if (borderSizeInLine>0) {
          paintRowSegment(pixelPtr + y * w + (int) centerX - (int) outer_x_offset, borderSizeInLine, colorABGR);
          paintRowSegment(pixelPtr + y * w + (int) centerX + (int) inner_x_offset, borderSizeInLine, colorABGR);
        }
        // internal border antialiasing
        antialiasInternalBorder(pixelPtr, borderColorPixel, y, w, centerX, centerY, inner_x_offset, innerRadius);
      } else if (outer_x_offset >= 0) {
        outer_x_offset = sqrt(outer_x_offset_sq);
        paintRowSegment(pixelPtr + y * w + (int) (centerX - outer_x_offset), (int) outer_x_offset * 2, colorABGR);
      } else if (inner_x_offset >= 0) {
        inner_x_offset = sqrt(inner_x_offset_sq);
        paintRowSegment(pixelPtr + y * w + (int) (centerX - inner_x_offset), (int) outer_x_offset * 2, colorABGR);
      }
  }
}

static int argbToABGR(int argbColor) {
    int r = (argbColor >> 16) & 0xFF;
    int b = argbColor & 0xFF;
    return (argbColor & 0xFF00FF00) | (b << 16) | r;
}


static void toCircleWithOptionalBorder(
    JNIEnv* env,
    jclass clazz,
    jobject bitmap,
    jint colorARGB,
    jint border_width,
    jboolean anti_aliased) {
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

  const int w = bitmapInfo.width;
  const int h = bitmapInfo.height;

  if (w > BITMAP_MAX_DIMENSION || h > BITMAP_MAX_DIMENSION) {
    safe_throw_exception(env, "Bitmap dimensions too large");
    return;
  }

  pixel_t* pixelPtr;

  // Locking pixels such that they will not get moved around during processing
  rc = AndroidBitmap_lockPixels(env, bitmap, (void*) &pixelPtr);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    safe_throw_exception(env, "Failed to lock Bitmap pixels");
    return;
  }

  // DRAW THE BORDER
  if(border_width > 0) {
    drawBorder(env, pixelPtr, w, h, argbToABGR(colorARGB), border_width);
  }

  if (anti_aliased) {
    toAntiAliasedCircle(env, pixelPtr, w, h);
  } else {
    toCircle(env, pixelPtr, w, h);
  }

  // Unlocking the pixels
  rc = AndroidBitmap_unlockPixels(env, bitmap);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    safe_throw_exception(env, "Failed to unlock Bitmap pixels");
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
    jobject bitmap,
    jboolean anti_aliased) {
  toCircleWithOptionalBorder(env, clazz, bitmap, 0, 0, anti_aliased);
}

/**
 * A native implementation for rounding a given bitmap to a circular shape and adds a border around
 * it. The underlying implementation uses a modified midpoint circle algorithm to draw the round
 * border and then clears all pixels starting from the circle all the way to the bitmap edges.
 */
static void RoundingFilter_toCircleWithBorder(
    JNIEnv* env,
    jclass clazz,
    jobject bitmap,
    jint colorARGB,
    jint border_width,
    jboolean anti_aliased) {
  toCircleWithOptionalBorder(env, clazz, bitmap, colorARGB, border_width, anti_aliased);
}

static JNINativeMethod rounding_native_methods[] = {
  { "nativeToCircleFilter",
    "(Landroid/graphics/Bitmap;Z)V",
    (void*) RoundingFilter_toCircle },
  { "nativeToCircleWithBorderFilter",
        "(Landroid/graphics/Bitmap;IIZ)V",
        (void*) RoundingFilter_toCircleWithBorder },
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
