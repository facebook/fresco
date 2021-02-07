/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <jni.h>

#include "exceptions_handler.h"
#include "java_globals.h"
#include "logging.h"
#include "JpegTranscoder.h"

jmethodID midInputStreamRead;
jmethodID midInputStreamSkip;
jmethodID midOutputStreamWrite;
jmethodID midOutputStreamWriteWithBounds;

jclass jRuntimeExceptionclass;

/**
 * Executed when libnative-imagetranscoder.so is loaded.
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
  jRuntimeExceptionclass =
    reinterpret_cast<jclass>(env->NewGlobalRef(runtimeException));

  jclass isClass = env->FindClass("java/io/InputStream");
  THROW_AND_RETURNVAL_IF(isClass == nullptr, "could not find InputStream", -1);

  jclass osClass = env->FindClass("java/io/OutputStream");
  THROW_AND_RETURNVAL_IF(osClass == nullptr, "could not find OutputStream", -1);

  // find java methods
  midInputStreamRead = env->GetMethodID(isClass, "read", "([B)I");
  THROW_AND_RETURNVAL_IF(
      midInputStreamRead == nullptr,
      "failed to register InputStream.read",
      -1);

  midInputStreamSkip = env->GetMethodID(isClass, "skip", "(J)J");
  THROW_AND_RETURNVAL_IF(
      midInputStreamSkip == nullptr,
      "failed to register InputStream.skip",
      -1);

  midOutputStreamWrite = env->GetMethodID(osClass, "write", "([B)V");
  THROW_AND_RETURNVAL_IF(
      midOutputStreamWrite == nullptr,
      "failed to register OutputStream.write",
      -1);

  midOutputStreamWriteWithBounds = env->GetMethodID(osClass, "write", "([BII)V");
  THROW_AND_RETURNVAL_IF(
      midOutputStreamWriteWithBounds == nullptr,
      "failed to register OutputStream.write",
      -1);

  // register native methods
  THROW_AND_RETURNVAL_IF(
      !registerJpegTranscoderMethods(env),
      "Could not register JpegTranscoder methods",
      -1);
  return JNI_VERSION_1_6;
}
