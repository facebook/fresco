/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
