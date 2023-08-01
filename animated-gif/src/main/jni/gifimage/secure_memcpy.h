/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#define ERR_POTENTIAL_BUFFER_OVERFLOW 34 // matches with ERANGE in errno.h

/**
 * Bounds checking (i.e. destination) wrapper for std::memcpy. This version adds
 * bounds checking capability and returns an error code if there's any potential
 * buffer overflow detected. Error handling is mandatory. Note that using this
 * function without error handling does not guarantee security.
 *
 * @param destination
 *      Pointer to the destination where the content is to be copied.
 * @param destination_size
 *      Max number of bytes to modify in the destination (typically the size of
 * the destination buffer).
 * @param source
 *      Pointer to the source of data to be copied.
 * @param count
 *      Number of bytes to copy.
 * @return int
 *      Returns zero on success and non-zero value on error.
 */
__attribute__((warn_unused_result)) inline int try_checked_memcpy(
    void* destination,
    size_t destination_size,
    const void* source,
    size_t count) {
  if (destination_size < count) {
    return ERR_POTENTIAL_BUFFER_OVERFLOW;
  }
  memcpy(destination, source, count);
  return 0;
}
