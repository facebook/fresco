/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include "exceptions_handler.h"

namespace facebook {
namespace imagepipeline {

void throwJavaException(JNIEnv* env, jclass exceptionCls, const char* msg) {
  if (!env->ExceptionCheck()) {
    env->ThrowNew(exceptionCls, msg);
  }
}

} }
