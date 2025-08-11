/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import android.net.Uri
import com.facebook.common.time.MonotonicClock
import com.facebook.common.util.UriUtil
import com.facebook.imagepipeline.producers.HttpUrlConnectionNetworkFetcher.HttpUrlConnectionNetworkFetchState
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLConnection
import java.util.LinkedList
import java.util.Queue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HttpUrlConnectionNetworkFetcherTest {
  @Mock private lateinit var fetchState: HttpUrlConnectionNetworkFetchState
  @Mock private lateinit var producerContext: ProducerContext
  @Mock private lateinit var callback: NetworkFetcher.Callback

  private lateinit var fetcher: HttpUrlConnectionNetworkFetcher
  private lateinit var connectionsQueue: Queue<HttpURLConnection>
  private lateinit var mockedUri: MockedStatic<Uri>
  private lateinit var mockedUriUtil: MockedStatic<UriUtil>

  @Before
  @Throws(Exception::class)
  fun setUp() {
    mockedUri = Mockito.mockStatic(Uri::class.java)
    MockitoAnnotations.initMocks(this)

    fetcher =
        HttpUrlConnectionNetworkFetcher(
            "user-agent-blabla", null, Mockito.mock(MonotonicClock::class.java))
    connectionsQueue = LinkedList()
    mockUrlConnections()
    mockUriParse()
    mockUriWithAppendedPath()
    mockFetchState()
  }

  @After
  fun tearDown() {
    mockedUriUtil.close()
  }

  @Throws(Exception::class)
  private fun mockUrlConnections() {
    mockedUriUtil = Mockito.mockStatic(UriUtil::class.java)
    val mockURL = Mockito.mock(URL::class.java)
    Mockito.`when`(mockURL.openConnection())
        .thenAnswer(
            object : Answer<URLConnection> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): URLConnection {
                return connectionsQueue.poll()
              }
            })
    Mockito.`when`(UriUtil.uriToUrl(ArgumentMatchers.any(Uri::class.java))).thenReturn(mockURL)
  }

  private fun mockUriParse() {
    mockedUri
        .`when`<Any> { Uri.parse(ArgumentMatchers.anyString()) }
        .then(
            object : Answer<Uri> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): Uri {
                return mockUri(invocation.getArguments()[0] as String)
              }
            })
  }

  private fun mockUriWithAppendedPath() {
    mockedUri
        .`when`<Any> {
          Uri.withAppendedPath(ArgumentMatchers.any(Uri::class.java), ArgumentMatchers.anyString())
        }
        .then(
            object : Answer<Uri> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): Uri {
                return invocation.getArguments()[0] as Uri
              }
            })
  }

  private fun mockUri(url: String): Uri {
    val uri = Mockito.mock(Uri::class.java)
    Mockito.`when`(uri.toString()).thenReturn(url)
    Mockito.`when`(uri.scheme)
        .then(
            object : Answer<String> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): String {
                return url.substring(0, url.indexOf(':'))
              }
            })
    return uri
  }

  private fun mockFetchState() {
    Mockito.`when`(fetchState.context).thenReturn(producerContext)
    Mockito.`when`(fetchState.uri)
        .then(
            object : Answer<Uri> {
              @Throws(Throwable::class)
              override fun answer(invocation: InvocationOnMock): Uri {
                return mockUri(INITIAL_TEST_URL)
              }
            })
  }

  @After
  fun tearDownStaticMocks() {
    mockedUri.close()
  }

  @Test
  @Throws(IOException::class)
  fun testFetchSendsSuccessToCallback() {
    val inputStream = Mockito.mock(InputStream::class.java)
    val connection = mockSuccessWithStream(inputStream)

    runFetch()

    val inOrder = Mockito.inOrder(callback, connection)
    inOrder.verify(connection).inputStream
    inOrder.verify(callback).onResponse(inputStream, -1)
    inOrder.verify(connection).disconnect()

    Mockito.verifyNoMoreInteractions(callback)
  }

  @Test
  @Throws(IOException::class)
  fun testFetchSendsErrorToCallbackAfterHttpError() {
    val response = mockFailure()

    runFetch()

    Mockito.verify(callback).onFailure(ArgumentMatchers.any(IOException::class.java))
    Mockito.verify(response).disconnect()

    Mockito.verifyNoMoreInteractions(callback)
  }

  @Test
  @Throws(IOException::class)
  fun testFetchSendsSuccessToCallbackAfterRedirect() {
    val redirect = mockRedirectTo(HTTPS_URL)

    val inputStream = Mockito.mock(InputStream::class.java)
    val redirectedConnection = mockSuccessWithStream(inputStream)

    runFetch()

    Mockito.verify(redirect).disconnect()

    val inOrder = Mockito.inOrder(callback, redirectedConnection)
    inOrder.verify(callback).onResponse(inputStream, -1)
    inOrder.verify(redirectedConnection).disconnect()

    Mockito.verifyNoMoreInteractions(callback)
  }

  @Test
  @Throws(IOException::class)
  fun testFetchSendsErrorToCallbackAfterRedirectToSameLocation() {
    val redirect = mockRedirectTo(INITIAL_TEST_URL)
    val success = mockSuccess()

    runFetch()

    Mockito.verify(callback).onFailure(ArgumentMatchers.any(IOException::class.java))
    Mockito.verify(redirect).disconnect()
    verifyZeroInteractions(success)

    Mockito.verifyNoMoreInteractions(callback)
  }

  @Test
  @Throws(IOException::class)
  fun testFetchSendsErrorToCallbackAfterTooManyRedirects() {
    mockRedirectTo(HTTPS_URL)
    mockRedirectTo(INITIAL_TEST_URL)
    mockRedirectTo(HTTPS_URL)
    mockRedirectTo(INITIAL_TEST_URL)
    mockRedirectTo(HTTPS_URL)
    mockRedirectTo(INITIAL_TEST_URL)
    val responseAfterSixRedirects = mockSuccess()

    runFetch()

    Mockito.verify(callback).onFailure(ArgumentMatchers.any(IOException::class.java))
    verifyZeroInteractions(responseAfterSixRedirects)

    Mockito.verifyNoMoreInteractions(callback)
  }

  @Test
  @Throws(Exception::class)
  fun testHttpUrlConnectionTimeout() {
    val url = Mockito.mock(URL::class.java)
    val connection = Mockito.mock(HttpURLConnection::class.java)
    connection.connectTimeout = 30000

    Mockito.`when`(url.openConnection()).thenReturn(connection)

    val expectedException = SocketTimeoutException()
    Mockito.`when`(connection.responseCode).thenThrow(expectedException)

    Mockito.verify(connection).connectTimeout = 30000
  }

  @Test
  @Throws(Exception::class)
  fun testUserAgent() {
    val connection = mockSuccessWithStream(Mockito.mock(InputStream::class.java))

    runFetch()

    Mockito.verify(connection)
        .setRequestProperty(
            ArgumentMatchers.eq("User-Agent"), ArgumentMatchers.eq("user-agent-blabla"))
  }

  @Throws(IOException::class)
  private fun mockSuccess(): HttpURLConnection {
    return mockSuccessWithStream(Mockito.mock(InputStream::class.java))
  }

  @Throws(IOException::class)
  private fun mockSuccessWithStream(inputStream: InputStream): HttpURLConnection {
    val response = Mockito.mock(HttpURLConnection::class.java)
    Mockito.`when`(response.responseCode).thenReturn(200)
    Mockito.`when`(response.inputStream).thenReturn(inputStream)

    queueConnection(response)

    return response
  }

  @Throws(IOException::class)
  private fun mockFailure(): HttpURLConnection {
    val response = Mockito.mock(HttpURLConnection::class.java)
    Mockito.`when`(response.responseCode).thenReturn(404)

    queueConnection(response)

    return response
  }

  @Throws(IOException::class)
  private fun mockRedirectTo(redirectUrl: String): HttpURLConnection {
    val response = Mockito.mock(HttpURLConnection::class.java)
    Mockito.`when`(response.responseCode).thenReturn(301)
    Mockito.`when`(response.getHeaderField("Location")).thenReturn(redirectUrl)

    queueConnection(response)

    return response
  }

  private fun queueConnection(connection: HttpURLConnection) {
    connectionsQueue.add(connection)
  }

  private fun runFetch() {
    fetcher.fetchSync(fetchState, callback)
  }

  companion object {
    const val INITIAL_TEST_URL: String = "http://localhost/"
    const val HTTPS_URL: String = "https://localhost/"
  }
}
