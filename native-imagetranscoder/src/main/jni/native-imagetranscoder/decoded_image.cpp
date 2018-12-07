/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include "decoded_image.h"

namespace facebook {
namespace imagepipeline {

int bytesPerPixel(PixelFormat pixel_format) {
  switch (pixel_format) {
  case PixelFormat::RGB:
    return 3;
  case PixelFormat::RGBA:
    return 4;
  default:
    return 0;
  }
}

} }
