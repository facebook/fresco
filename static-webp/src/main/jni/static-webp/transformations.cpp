/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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

RotationType getRotationTypeFromRawExifOrientation(JNIEnv* env, uint16_t exif_orientation) {
  switch (exif_orientation) {
  case 1:
    return RotationType::ROTATE_0;
  case 6:
    return RotationType::ROTATE_90;
  case 3:
    return RotationType::ROTATE_180;
  case 8:
    return RotationType::ROTATE_270;
  case 2:
    return RotationType::FLIP_HORIZONTAL;
  case 4:
    return RotationType::FLIP_VERTICAL;
  case 5:
    return RotationType::TRANSPOSE;
  case 7:
    return RotationType::TRANSVERSE;
  default:
    THROW_AND_RETURNVAL_IF(
        true,
        "wrong exif orientation",
        RotationType::ROTATE_0);
  }
}

} }
