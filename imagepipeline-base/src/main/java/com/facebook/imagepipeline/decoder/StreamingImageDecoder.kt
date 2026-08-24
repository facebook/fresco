/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder

import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.image.QualityInfo
import java.io.Closeable

/**
 * Optional capability for an [ImageDecoder] whose format can be decoded from a prefix of the
 * encoded bytes that grows as the image downloads.
 *
 * Progressive JPEG does not need this: every scan can be decoded from scratch, so the pipeline can
 * hand the whole prefix to a stateless decoder each time. Formats that decode incrementally instead
 * carry state between calls — how much has already been consumed, which parts of the frame are
 * already reconstructed — and would otherwise redo that work on every partial result.
 *
 * [ImageDecoder] cannot hold that state: it is a process-wide singleton called concurrently on the
 * decode pool. A [Session] can. The pipeline creates one per image request, routes every result of
 * that request through it (partial ones first, the complete one last) and closes it when the
 * request finishes, succeeds, fails or is cancelled.
 */
interface StreamingImageDecoder {

  /**
   * Called with the first partial result of a request.
   *
   * @return a session, or null if this image cannot usefully be decoded incrementally. On null the
   *   pipeline leaves the request alone: partial results are dropped as before and only the
   *   complete image is decoded, through [ImageDecoder.decode].
   */
  fun maybeCreateStreamingSession(encodedImage: EncodedImage): Session?

  /**
   * Per-request decode state. See [StreamingImageDecoder].
   *
   * The pipeline never calls [decode] concurrently with itself or with [close], and never calls
   * [decode] after [close]. An implementation therefore needs no locking of its own, but its state
   * must still be safely published: a request is decoded on the decode pool and consecutive calls
   * routinely land on different threads there, and [close] can run on the thread that cancelled the
   * request.
   */
  interface Session : Closeable {

    /**
     * Consumes the first [length] bytes of [encodedImage], continuing from whatever this session
     * consumed on earlier calls. Successive calls see the same image growing, so [length] never
     * shrinks.
     *
     * A call that throws ends the session: the pipeline fails the request and closes the session
     * rather than retrying, so an implementation is never asked to resume from a failed decode.
     *
     * @param isComplete true on the final call for this request; no further calls follow. Note that
     *   the bytes may still be short of the whole image — a truncated response also ends the
     *   request — so this means "last chance", not "everything arrived".
     * @return the image as decoded so far, or null if not enough has arrived to draw anything yet.
     *   Null is a normal outcome for the early calls of a stream, not a failure — to fail, throw
     *   [DecodeException]. Null must not be returned when [isComplete] is true.
     */
    fun decode(
        encodedImage: EncodedImage,
        length: Int,
        isComplete: Boolean,
        qualityInfo: QualityInfo,
        options: ImageDecodeOptions,
    ): CloseableImage?

    /**
     * Releases whatever the session holds. Called exactly once per session, on whichever thread
     * ended the request, and possibly long before the image was complete — a cancelled or failed
     * request is closed wherever it happens to end.
     */
    override fun close()
  }
}
