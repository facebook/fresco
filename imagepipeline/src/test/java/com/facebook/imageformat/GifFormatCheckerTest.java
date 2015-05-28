/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imageformat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link GifFormatChecker}
 */
@RunWith(RobolectricTestRunner.class)
public class GifFormatCheckerTest {

    @Test
    public void testStaticGifs() throws Exception {
        singleAnimatedGifTest(getNames(5, "gifs/%d.gif"), false);
    }

    @Test
    public void testAnimatedGifs() throws Exception {
        singleAnimatedGifTest(getNames(2, "animatedgifs/%d.gif"), true);
    }

    @Test
    public void testCircularBufferMatchesBytePattern() {
        byte[] testBytes = new byte[]{
                (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04};
        assertTrue(GifFormatChecker.circularBufferMatchesBytePattern(testBytes, 0, testBytes));
        for (int i = 1; i < testBytes.length; i++) {
            assertFalse(GifFormatChecker.circularBufferMatchesBytePattern(testBytes, i, testBytes));
        }

        byte[] testInnerBytes = new byte[] {(byte) 0x01, (byte) 0x02};
        assertTrue(GifFormatChecker.circularBufferMatchesBytePattern(testBytes, 1, testInnerBytes));
        for (int i = 2; i < testBytes.length; i++) {
            assertFalse(GifFormatChecker.circularBufferMatchesBytePattern(
                    testBytes, i, testInnerBytes));
        }

        byte[] testCircularBytes = new byte[] {(byte) 0x04, (byte) 0x00};
        assertTrue(GifFormatChecker.circularBufferMatchesBytePattern(
                testBytes, 4, testCircularBytes));
        for (int i = 0; i < 4; i++) {
            assertFalse(GifFormatChecker.circularBufferMatchesBytePattern(
                    testBytes, i, testCircularBytes));
        }
    }

    private void singleAnimatedGifTest(
            final List<String> resourceNames,
            final boolean expectedAnimated)
            throws Exception {
        for (String name : resourceNames) {
            final InputStream resourceStream = getResourceStream(name);
            try {
                assertSame(
                        "failed with resource: " + name,
                        expectedAnimated,
                        GifFormatChecker.isAnimated(resourceStream));
            } finally {
                resourceStream.close();
            }
        }
    }

    private static List<String> getNames(int amount, String pathFormat) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= amount; ++i) {
            result.add(String.format(pathFormat, i));
        }
        return result;
    }

    private static InputStream getResourceStream(String name) throws IOException {
        InputStream is = GifFormatCheckerTest.class.getResourceAsStream(name);
        assertNotNull("failed to read resource: " + name, is);
        return is;
    }

}
