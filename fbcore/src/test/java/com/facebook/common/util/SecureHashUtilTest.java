/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests {@link SecureHashUtil}, covering both the historical allocate-per-call path and the
 * per-thread {@code MessageDigest} reuse path gated by {@link
 * SecureHashUtil#getReuseDigestInstances()}.
 */
@RunWith(RobolectricTestRunner.class)
public class SecureHashUtilTest {

  private static final byte[] INPUT = "the quick brown fox".getBytes(StandardCharsets.UTF_8);

  @After
  public void tearDown() {
    // reuseDigestInstances is process-wide static state; restore the default so it does not leak
    // into other tests.
    SecureHashUtil.setReuseDigestInstances(false);
  }

  @Test
  public void testDefaultDoesNotReuseDigests() {
    assertThat(SecureHashUtil.getReuseDigestInstances()).isFalse();
  }

  @Test
  public void testKnownSha1Value() {
    // SHA-1 of the empty string, identical on both code paths.
    String expected = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    SecureHashUtil.setReuseDigestInstances(false);
    assertThat(SecureHashUtil.makeSHA1Hash("")).isEqualTo(expected);

    SecureHashUtil.setReuseDigestInstances(true);
    assertThat(SecureHashUtil.makeSHA1Hash("")).isEqualTo(expected);
  }

  @Test
  public void testReuseProducesIdenticalHashesAcrossAlgorithms() {
    SecureHashUtil.setReuseDigestInstances(false);
    String sha1PerCall = SecureHashUtil.makeSHA1Hash(INPUT);
    String sha256PerCall = SecureHashUtil.makeSHA256Hash(INPUT);
    String md5PerCall = SecureHashUtil.makeMD5Hash(INPUT);
    String base64PerCall = SecureHashUtil.makeSHA1HashBase64(INPUT);

    SecureHashUtil.setReuseDigestInstances(true);
    assertThat(SecureHashUtil.makeSHA1Hash(INPUT)).isEqualTo(sha1PerCall);
    assertThat(SecureHashUtil.makeSHA256Hash(INPUT)).isEqualTo(sha256PerCall);
    assertThat(SecureHashUtil.makeMD5Hash(INPUT)).isEqualTo(md5PerCall);
    assertThat(SecureHashUtil.makeSHA1HashBase64(INPUT)).isEqualTo(base64PerCall);
  }

  @Test
  public void testReuseIsStableAcrossRepeatedCalls() {
    SecureHashUtil.setReuseDigestInstances(true);
    String first = SecureHashUtil.makeSHA1Hash(INPUT);
    // The cached digest is reused (and reset) on every subsequent call; the output must not drift.
    assertThat(SecureHashUtil.makeSHA1Hash(INPUT)).isEqualTo(first);
    assertThat(SecureHashUtil.makeSHA1Hash(INPUT)).isEqualTo(first);
  }

  @Test
  public void testReuseResetsBetweenDifferentInputs() {
    SecureHashUtil.setReuseDigestInstances(true);
    String hashA = SecureHashUtil.makeSHA1Hash("aaaa");
    String hashB = SecureHashUtil.makeSHA1Hash("bbbb");
    // Hashing "aaaa" again after "bbbb" must yield the original value, proving the reused digest is
    // reset() and not left in a partially-updated state.
    String hashAAgain = SecureHashUtil.makeSHA1Hash("aaaa");

    assertThat(hashB).isNotEqualTo(hashA);
    assertThat(hashAAgain).isEqualTo(hashA);
  }

  @Test
  public void testStreamHashMatchesByteArrayHash() throws Exception {
    SecureHashUtil.setReuseDigestInstances(false);
    String expected = SecureHashUtil.makeMD5Hash(INPUT);

    assertThat(SecureHashUtil.makeMD5Hash(new ByteArrayInputStream(INPUT))).isEqualTo(expected);

    SecureHashUtil.setReuseDigestInstances(true);
    assertThat(SecureHashUtil.makeMD5Hash(new ByteArrayInputStream(INPUT))).isEqualTo(expected);
  }

  @Test
  public void testStreamHashSurvivesReentrantHashing() throws Exception {
    SecureHashUtil.setReuseDigestInstances(false);
    String expected = SecureHashUtil.makeMD5Hash(INPUT);

    // The stream hash holds its digest across caller-supplied read() code. If that code hashes with
    // the same algorithm on the same thread, a shared per-thread digest would be reset mid-hash and
    // the outer result would silently be wrong -- so the stream path must not share one.
    SecureHashUtil.setReuseDigestInstances(true);
    assertThat(SecureHashUtil.makeMD5Hash(new ReentrantHashingInputStream(INPUT)))
        .isEqualTo(expected);
  }

  @Test
  public void testConcurrentHashingIsThreadIsolated() throws Exception {
    final String[] inputs = {"alpha", "beta", "gamma", "delta"};
    SecureHashUtil.setReuseDigestInstances(false);
    final String[] expected = new String[inputs.length];
    for (int i = 0; i < inputs.length; i++) {
      expected[i] = SecureHashUtil.makeSHA1Hash(inputs[i]);
    }

    SecureHashUtil.setReuseDigestInstances(true);
    final CountDownLatch start = new CountDownLatch(1);
    final List<String> mismatches = new CopyOnWriteArrayList<>();
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < inputs.length; i++) {
      final int index = i;
      Thread thread =
          new Thread(
              () -> {
                try {
                  start.await();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return;
                }
                for (int n = 0; n < 200; n++) {
                  String actual = SecureHashUtil.makeSHA1Hash(inputs[index]);
                  if (!expected[index].equals(actual)) {
                    mismatches.add(inputs[index] + " -> " + actual);
                    return;
                  }
                }
              });
      threads.add(thread);
      thread.start();
    }
    start.countDown();
    for (Thread thread : threads) {
      thread.join();
    }

    assertThat(mismatches).isEmpty();
  }

  /**
   * Stream that re-enters {@link SecureHashUtil} with the same algorithm on every read, modelling a
   * decorating or lazily-populated stream that itself hashes.
   */
  private static final class ReentrantHashingInputStream extends InputStream {
    private final byte[] data;
    private int pos;

    ReentrantHashingInputStream(byte[] data) {
      this.data = data;
    }

    @Override
    public int read() {
      return pos >= data.length ? -1 : data[pos++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) {
      SecureHashUtil.makeMD5Hash("re-entrant call");
      if (pos >= data.length) {
        return -1;
      }
      // One byte per call, so the outer hash spans many re-entrant calls.
      b[off] = data[pos++];
      return 1;
    }
  }
}
