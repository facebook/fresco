/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>

#include <jni.h>

int initWebPImage(JNIEnv *env);

// Registers jni methods.
__attribute__((visibility("default")))
jint JNI_OnLoad(JavaVM* vm, void* reserved) {
  // get the current env
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }

  int result = initWebPImage(env);
  if (result != JNI_OK) {
    return result;
  }
  return JNI_VERSION_1_6;
}
