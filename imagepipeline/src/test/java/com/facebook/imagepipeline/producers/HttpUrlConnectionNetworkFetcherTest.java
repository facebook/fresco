/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.provider.ContactsContract;
import com.facebook.common.time.MonotonicClock;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.producers.HttpUrlConnectionNetworkFetcher.HttpUrlConnectionNetworkFetchState;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.Queue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@PrepareForTest({
  HttpUrlConnectionNetworkFetcher.class,
  Uri.class,
  UriUtil.class,
  ContactsContract.class
})
public class HttpUrlConnectionNetworkFetcherTest {

  public static final String INITIAL_TEST_URL = "http://localhost/";
  public static final String HTTPS_URL = "https://localhost/";

  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  @Mock private HttpUrlConnectionNetworkFetchState mMockFetchState;
  @Mock private ProducerContext mMockProducerContext;
  @Mock private NetworkFetcher.Callback mMockCallback;

  private HttpUrlConnectionNetworkFetcher mFetcher;
  private Queue<HttpURLConnection> mConnectionsQueue;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    mFetcher =
        new HttpUrlConnectionNetworkFetcher("user-agent-blabla", null, mock(MonotonicClock.class));
    mConnectionsQueue = new LinkedList<>();
    mockUrlConnections();
    mockUriParse();
    mockUriWithAppendedPath();
    mockFetchState();
  }

  private void mockUrlConnections() throws Exception {
    URL mockUrl = PowerMockito.mock(URL.class);
    PowerMockito.whenNew(URL.class).withAnyArguments().thenReturn(mockUrl);

    PowerMockito.when(mockUrl.openConnection())
        .then(
            new Answer<URLConnection>() {
              @Override
              public URLConnection answer(InvocationOnMock invocation) throws Throwable {
                return mConnectionsQueue.poll();
              }
            });
  }

  private void mockUriParse() {
    PowerMockito.mockStatic(Uri.class);
    PowerMockito.when(Uri.parse(anyString()))
        .then(
            new Answer<Uri>() {
              @Override
              public Uri answer(InvocationOnMock invocation) throws Throwable {
                return mockUri((String) invocation.getArguments()[0]);
              }
            });
  }

  private void mockUriWithAppendedPath() {
    PowerMockito.when(Uri.withAppendedPath(any(Uri.class), anyString()))
        .then(
            new Answer<Uri>() {
              @Override
              public Uri answer(InvocationOnMock invocation) throws Throwable {
                return (Uri) invocation.getArguments()[0];
              }
            });
  }

  private Uri mockUri(final String url) {
    Uri mockUri = mock(Uri.class);
    when(mockUri.toString()).thenReturn(url);
    when(mockUri.getScheme())
        .then(
            new Answer<String>() {
              @Override
              public String answer(InvocationOnMock invocation) throws Throwable {
                return url.substring(0, url.indexOf(':'));
              }
            });
    return mockUri;
  }

  private void mockFetchState() {
    when(mMockFetchState.getContext()).thenReturn(mMockProducerContext);
    when(mMockFetchState.getUri())
        .then(
            new Answer<Uri>() {
              @Override
              public Uri answer(InvocationOnMock invocation) throws Throwable {
                return mockUri(INITIAL_TEST_URL);
              }
            });
  }

  @Test
  public void testFetchSendsSuccessToCallback() throws IOException {
    InputStream mockInputStream = mock(InputStream.class);
    HttpURLConnection mockConnection = mockSuccessWithStream(mockInputStream);

    runFetch();

    InOrder inOrder = inOrder(mMockCallback, mockConnection);
    inOrder.verify(mockConnection).getInputStream();
    inOrder.verify(mMockCallback).onResponse(mockInputStream, -1);
    inOrder.verify(mockConnection).disconnect();

    verifyNoMoreInteractions(mMockCallback);
  }

  @Test
  public void testFetchSendsErrorToCallbackAfterHttpError() throws IOException {
    HttpURLConnection mockResponse = mockFailure();

    runFetch();

    verify(mMockCallback).onFailure(any(IOException.class));
    verify(mockResponse).disconnect();

    verifyNoMoreInteractions(mMockCallback);
  }

  @Test
  public void testFetchSendsSuccessToCallbackAfterRedirect() throws IOException {
    HttpURLConnection mockRedirect = mockRedirectTo(HTTPS_URL);

    InputStream mockInputStream = mock(InputStream.class);
    HttpURLConnection mockRedirectedConnection = mockSuccessWithStream(mockInputStream);

    runFetch();

    verify(mockRedirect).disconnect();

    InOrder inOrder = inOrder(mMockCallback, mockRedirectedConnection);
    inOrder.verify(mMockCallback).onResponse(mockInputStream, -1);
    inOrder.verify(mockRedirectedConnection).disconnect();

    verifyNoMoreInteractions(mMockCallback);
  }

  @Test
  public void testFetchSendsErrorToCallbackAfterRedirectToSameLocation() throws IOException {
    HttpURLConnection mockRedirect = mockRedirectTo(INITIAL_TEST_URL);
    HttpURLConnection mockSuccess = mockSuccess();

    runFetch();

    verify(mMockCallback).onFailure(any(IOException.class));
    verify(mockRedirect).disconnect();
    verifyZeroInteractions(mockSuccess);

    verifyNoMoreInteractions(mMockCallback);
  }

  @Test
  public void testFetchSendsErrorToCallbackAfterTooManyRedirects() throws IOException {
    mockRedirectTo(HTTPS_URL);
    mockRedirectTo(INITIAL_TEST_URL);
    mockRedirectTo(HTTPS_URL);
    mockRedirectTo(INITIAL_TEST_URL);
    mockRedirectTo(HTTPS_URL);
    mockRedirectTo(INITIAL_TEST_URL);
    HttpURLConnection mockResponseAfterSixRedirects = mockSuccess();

    runFetch();

    verify(mMockCallback).onFailure(any(IOException.class));
    verifyZeroInteractions(mockResponseAfterSixRedirects);

    verifyNoMoreInteractions(mMockCallback);
  }

  @Test
  public void testHttpUrlConnectionTimeout() throws Exception {

    URL mockURL = PowerMockito.mock(URL.class);
    HttpURLConnection mockConnection = PowerMockito.mock(HttpURLConnection.class);
    mockConnection.setConnectTimeout(30000);

    PowerMockito.when(mockURL.openConnection()).thenReturn(mockConnection);

    SocketTimeoutException expectedException = new SocketTimeoutException();
    PowerMockito.when(mockConnection.getResponseCode()).thenThrow(expectedException);

    verify(mockConnection).setConnectTimeout(30000);
  }

  @Test
  public void testUserAgent() throws Exception {
    HttpURLConnection mockConnection = mockSuccessWithStream(mock(InputStream.class));

    runFetch();

    verify(mockConnection).setRequestProperty(eq("User-Agent"), eq("user-agent-blabla"));
  }

  private HttpURLConnection mockSuccess() throws IOException {
    return mockSuccessWithStream(mock(InputStream.class));
  }

  private HttpURLConnection mockSuccessWithStream(InputStream is) throws IOException {
    HttpURLConnection mockResponse = mock(HttpURLConnection.class);
    when(mockResponse.getResponseCode()).thenReturn(200);
    when(mockResponse.getInputStream()).thenReturn(is);

    queueConnection(mockResponse);

    return mockResponse;
  }

  private HttpURLConnection mockFailure() throws IOException {
    HttpURLConnection mockResponse = mock(HttpURLConnection.class);
    when(mockResponse.getResponseCode()).thenReturn(404);

    queueConnection(mockResponse);

    return mockResponse;
  }

  private HttpURLConnection mockRedirectTo(String redirectUrl) throws IOException {
    HttpURLConnection mockResponse = mock(HttpURLConnection.class);
    when(mockResponse.getResponseCode()).thenReturn(301);
    when(mockResponse.getHeaderField("Location")).thenReturn(redirectUrl);

    queueConnection(mockResponse);

    return mockResponse;
  }

  private void queueConnection(HttpURLConnection connection) {
    mConnectionsQueue.add(connection);
  }

  private void runFetch() {
    mFetcher.fetchSync(mMockFetchState, mMockCallback);
  }
}
