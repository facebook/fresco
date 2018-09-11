/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;

import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormatChecker;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

/**
 * Unit test for DataFetchProducer
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class DataFetchProducerTest {

  @Test
  public void testBase64() {
    assertFalse(DataFetchProducer.isBase64("data:"));
    assertTrue(DataFetchProducer.isBase64("data:text/plain;param=value;base64"));
  }

  @Test
  public void testSimple() {
    assertArrayEquals(
        "A brief note".getBytes(),
        DataFetchProducer.getData("data:,A%20brief%20note"));
    // 5-character Tamil sequence 'thamizh'
    assertArrayEquals(
        "\u0ba4\u0bae\u0bbf\u0bb4\u0bcd".getBytes(),
        DataFetchProducer.getData(
            "data:text/plain;,%E0%AE%A4%E0%AE%AE%E0%AE%BF%E0%AE%B4%E0%AF%8D"));
  }

  @Test
  public void testGif() throws IOException {
    byte[] gif = DataFetchProducer.getData(
        "data:image/gif;base64," +
            "R0lGODdhMAAwAPAAAAAAAP///ywAAAAAMAAwAAAC8IyPqcvt3wCcDkiLc7C0qwyGHhSWpjQu5yqmCYsapyu" +
            "vUUlvONmOZtfzgFzByTB10QgxOR0TqBQejhRNzOfkVJ+5YiUqrXF5Y5lKh/DeuNcP5yLWGsEbtLiOSpa/TP" +
            "g7JpJHxyendzWTBfX0cxOnKPjgBzi4diinWGdkF8kjdfnycQZXZeYGejmJlZeGl9i2icVqaNVailT6F5iJ9" +
            "0m6mvuTS4OK05M0vDk0Q4XUtwvKOzrcd3iq9uisF81M1OIcR7lEewwcLp7tuNNkM3uNna3F2JQFo97Vriy/" +
            "Xl4/f1cf5VWzXyym7PHhhx4dbgYKAAA7");
    assertEquals(
        DefaultImageFormats.GIF,
        ImageFormatChecker.getImageFormat(new ByteArrayInputStream(gif)));
  }
}
