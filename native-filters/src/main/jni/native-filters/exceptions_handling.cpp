/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include "exceptions_handling.h"

namespace facebook {
namespace imagepipeline {

void safeJavaThrowException(JNIEnv* env, jclass exceptionCls, const char* msg) {
  if (!env->ExceptionCheck()) {
    env->ThrowNew(exceptionCls, msg);
  }
}

} }
