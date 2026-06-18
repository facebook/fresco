/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <gtest/gtest.h>

// Pull in the source under test directly so the test target does not have to
// link the rest of the JNI plumbing in init.cpp / JpegTranscoder.cpp.  The
// indirection through the BUCK `headers` map keeps the source's
// `#include "java_globals.h"` / `#include "jpeg_error_handler.h"` resolving
// against the original files.
#include "jpeg_error_handler.cpp" // @nolint

// Stub for the extern global declared in java_globals.h.  These tests do not
// exercise the JNI throwing path (which dereferences this), but the symbol
// must be defined so the included translation unit links cleanly.
jclass jRuntimeExceptionclass = nullptr;

namespace facebook::imagepipeline::jpeg {
namespace {

class JpegErrorHandlerTest : public ::testing::Test {};

// jpeg_std_error() (called inside the constructor) installs a default
// error_exit handler that calls `exit()` on errors.  JpegErrorHandler must
// override that with jpegThrow so libjpeg errors propagate as Java exceptions
// instead of killing the process; dropping the override would silently
// re-enable process aborts on corrupt JPEGs.  dinfoPtr / cinfoPtr must also
// start as null because jpegCleanup uses the null check to decide whether to
// call jpeg_destroy_*; uninitialized pointers there would crash on cleanup
// when no compress/decompress struct has been associated yet.
TEST_F(JpegErrorHandlerTest, constructorOverridesDefaultErrorExitWithJpegThrow) {
  JpegErrorHandler handler(/*env=*/nullptr);

  EXPECT_EQ(&jpegThrow, handler.pub.error_exit);
  EXPECT_EQ(nullptr, handler.dinfoPtr);
  EXPECT_EQ(nullptr, handler.cinfoPtr);
}

// setDecompressStruct must establish a *bidirectional* link between the
// libjpeg state and the handler:
//   - dinfo.err points at handler.pub so libjpeg can dispatch errors via
//     `dinfo.err->error_exit(...)`.  The `(JpegErrorHandler*)cinfo->err` cast
//     in jpegSafeThrow / jpegJumpOnException then recovers the handler.
//   - handler.dinfoPtr stores &dinfo so jpegCleanup can later call
//     jpeg_destroy_decompress on it.
// Dropping either assignment would either leak the libjpeg decompress struct
// or cause libjpeg to crash the process on the first decoding error.  The
// compress slot must remain untouched.
TEST_F(JpegErrorHandlerTest, setDecompressStructWiresLibjpegToHandler) {
  JpegErrorHandler handler(/*env=*/nullptr);
  jpeg_decompress_struct dinfo{};

  handler.setDecompressStruct(dinfo);

  EXPECT_EQ(&handler.pub, dinfo.err);
  EXPECT_EQ(&dinfo, handler.dinfoPtr);
  EXPECT_EQ(nullptr, handler.cinfoPtr);
}

// Symmetric counterpart of setDecompressStruct for the encode path.  The
// decompress slot must remain untouched.
TEST_F(JpegErrorHandlerTest, setCompressStructWiresLibjpegToHandler) {
  JpegErrorHandler handler(/*env=*/nullptr);
  jpeg_compress_struct cinfo{};

  handler.setCompressStruct(cinfo);

  EXPECT_EQ(&handler.pub, cinfo.err);
  EXPECT_EQ(&cinfo, handler.cinfoPtr);
  EXPECT_EQ(nullptr, handler.dinfoPtr);
}

// A single handler is wired to a decompress struct and a compress struct at
// the same time during transcoding.  Each pointer must be tracked
// independently so jpegCleanup destroys both libjpeg structs.  If
// dinfoPtr / cinfoPtr were aliased (e.g., one slot via a union), one of the
// libjpeg structs would silently leak on error.
TEST_F(JpegErrorHandlerTest, tracksDecompressAndCompressIndependently) {
  JpegErrorHandler handler(/*env=*/nullptr);
  jpeg_decompress_struct dinfo{};
  jpeg_compress_struct cinfo{};

  handler.setDecompressStruct(dinfo);
  handler.setCompressStruct(cinfo);

  EXPECT_EQ(&dinfo, handler.dinfoPtr);
  EXPECT_EQ(&cinfo, handler.cinfoPtr);
  EXPECT_EQ(&handler.pub, dinfo.err);
  EXPECT_EQ(&handler.pub, cinfo.err);
}

} // namespace
} // namespace facebook::imagepipeline::jpeg
