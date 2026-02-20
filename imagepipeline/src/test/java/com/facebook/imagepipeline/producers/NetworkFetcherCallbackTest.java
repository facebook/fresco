/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.imagepipeline.network.NetworkResponseData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NetworkFetcherCallbackTest {

  @Test
  public void testDefaultOnResponseBridgesStreamToLegacy() throws IOException {
    final InputStream[] capturedStream = new InputStream[1];
    final int[] capturedLength = new int[1];

    NetworkFetcher.Callback callback =
        new NetworkFetcher.Callback() {
          @Override
          public void onResponse(InputStream response, int responseContentLength) {
            capturedStream[0] = response;
            capturedLength[0] = responseContentLength;
          }

          @Override
          public void onFailure(Throwable throwable) {}

          @Override
          public void onCancellation() {}
        };

    InputStream stream = new ByteArrayInputStream(new byte[] {1, 2, 3});
    callback.onResponse(new NetworkResponseData.Stream(stream, 3));

    assertThat(capturedStream[0]).isEqualTo(stream);
    assertThat(capturedLength[0]).isEqualTo(3);
  }

  @Test
  public void testDefaultOnResponseBridgesBytesToLegacy() throws IOException {
    final InputStream[] capturedStream = new InputStream[1];
    final int[] capturedLength = new int[1];

    NetworkFetcher.Callback callback =
        new NetworkFetcher.Callback() {
          @Override
          public void onResponse(InputStream response, int responseContentLength) {
            capturedStream[0] = response;
            capturedLength[0] = responseContentLength;
          }

          @Override
          public void onFailure(Throwable throwable) {}

          @Override
          public void onCancellation() {}
        };

    byte[] data = new byte[] {10, 20, 30, 40, 50};
    callback.onResponse(new NetworkResponseData.Bytes(data, 3));

    assertThat(capturedLength[0]).isEqualTo(3);
    byte[] read = new byte[3];
    capturedStream[0].read(read);
    assertThat(read).isEqualTo(new byte[] {10, 20, 30});
  }
}
