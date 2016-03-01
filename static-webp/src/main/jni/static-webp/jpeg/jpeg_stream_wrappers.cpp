/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <stdio.h>

#include <jni.h>
#include <jpeglib.h>
#include <jerror.h>

#include "java_globals.h"
#include "jpeg_error_handler.h"
#include "jpeg_stream_wrappers.h"

namespace facebook {
namespace imagepipeline {
namespace jpeg {

/**
 * Default size of stream buffers
 */
static const unsigned int kStreamBufferSize = 8 * 1024;

/**
 * initialize input stream
 */
static void isInitSource(j_decompress_ptr dinfo) {
  JpegInputStreamWrapper* src = (JpegInputStreamWrapper*) dinfo->src;
  JNIEnv* env = src->env;
  src->start = true;
  src->javaBuffer = env->NewByteArray(kStreamBufferSize);
  jpegJumpOnException((j_common_ptr) dinfo);
  src->buffer = (JOCTET*) (*dinfo->mem->alloc_small)(
      (j_common_ptr) dinfo,
      JPOOL_PERMANENT,
      kStreamBufferSize * sizeof(JOCTET));
  if (src->buffer == nullptr) {
    jpegSafeThrow(
        (j_common_ptr) dinfo,
        "Failed to allocate memory for read buffer");
  }
}

/*
 * Fill the input buffer --- called whenever buffer is emptied.
 */
static boolean isFillInputBuffer(j_decompress_ptr dinfo) {
  JpegInputStreamWrapper* src = (JpegInputStreamWrapper*) dinfo->src;
  JNIEnv* env = src->env;
  jint nbytes = env->CallIntMethod(
      src->inputStream,
      midInputStreamRead,
      src->javaBuffer);
  jpegJumpOnException((j_common_ptr) dinfo);

  if (nbytes <= 0) {
    if (src->start) {
      ERREXIT(dinfo, JERR_INPUT_EMPTY);
    }
    src->buffer[0] = (JOCTET) 0xFF;
    src->buffer[1] = (JOCTET) JPEG_EOI;
    nbytes = 2;
  } else {
    env->GetByteArrayRegion(
        src->javaBuffer,
        0,
        kStreamBufferSize,
        (jbyte*) src->buffer);
    jpegJumpOnException((j_common_ptr) dinfo);
  }
  src->public_fields.next_input_byte = src->buffer;
  src->public_fields.bytes_in_buffer = nbytes;
  src->start = false;
  return true;
}

/*
 * Skip data --- used to skip over a potentially large amount of
 * uninteresting data (such as an APPn marker).
 */
static void isSkipInputData(j_decompress_ptr dinfo, long num_bytes) {
  JpegInputStreamWrapper* src = (JpegInputStreamWrapper*) dinfo->src;
  if (num_bytes > 0) {
    if (src->public_fields.bytes_in_buffer > (unsigned long) num_bytes) {
      src->public_fields.next_input_byte += (size_t) num_bytes;
      src->public_fields.bytes_in_buffer -= (size_t) num_bytes;
    } else {
      long to_skip = num_bytes - src->public_fields.bytes_in_buffer;
      JNIEnv* env = src->env;
      // We could at least try to skip appropriate amout of bytes...
      // TODO: 3752653
      env->CallLongMethod(
          src->inputStream,
          midInputStreamSkip,
          (jlong) to_skip);
      jpegJumpOnException((j_common_ptr) dinfo);
      src->public_fields.next_input_byte = nullptr;
      src->public_fields.bytes_in_buffer = 0;
    }
  }
}

/*
 * Terminate source --- called by jpeg_finish_decompress
 * after all data has been read.  Often a no-op.
 */
static void isTermSource(j_decompress_ptr dinfo) {
  /* no work necessary here */
}

JpegInputStreamWrapper::JpegInputStreamWrapper(
    JNIEnv* env,
    jobject inputStream) : inputStream(inputStream), env(env) {
  public_fields.init_source = isInitSource;
  public_fields.fill_input_buffer = isFillInputBuffer;
  public_fields.skip_input_data = isSkipInputData;
  public_fields.resync_to_restart = jpeg_resync_to_restart; /* use default method */
  public_fields.term_source = isTermSource;
  public_fields.bytes_in_buffer = 0;    // forces fill_input_buffer on first read
  public_fields.next_input_byte = NULL; // until buffer loaded
}


/**
  * Initialize output stream
  */
static void osInitDestination(j_compress_ptr cinfo) {
  JpegOutputStreamWrapper* dest = (JpegOutputStreamWrapper*) cinfo->dest;
  JNIEnv* env = dest->env;

  // allocate java byte array
  dest->javaBuffer = env->NewByteArray(kStreamBufferSize);
  jpegJumpOnException((j_common_ptr) cinfo);

  // allocate the output buffer --- it will be released when done with image
  dest->buffer = (JOCTET *) (*cinfo->mem->alloc_small)(
      (j_common_ptr) cinfo,
      JPOOL_IMAGE,
      kStreamBufferSize * sizeof(JOCTET));
  if (dest->buffer == NULL) {
    jpegSafeThrow(
        (j_common_ptr) cinfo,
        "Failed to allcoate memory for byte buffer.");
  }
  dest->public_fields.next_output_byte = dest->buffer;
  dest->public_fields.free_in_buffer = kStreamBufferSize;
}

/**
 * Empty the output buffer --- called whenever buffer fills up.
 */
static boolean osEmptyOutputBuffer(j_compress_ptr cinfo) {
  JpegOutputStreamWrapper* dest = (JpegOutputStreamWrapper*) cinfo->dest;
  JNIEnv* env = dest->env;

  env->SetByteArrayRegion(
      dest->javaBuffer,
      0,
      kStreamBufferSize,
      (jbyte*) dest->buffer);
  jpegJumpOnException((j_common_ptr) cinfo);
  env->CallVoidMethod(
      dest->outputStream,
      midOutputStreamWrite,
      dest->javaBuffer);
  jpegJumpOnException((j_common_ptr) cinfo);
  dest->public_fields.next_output_byte = dest->buffer;
  dest->public_fields.free_in_buffer = kStreamBufferSize;
  return true;
}

/**
 * Terminate destination --- called by jpeg_finish_compress
 * after all data has been written.
 */
static void osTermDestination(j_compress_ptr cinfo) {
  JpegOutputStreamWrapper* dest = (JpegOutputStreamWrapper*) cinfo->dest;
  size_t datacount = kStreamBufferSize - dest->public_fields.free_in_buffer;
  JNIEnv* env = dest->env;

  if (datacount>0) {
    env->SetByteArrayRegion(
        dest->javaBuffer,
        0,
        datacount,
        (jbyte*) dest->buffer);
    jpegJumpOnException((j_common_ptr) cinfo);
    env->CallVoidMethod(
        dest->outputStream,
        midOutputStreamWriteWithBounds,
        dest->javaBuffer,
        0,
        datacount);
    jpegJumpOnException((j_common_ptr) cinfo);
  }
}


JpegOutputStreamWrapper::JpegOutputStreamWrapper(
    JNIEnv* env,
    jobject output_stream) : outputStream(output_stream), env(env) {
  public_fields.init_destination = osInitDestination;
  public_fields.empty_output_buffer = osEmptyOutputBuffer;
  public_fields.term_destination = osTermDestination;
}

} } }
