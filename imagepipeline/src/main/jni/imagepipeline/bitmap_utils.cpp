/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <android/bitmap.h>

#include "bitmap_utils.h"

namespace facebook {
namespace imagepipeline {

BitmapPixelsLock::BitmapPixelsLock(JNIEnv* env, jobject bitmap)
    : env_(env), bitmap_(bitmap), ptr_(NULL) {
  const int lockResult = AndroidBitmap_lockPixels(env_, bitmap_, (void**)&ptr_);
  if (lockResult != ANDROID_BITMAP_RESULT_SUCCESS) {
    ptr_ = NULL;
  }
}


BitmapPixelsLock::~BitmapPixelsLock() {
  if (ptr_) {
    jthrowable pendingException = env_->ExceptionOccurred();
    if (!pendingException) {
      AndroidBitmap_unlockPixels(env_, bitmap_);
      return;
    }
    // unlockPixels by itself should not throw, but it can't run if
    // there's an exception pending
    env_->ExceptionClear();
    AndroidBitmap_unlockPixels(env_, bitmap_);
    env_->Throw(pendingException);
  }
}

} }
