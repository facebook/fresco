/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <jni.h>
#include <webp/demux.h>
#include <webp/decode.h>

#include "exceptions.h"
#include "decoded_image.h"
#include "streams.h"
#include "webp_codec.h"

namespace facebook {
namespace imagepipeline {
namespace webp {

/**
 * Uses libwebp to extract xmp metadata.
 */
const std::vector<uint8_t> extractMetadata(
    JNIEnv* env,
    std::vector<uint8_t>& image_data) {
  // Create WebPDemux from provided data.
  // It is "index" of all chunks. It stores
  // list of pointers to particular chunks, but does
  // not copy memory from provided WebPData.
  WebPData webpdata = {image_data.data(), image_data.size()};
  // Thsnks to using RAII we do not need to worry about
  // releasing WebPDemuxer structure
  auto demux = std::unique_ptr<WebPDemuxer, decltype(&WebPDemuxDelete)>{
      WebPDemux(&webpdata),
      WebPDemuxDelete};
  THROW_AND_RETURNVAL_IF(
      demux == nullptr,
      "Could not create WebPDemux from image. This webp might be malformed.",
      {});

  // find xmp chunk
  WebPChunkIterator chunk_iterator;
  if (!WebPDemuxGetChunk(demux.get(), "XMP ", 1, &chunk_iterator)) {
    // we failed to find "XMP " chunk - don't worry, maybe it was not
    // there. Let the transcode proceed
    WebPDemuxReleaseChunkIterator(&chunk_iterator);
    return {};
  }

  // we managed to find "XMP " chunk, let's return its size and pointer to it
  const unsigned int metadata_length = chunk_iterator.chunk.size;
  const uint8_t* metadata_ptr = chunk_iterator.chunk.bytes;

  WebPDemuxReleaseChunkIterator(&chunk_iterator);

  // If XMP chunk contains no data then return nullptr.
  if (metadata_length == 0) {
    return {};
  }

  return {metadata_ptr, metadata_ptr + metadata_length};
}

std::unique_ptr<DecodedImage> decodeWebpFromInputStream(
    JNIEnv* env,
    jobject is,
    PixelFormat pixel_format) {
  // get image into decoded heap
  auto encoded_image = readStreamFully(env, is);
  RETURNVAL_IF_EXCEPTION_PENDING({});

  // extract metadata
  auto metadata = extractMetadata(env, encoded_image);
  RETURNVAL_IF_EXCEPTION_PENDING({});

  // get pixels
  int image_width = 0;
  int image_height = 0;
  uint8_t* raw_pixels = nullptr;

  switch (pixel_format) {
  case PixelFormat::RGB:
    raw_pixels = WebPDecodeRGB(
        encoded_image.data(),
        encoded_image.size(),
        &image_width,
        &image_height);
    break;

  case PixelFormat::RGBA:
    raw_pixels = WebPDecodeRGBA(
        encoded_image.data(),
        encoded_image.size(),
        &image_width,
        &image_height);
    break;

  default:
    THROW_AND_RETURNVAL_IF(true, "unrecognized pixel format", {});
  }

  auto pixels = pixels_t{raw_pixels, (void(*)(uint8_t*)) &free};

  return std::unique_ptr<DecodedImage>{
      new DecodedImage{
          std::move(pixels),
          pixel_format,
          (unsigned int) image_width,
          (unsigned int) image_height,
          std::move(metadata)}};
}

} } }
