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
