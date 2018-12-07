/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <jni.h>

#include "Bitmaps.h"
#include "exceptions.h"
#include "logging.h"
#include "NativeMemoryChunk.h"

jclass jRuntimeException_class;

/**
 * Executed when libimagepipeline.so is loaded.
 *
 * <p> Responsibilites:
 * - looks up and stores global references to Java classes used by native code
 * - registers native methods of image pipeline classes
 *
 * <p> In case of method registration failure a RuntimeException is thrown.
 */
__attribute__((visibility("default")))
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  JNIEnv* env;

  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return -1;
  }

  // find java classes
  jclass runtimeException = env->FindClass("java/lang/RuntimeException");
  if (runtimeException == nullptr) {
    LOGE("could not find RuntimeException class");
    return -1;
  }
  jRuntimeException_class =
    reinterpret_cast<jclass>(env->NewGlobalRef(runtimeException));

  // register native methods
  THROW_AND_RETURNVAL_IF(
      registerBitmapsMethods(env) == JNI_ERR,
      "Could not register Bitmaps methods",
      -1);

  THROW_AND_RETURNVAL_IF(
      registerDalvikDecoderMethods(env) == JNI_ERR,
      "Could not register DalvikPurgeableDecoder methods",
      -1);

  THROW_AND_RETURNVAL_IF(
      registerNativeMemoryChunkMethods(env) == JNI_ERR,
      "Could not register NativeMemoryChunk methods",
      -1);

  return JNI_VERSION_1_6;
}
