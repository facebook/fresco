/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef _BITMAP_UTILS_H_
#define _BITMAP_UTILS_H_

#include <cstdint>

#include <jni.h>

namespace facebook {
namespace imagepipeline {

/**
 * Manages bitmap pixels' lock in RAII fashion.
 */
class BitmapPixelsLock {
public:
  BitmapPixelsLock(JNIEnv* env, jobject bitmap);
  ~BitmapPixelsLock();

  // prevent copying
  BitmapPixelsLock(const BitmapPixelsLock&) = delete;
  BitmapPixelsLock& operator=(const BitmapPixelsLock&) = delete;

  // prevent moving
  BitmapPixelsLock(BitmapPixelsLock&&) = delete;
  BitmapPixelsLock& operator=(BitmapPixelsLock&&) = delete;

  uint8_t* getPixelsPtr() { return ptr_; }

private:
  JNIEnv* env_;
  jobject bitmap_;
  uint8_t* ptr_;
};

} }

#endif /* _BITMAP_UTILS_H_ */
