package com.facebook.imageutils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

object ByteBufferUtil {
  private const val BUFFER_SIZE = 16384 // 16KiB
  private val BUFFER_REF = AtomicReference<ByteArray>()

  @Throws(IOException::class)
  fun fromStream(stream: InputStream): ByteBuffer {
    val outStream = ByteArrayOutputStream(BUFFER_SIZE)

    var buffer = BUFFER_REF.getAndSet(null)
    if (buffer == null) {
      buffer = ByteArray(BUFFER_SIZE)
    }

    var n: Int
    while (stream.read(buffer).also { n = it } >= 0) {
      outStream.write(buffer, 0, n)
    }

    BUFFER_REF.set(buffer)

    val bytes = outStream.toByteArray()

    return rewind(ByteBuffer.allocateDirect(bytes.size).put(bytes))
  }

  private fun rewind(buffer: ByteBuffer) = buffer.position(0) as ByteBuffer
}
