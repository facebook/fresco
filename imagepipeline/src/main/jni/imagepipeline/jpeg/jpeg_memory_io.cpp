#include <vector>

#include <stdio.h>

#include <jpeglib.h>

#include "jpeg_error_handler.h"
#include "jpeg_memory_io.h"

namespace facebook {
namespace imagepipeline {
namespace jpeg {

/**
 * Default size of read/write buffers
 */
static const unsigned int kBufferSize = 8 * 1024;

/**
 * Initialize source.
 *
 * <p> This function is a callback passed to libjpeg and should not be used
 * directly.
 *
 * <p> The method is called by libjpeg before the library performs first
 * read. We use this place to initialize libjpeg's pointer to read buffer
 * and amount of bytes.
 */
static void memSourceInit(j_decompress_ptr dinfo) {
  JpegMemorySource* src = reinterpret_cast<JpegMemorySource*>(dinfo->src);
  src->public_fields.next_input_byte = src->buffer.data();
  src->public_fields.bytes_in_buffer = src->buffer.size();
}

/*
 * Fill the input buffer.
 *
 * <p> This function is a callback passed to libjpeg and should not be used
 * directly.
 *
 * <p> libjpeg will call this when it consumes all bytes  provided in
 * memSourceInit and requires more data to proceed with image decoding.
 * This will happen only if the buffer provided at the beginning of decode
 * operation does not contain full jpeg image. In such case this method
 * will provide extra EOI marker.
 */
static boolean memSourceFillInputBuffer(j_decompress_ptr dinfo) {
  // "read" EOI marker
  // implementation borrowed from libjepg's jdatasrc.c
  static const JOCTET mybuffer[4] = {
    (JOCTET) 0xFF, (JOCTET) JPEG_EOI, 0, 0
  };
  dinfo->src->next_input_byte = mybuffer;
  dinfo->src->bytes_in_buffer = 2;
  return true;
}

/**
 * Skip data.
 *
 * <p> This function is a callback passed to libjpeg and should not be used
 * directly.
 *
 * <p> libjpeg will call this in order to efficiently skip over memory region.
 * In general case this callback should make sure to instruct underlying data
 * source to drop bytes as well, but since we do memory only IO, we are
 * allowed to skip all remaining bytes if number of bytes to skip is higher
 * than size of memory buffer without doing anything extra.
 */
static void memSourceSkipInputData(j_decompress_ptr dinfo, long num_bytes) {
  struct jpeg_source_mgr* src = dinfo->src;
  long bytes_to_skip = std::min<long>(num_bytes, src->bytes_in_buffer);
  src->bytes_in_buffer -= bytes_to_skip;
  src->next_input_byte += bytes_to_skip;
}

/**
 * Terminate source
 *
 * <p> This function is a callback passed to libjpeg and should not be used
 * directly.
 *
 * <p> libjpeg will call this callback after it reads all relevant bytes.
 * At that point of time the source can be released.
 */
static void memSourceTermSource(j_decompress_ptr dinfo) {
  // Do not di anything extra
}

JpegMemorySource::JpegMemorySource() {
  public_fields.init_source = memSourceInit;
  public_fields.fill_input_buffer = memSourceFillInputBuffer;
  public_fields.skip_input_data = memSourceSkipInputData;
  public_fields.resync_to_restart = jpeg_resync_to_restart;
  public_fields.term_source = memSourceTermSource;
  public_fields.bytes_in_buffer = 0;
  public_fields.next_input_byte = nullptr;
}

/**
 * Initialize destination.
 *
 * <p> This function is a callback passed to libjpeg and should not be used
 * directly.
 *
 * <p> This method is called by libjpeg before it writes first byte to the
 * destination. We allocate write buffer and tie its lifetime to passed
 * compress struct.
 */
static void memDestinationInit(j_compress_ptr cinfo) {
  JpegMemoryDestination* dest =
    reinterpret_cast<JpegMemoryDestination*>(cinfo->dest);
  dest->write_memory = (JOCTET *) (*cinfo->mem->alloc_small)(
      (j_common_ptr) cinfo,
      JPOOL_IMAGE,
      kBufferSize * sizeof(JOCTET));
  if (dest->write_memory == nullptr) {
    jpegSafeThrow(
        (j_common_ptr) cinfo,
        "Failed to allocate memory for libjpeg output buffer.");
  }
  dest->public_fields.next_output_byte = dest->write_memory;
  dest->public_fields.free_in_buffer = kBufferSize;
}

/**
 * Empty the output buffer.
 *
 * <p> This function is a callback passed to libjpeg and should not be used
 * directly.
 *
 * <p> When output buffer fills up libjpeg will call this function. Its purpose
 * is to copy bytes from write buffer to the std::vector which accumulates the
 * output.
 */
static boolean memDestinationEmptyOutputBuffer(j_compress_ptr cinfo) {
  JpegMemoryDestination* dest =
    reinterpret_cast<JpegMemoryDestination*>(cinfo->dest);
  dest->buffer.insert(
      dest->buffer.end(),
      dest->write_memory,
      dest->write_memory + kBufferSize);
  dest->public_fields.next_output_byte = dest->write_memory;
  dest->public_fields.free_in_buffer = kBufferSize;
  return true;
}

/**
 * Terminate destination.
 *
 * <p> This function is a callback passed to libjpeg and should not be used
 * directly.
 *
 * <p> This method is called after libjpeg puts the last byte in the write
 * buffer. We use it in order to copy last portion of bytes to the output
 * std::vector.
 */
static void memDestinationTerm(j_compress_ptr cinfo) {
  JpegMemoryDestination* dest =
    reinterpret_cast<JpegMemoryDestination*>(cinfo->dest);
  const long bytes_written = kBufferSize - dest->public_fields.free_in_buffer;
  dest->buffer.insert(
      dest->buffer.end(),
      dest->write_memory,
      dest->write_memory + bytes_written);
}

JpegMemoryDestination::JpegMemoryDestination() {
  public_fields.init_destination = memDestinationInit;
  public_fields.empty_output_buffer = memDestinationEmptyOutputBuffer;
  public_fields.term_destination = memDestinationTerm;
}

} } }
