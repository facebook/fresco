#ifndef JPEG_MEMORY_IO_H
#define JPEG_MEMORY_IO_H

#include <type_traits>
#include <vector>

#include <jpeglib.h>

namespace facebook {
namespace imagepipeline {
namespace jpeg {


/**
 * Provides jpeg data from std::vector.
 *
 * <p> This struct is designed to be directly castable to and from
 * jpeg_source_mgr so it can be passed to and from libjpeg.
 */
struct JpegMemorySource {
  /**
   * jpeg_source_mgr is a struct used by libjpeg to encapsulate bytes source.
   * It consists of callbacks (fucntion pointers) and memory pointer to read
   * buffer. Our implementations point public_fields correctly at buffer.
   */
  struct jpeg_source_mgr public_fields;
  std::vector<uint8_t> buffer;

  /**
   * Creates jpeg_source_mgr providing bytes from std::vector.
   */
  JpegMemorySource();

  void setBuffer(std::vector<uint8_t>&& new_buffer) {
    buffer = std::move(new_buffer);
  }
};

/**
 * We cast pointers of type struct jpeg_source_mgr* pointing to public_fields
 * to a pointer of type struct JpegMemorySource* and expect that we obtain a
 * valid pointer to enclosing structure. Assertions below ensure that this
 * assumption is always true.
 */
static_assert(
    std::is_standard_layout<JpegMemorySource>::value,
    "JpegMemorySource has to be type of standard layout");
static_assert(
    offsetof(JpegMemorySource, public_fields) == 0,
    "offset of JpegMemorySource.public_fields should be 0");


/**
 * Stores libjpeg output in memory using std::vector
 *
 * <p> This struct is designed to be directly castable to and from
 * jpeg_destination_mgr so it can be passed to and from libjpeg.
 */
struct JpegMemoryDestination {
  /**
   * jpeg_destination_mgr is a libjpeg struct representing bytes destination.
   * It consists of number of callbacks (function pointers) and pointer to
   * memory serving as write buffer. We initialize that pointer with
   * write_memory
   */
  struct jpeg_destination_mgr public_fields;

  /**
   * Buffer accumulates bytes writen by libjpeg. We don't allow the library
   * to write directly to the buffer. Instead we maintain extra write_memory
   * pointer which is set to a piece of memory allocated via libjpeg memory
   * manager. We instruct libjpeg to write directly to that memory and we
   * copy written bytes to given vector later.
   */
  std::vector<uint8_t> buffer;
  JOCTET *write_memory;

  /**
   * Creates jpeg_destination_mgr storing output bytes in std::vector.
   */
  JpegMemoryDestination();
};

/**
 * We cast pointers of type struct jpeg_destination_mgr* pointing to public_fields
 * to a pointer of type struct JpegMemoryDestination* and expect that we obtain a
 * valid pointer to enclosing structure. Assertions below ensure that this
 * assumption is always true.
 */
static_assert(
    std::is_standard_layout<JpegMemoryDestination>::value,
    "JpegMemoryDestination has to be type of standard layout");
static_assert(
    offsetof(JpegMemoryDestination, public_fields) == 0,
    "offset of JpegMemoryDestination.public_fields should be 0");


} } }

#endif /* JPEG_MEMORY_IO_H */
