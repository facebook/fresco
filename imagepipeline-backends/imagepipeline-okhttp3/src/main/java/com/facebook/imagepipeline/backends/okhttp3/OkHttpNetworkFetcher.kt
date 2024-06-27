/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.backends.okhttp3

import android.os.Build
import android.os.Looper
import android.os.SystemClock
import com.facebook.imagepipeline.backends.okhttp3.OkHttpNetworkFetcher.OkHttpNetworkFetchState
import com.facebook.imagepipeline.common.BytesRange
import com.facebook.imagepipeline.common.BytesRange.Companion.fromContentRangeHeader
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.BaseNetworkFetcher
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks
import com.facebook.imagepipeline.producers.Consumer
import com.facebook.imagepipeline.producers.FetchState
import com.facebook.imagepipeline.producers.NetworkFetcher
import com.facebook.imagepipeline.producers.ProducerContext
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.Executor
import kotlin.collections.Map
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Network fetcher that uses OkHttp 3 as a backend.
 *
 * @param callFactory custom [Call.Factory] for fetching image from the network
 * @param cancellationExecutor executor on which fetching cancellation is performed if cancellation
 *   is requested from the UI Thread
 * @param disableOkHttpCache true if network requests should not be cached by OkHttp
 */
open class OkHttpNetworkFetcher
@JvmOverloads
constructor(
    private val callFactory: Call.Factory,
    private val cancellationExecutor: Executor,
    disableOkHttpCache: Boolean = true
) : BaseNetworkFetcher<OkHttpNetworkFetchState>() {

  /** @param okHttpClient client to use */
  constructor(
      okHttpClient: OkHttpClient
  ) : this(okHttpClient, okHttpClient.dispatcher().executorService())

  class OkHttpNetworkFetchState(
      consumer: Consumer<EncodedImage>,
      producerContext: ProducerContext
  ) : FetchState(consumer, producerContext) {
    @JvmField var submitTime: Long = 0
    @JvmField var responseTime: Long = 0
    @JvmField var fetchCompleteTime: Long = 0
  }

  private val cacheControl: CacheControl? =
      if (disableOkHttpCache) CacheControl.Builder().noStore().build() else null

  override fun createFetchState(
      consumer: Consumer<EncodedImage>,
      context: ProducerContext
  ): OkHttpNetworkFetchState = OkHttpNetworkFetchState(consumer, context)

  override fun fetch(fetchState: OkHttpNetworkFetchState, callback: NetworkFetcher.Callback) {
    fetchState.submitTime = SystemClock.elapsedRealtime()
    val uri = fetchState.uri
    try {
      val requestBuilder = Request.Builder().url(uri.toString()).get()
      cacheControl?.let(requestBuilder::cacheControl)

      fetchState.context.imageRequest.bytesRange?.let {
        requestBuilder.addHeader("Range", it.toHttpRangeHeaderValue())
      }
      fetchWithRequest(fetchState, callback, requestBuilder.build())
    } catch (e: Exception) {
      // handle error while creating the request
      callback.onFailure(e)
    }
  }

  override fun onFetchCompletion(fetchState: OkHttpNetworkFetchState, byteSize: Int) {
    fetchState.fetchCompleteTime = SystemClock.elapsedRealtime()
  }

  override fun getExtraMap(
      fetchState: OkHttpNetworkFetchState,
      byteSize: Int
  ): Map<String, String>? =
      mapOf(
          QUEUE_TIME to (fetchState.responseTime - fetchState.submitTime).toString(),
          FETCH_TIME to (fetchState.fetchCompleteTime - fetchState.responseTime).toString(),
          TOTAL_TIME to (fetchState.fetchCompleteTime - fetchState.submitTime).toString(),
          IMAGE_SIZE to byteSize.toString())

  protected open fun fetchWithRequest(
      fetchState: OkHttpNetworkFetchState,
      callback: NetworkFetcher.Callback,
      request: Request
  ) {
    val call = callFactory.newCall(request)
    fetchState.context.addCallbacks(
        object : BaseProducerContextCallbacks() {
          override fun onCancellationRequested() {
            if (Looper.myLooper() != Looper.getMainLooper()) {
              call.cancel()
            } else {
              cancellationExecutor.execute { call.cancel() }
            }
          }
        })
    call.enqueue(
        object : Callback {
          @Throws(IOException::class)
          override fun onResponse(call: Call, response: Response) {
            fetchState.responseTime = SystemClock.elapsedRealtime()
            val responseBody: ResponseBody? = response.body()
            responseBody?.use { body ->
              try {
                if (!response.isSuccessful) {
                  handleException(
                      call,
                      makeExceptionFromResponse("Unexpected HTTP code $response", response),
                      callback)
                  return@use
                }
                val responseRange = fromContentRangeHeader(response.header("Content-Range"))
                if (responseRange != null &&
                    !(responseRange.from == 0 &&
                        responseRange.to == BytesRange.TO_END_OF_CONTENT)) {
                  // Only treat as a partial image if the range is not all of the content
                  fetchState.responseBytesRange = responseRange
                  fetchState.onNewResultStatusFlags = Consumer.IS_PARTIAL_RESULT
                }
                val contentLength =
                    if (body.contentLength() < 0) 0 else body.contentLength().toInt()

                callback.onResponse(body.byteStream(), contentLength)
              } catch (e: Exception) {
                handleException(call, e, callback)
              }
            }
                ?: handleException(
                    call,
                    makeExceptionFromResponse("Response body null: $response", response),
                    callback)
          }

          override fun onFailure(call: Call, e: IOException) = handleException(call, e, callback)
        })
  }

  private fun makeExceptionFromResponse(message: String, response: Response): IOException =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        IOException(message, OkHttpNetworkFetcherException.fromResponse(response))
      } else {
        IOException(message)
      }

  /**
   * Handles exceptions.
   *
   * OkHttp notifies callers of cancellations via an IOException. If IOException is caught after
   * request cancellation, then the exception is interpreted as successful cancellation and
   * onCancellation is called. Otherwise onFailure is called.
   */
  private fun handleException(call: Call, e: Exception, callback: NetworkFetcher.Callback) {
    if (call.isCanceled) {
      callback.onCancellation()
    } else {
      callback.onFailure(e)
    }
  }

  private companion object {
    private const val QUEUE_TIME = "queue_time"
    private const val FETCH_TIME = "fetch_time"
    private const val TOTAL_TIME = "total_time"
    private const val IMAGE_SIZE = "image_size"
  }
}
