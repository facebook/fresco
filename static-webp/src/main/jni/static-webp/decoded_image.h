/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef _DECODED_IMAGE_H_
#define _DECODED_IMAGE_H_

#include <memory>
#include <vector>

namespace facebook {
namespace imagepipeline {

/**
 * Describes pixel formats of DecodedImage
 */
  enum class PixelFormat {RGB, RGBA};

/**
 * Returns number of bytes per pixel for given PixelFormat
 *
 * @param pixel_format
 */
int bytesPerPixel(PixelFormat pixel_format);

/**
 * Type of pixel buffer
 */
typedef std::unique_ptr<uint8_t, void(*)(uint8_t*)> pixels_t;

/**
 * Class representing an image.
 *
 * <p> Convenient wrapper around pixel buffer and basic information
 * used to pass images between different image decoders/encoders.
 */
class DecodedImage {
 public:
  DecodedImage(
      pixels_t&& pixels,
      PixelFormat pixelFormat,
      unsigned int width,
      unsigned int height,
      std::vector<uint8_t> metadata)
      : pixels_(std::move(pixels)), pixelFormat_(pixelFormat),
        width_(width), height_(height), metadata_(std::move(metadata)) {
  }

  // disallow copying
  DecodedImage(const DecodedImage& other) = delete;
  DecodedImage& operator=(const DecodedImage& other) = delete;

  uint8_t* getPixelsPtr() {
    return pixels_.get();
  }

  const uint8_t* getPixelsPtr() const {
    return pixels_.get();
  }

  PixelFormat getPixelFormat() const {
    return pixelFormat_;
  }

  unsigned int getStride() const {
    return bytesPerPixel(pixelFormat_) * width_;
  }

  unsigned int getWidth() const {
    return width_;
  }

  unsigned int getHeight() const {
    return height_;
  }

  unsigned int getMetadataLength() const {
    return metadata_.size();
  }

  const uint8_t* getMetadataPtr() const {
    return metadata_.size() == 0 ? nullptr : metadata_.data();
  }

 private:
  const pixels_t pixels_;
  const PixelFormat pixelFormat_;
  const unsigned int width_;
  const unsigned int height_;

  const std::vector<uint8_t> metadata_;
};

} }

#endif /* _DECODED_IMAGE_H_ */
