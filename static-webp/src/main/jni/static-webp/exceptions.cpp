/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include "exceptions.h"

namespace facebook {
namespace imagepipeline {

void safeThrowJavaException(JNIEnv* env, jclass exceptionCls, const char* msg) {
  if (!env->ExceptionCheck()) {
    env->ThrowNew(exceptionCls, msg);
  }
}

} // namespace imagepipeline
} // namespace facebook
