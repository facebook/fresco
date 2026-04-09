/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ig.imageloader.sample

import android.util.Log
import com.instagram.common.analytics.intf.PhotosQPL
import com.instagram.common.api.base.Downloader
import com.instagram.common.api.base.asynchttp.AsyncHttpRequestCallback
import com.instagram.common.api.base.asynchttp.AsyncHttpRequestToken
import com.instagram.common.api.base.asynchttp.AsyncHttpResponseInfo
import com.instagram.common.api.base.httprequest.HttpRequestPolicy
import com.instagram.common.api.base.model.Header
import com.instagram.common.cache.base.MediaUri
import com.instagram.common.cache.base.MediaUriIntf
import com.instagram.common.session.Session
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple HTTP-based [Downloader] for the sample app. Uses [HttpURLConnection] to fetch images and
 * delivers bytes through IG's [AsyncHttpRequestCallback] interface.
 *
 * This is NOT IG's production HTTP stack — it has no auth headers, traffic management, or request
 * policies. It's sufficient for loading public image URLs.
 *
 * Set via [Downloader.setInstance] in [SampleApplication.onCreate].
 */
class SampleDownloader : Downloader() {

  private val executor =
      java.util.concurrent
          .ThreadPoolExecutor(
              4,
              8,
              30,
              java.util.concurrent.TimeUnit.SECONDS,
              java.util.concurrent.LinkedBlockingQueue(16),
          )
          .also { it.allowCoreThreadTimeOut(true) }
  private val requestIdCounter = AtomicInteger(0)

  override fun downloadImageAsync(
      uri: MediaUriIntf<*>,
      estimatedFileSizeBytes: Int,
      percentage: Float,
      callback: AsyncHttpRequestCallback,
      requestPolicy: HttpRequestPolicy,
      edgeConnectionUUID: String?,
      session: Session?,
      photosQPL: PhotosQPL?,
  ): AsyncHttpRequestToken {
    val requestId = requestIdCounter.incrementAndGet()
    val cancelled = AtomicBoolean(false)
    val urlString = uri.uriToFetch
    Log.d(TAG, "downloadImageAsync called: url=$urlString requestId=$requestId")

    executor.execute {
      if (cancelled.get()) {
        callback.onFailed(IOException("Request cancelled"))
        return@execute
      }

      try {
        Log.d(TAG, "Connecting to $urlString")
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = 60_000
        connection.readTimeout = 120_000
        connection.instanceFollowRedirects = true

        try {
          Log.d(TAG, "Waiting for response from $urlString")
          val responseCode = connection.responseCode
          val contentLength = connection.contentLength
          Log.d(TAG, "Response: HTTP $responseCode, contentLength=$contentLength for $urlString")

          if (responseCode !in 200..299) {
            Log.e(TAG, "HTTP error $responseCode for $urlString")
            callback.onFailed(IOException("HTTP $responseCode: ${connection.responseMessage}"))
            return@execute
          }

          val responseInfo =
              AsyncHttpResponseInfo(
                  responseCode,
                  connection.responseMessage,
                  requestId,
                  connection.headerFields?.flatMap { (key, values) ->
                    if (key != null) values.map { Header(key, it) } else emptyList()
                  } ?: emptyList(),
              )

          callback.onResponseStarted(responseInfo)

          if (cancelled.get()) {
            callback.onFailed(IOException("Request cancelled"))
            return@execute
          }

          if (responseCode in 200..299) {
            val inputStream: InputStream = connection.inputStream
            val buffer = ByteArray(AsyncHttpRequestCallback.MAX_CALLBACK_DATA_BUFFER_SIZE)
            var bytesRead: Int
            var totalBytes = 0
            var chunkCount = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
              if (cancelled.get()) {
                callback.onFailed(IOException("Request cancelled"))
                return@execute
              }
              totalBytes += bytesRead
              chunkCount++
              if (chunkCount % 10 == 1) {
                Log.d(
                    TAG,
                    "Chunk #$chunkCount: $bytesRead bytes (total: $totalBytes/$contentLength) for $urlString",
                )
              }
              callback.onNewData(ByteBuffer.wrap(buffer, 0, bytesRead))
            }

            Log.d(TAG, "Download complete: $chunkCount chunks, $totalBytes bytes for $urlString")
            callback.onComplete()
          } else {
            Log.e(TAG, "HTTP error $responseCode for $urlString")
            callback.onFailed(IOException("HTTP $responseCode: ${connection.responseMessage}"))
          }
        } finally {
          connection.disconnect()
        }
      } catch (e: IOException) {
        Log.e(TAG, "Download failed for $urlString: ${e.message}", e)
        callback.onFailed(e)
      } catch (e: Exception) {
        Log.e(TAG, "Unexpected error downloading $urlString: ${e.message}", e)
        callback.onFailed(IOException("Unexpected error: ${e.message}", e))
      }
    }

    return object : AsyncHttpRequestToken {
      override fun getRequestId(): Int = requestId

      override fun updateBehavior(behavior: HttpRequestPolicy.Behavior) {}

      override fun cancel() {
        cancelled.set(true)
      }
    }
  }

  override fun downloadAsync(
      uri: MediaUri,
      callback: AsyncHttpRequestCallback,
      requestPolicy: HttpRequestPolicy,
      session: Session?,
  ): AsyncHttpRequestToken {
    throw UnsupportedOperationException("SampleDownloader only supports downloadImageAsync")
  }

  override fun download(
      uri: MediaUri,
      requestPolicy: HttpRequestPolicy,
      session: Session?,
  ): Response {
    throw UnsupportedOperationException("SampleDownloader only supports downloadImageAsync")
  }

  override fun downloadVideo(
      uri: MediaUri,
      startRange: Long,
      endRange: Long,
      requestPolicy: HttpRequestPolicy,
      headers: Map<String, String>?,
      callback: Callback?,
      session: Session?,
  ): RangeResponse {
    throw UnsupportedOperationException("SampleDownloader only supports downloadImageAsync")
  }

  companion object {
    private const val TAG = "SampleDownloader"
  }
}
