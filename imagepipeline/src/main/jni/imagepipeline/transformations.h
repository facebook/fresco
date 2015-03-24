#ifndef TRANSFORMATIONS_H
#define TRANSFORMATIONS_H

#include <stdint.h>

#include <jni.h>

namespace facebook {
namespace imagepipeline {

/**
 * Rotation types.
 */
enum class RotationType { ROTATE_0, ROTATE_90, ROTATE_180, ROTATE_270 };

/**
 * Transforms degrees into RotationType
 */
RotationType getRotationTypeFromDegrees(JNIEnv* env, uint16_t degrees);

/**
 * Scale factor to be used for resizing.
 */
class ScaleFactor {
 public:
  ScaleFactor(uint8_t numerator, uint8_t denominator)
  : numerator_(numerator), denominator_(denominator) {}

  uint8_t getNumerator() const {
    return numerator_;
  }

  uint8_t getDenominator() const {
    return denominator_;
  }

  bool shouldScale() const {
    return numerator_ != denominator_ && denominator_ > 0;
  }

  int scale(int dimension) const {
    if (!shouldScale()) {
      return dimension;
    }
    return (dimension * numerator_) / denominator_;
  }

 private:
  const uint8_t numerator_;
  const uint8_t denominator_;
};

} }

#endif /* TRANSFORMATIONS_H */
