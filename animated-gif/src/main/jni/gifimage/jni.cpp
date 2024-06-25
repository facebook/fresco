/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <assert.h>
#include <fcntl.h>
#include <limits.h>
#include <stdio.h>
#include <unistd.h>

#include <jni.h>

int initGifImage(JNIEnv* env);

// Registers jni methods.
__attribute__((visibility("default"))) jint
JNI_OnLoad(JavaVM* vm, void* reserved) {
  // get the current env
  JNIEnv* env = nullptr;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }

  int result = initGifImage(env);
  if (result != JNI_OK) {
    return result;
  }
  return JNI_VERSION_1_6;
}
