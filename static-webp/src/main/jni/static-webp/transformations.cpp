/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
#include <algorithm>

#include <jni.h>

#include "exceptions.h"
#include "java_globals.h"
#include "transformations.h"

namespace facebook {
namespace imagepipeline {

RotationType getRotationTypeFromDegrees(JNIEnv* env, uint16_t degrees) {
  switch (degrees) {
  case 0:
    return RotationType::ROTATE_0;
  case 90:
    return RotationType::ROTATE_90;
  case 180:
    return RotationType::ROTATE_180;
  case 270:
    return RotationType::ROTATE_270;
  default:
    THROW_AND_RETURNVAL_IF(
        true,
        "wrong rotation angle",
        RotationType::ROTATE_0);
  }
}

} }
