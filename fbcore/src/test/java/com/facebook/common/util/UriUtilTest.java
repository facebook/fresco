/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.util;

import android.net.Uri;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters;

/** Unit test for {@link UriUtilTest}. */
@RunWith(ParameterizedRobolectricTestRunner.class)
public class UriUtilTest {

  private static final String FB_COM = "www.facebook.com";
  private static final List<String> PATHS_GIVEN = Arrays.asList("a", "b", "c");

  private static final List<String> KEYS_GIVEN = Arrays.asList("key1", "key2", "key3");
  private static final List<String> VALS_GIVEN = Arrays.asList("val1", "val2", "val3");

  private static final List<String> EMPTY_LIST = Collections.<String>emptyList();
  private static final List<String> NO_PATHS = EMPTY_LIST;
  private static final List<String> NO_KEYS = EMPTY_LIST;
  private static final List<String> NO_VALS = EMPTY_LIST;

  private String scheme;

  public UriUtilTest(String scheme) {
    this.scheme = scheme;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] {"http"}, new Object[] {"https"}, new Object[] {"ftp"}, new Object[] {"file"});
  }

  @Test
  public void testWithParams() {
    assertConversionFromUriToUrl(FB_COM, PATHS_GIVEN, KEYS_GIVEN, VALS_GIVEN);
    assertConversionFromUriToUrl(FB_COM, NO_PATHS, KEYS_GIVEN, VALS_GIVEN);
  }

  @Test
  public void testWithoutParams() {
    assertConversionFromUriToUrl(FB_COM, PATHS_GIVEN, NO_KEYS, NO_VALS);
    assertConversionFromUriToUrl(FB_COM, NO_PATHS, NO_KEYS, NO_VALS);
  }

  @Test
  public void testNull() {
    org.junit.Assert.assertNull(UriUtil.uriToUrl(null));
  }

  @Test
  public void testBadHostname() {
    assertConversionFromUriToUrl("www", NO_PATHS, NO_KEYS, NO_VALS);
    assertConversionFromUriToUrl(".www", NO_PATHS, NO_KEYS, NO_VALS);
    assertConversionFromUriToUrl("ww.w", NO_PATHS, NO_KEYS, NO_VALS);
    assertConversionFromUriToUrl("www.", NO_PATHS, NO_KEYS, NO_VALS);
    assertConversionFromUriToUrl("?k=v", NO_PATHS, NO_KEYS, NO_VALS);
  }

  @Test
  public void testListParameters() {
    assertConversionFromUriToUrl(
        FB_COM, PATHS_GIVEN, KEYS_GIVEN, Arrays.asList("[val11, val12]", "[val21, val22]"));
  }

  @Test
  public void testBadPaths() {
    assertConversionFromUriToUrl(FB_COM, Arrays.asList("a.b", "b//c"), NO_KEYS, NO_VALS);
    assertConversionFromUriToUrl(FB_COM, Arrays.asList("a?b", "b\\?c"), NO_KEYS, NO_VALS);
    assertConversionFromUriToUrl(FB_COM, Arrays.asList("{", "}"), NO_KEYS, NO_VALS);
  }

  @Test
  public void withQueryParameterReplacesExistingValuesWithoutRewritingUnrelatedQuery() {
    String original =
        "https://facebook.com/story?first=1&join_id=old&q=hello+world&flag&encoded=%2f%2B&join%5Fid=older&first=2#section%201";
    Uri uri = Uri.parse(original);

    Uri result = UriUtilKt.withQueryParameter(uri, "join_id", "current");

    org.junit.Assert.assertEquals(original, uri.toString());
    org.junit.Assert.assertEquals(
        "https://facebook.com/story?first=1&q=hello+world&flag&encoded=%2f%2B&first=2&join_id=current#section%201",
        result.toString());
  }

  @Test
  public void withQueryParameterEncodesOnlyNewNameAndValue() {
    Uri uri = Uri.parse("https://facebook.com/story#section%201");

    Uri result = UriUtilKt.withQueryParameter(uri, "new name", "hello+world &more");

    org.junit.Assert.assertEquals(
        "https://facebook.com/story?new%20name=hello%2Bworld%20%26more#section%201",
        result.toString());
  }

  @Test
  public void withQueryParameterPreservesEmptyAndValuelessComponents() {
    Uri uri = Uri.parse("https://facebook.com/story?flag&&empty=&");

    Uri result = UriUtilKt.withQueryParameter(uri, "join_id", "current");

    org.junit.Assert.assertEquals(
        "https://facebook.com/story?flag&&empty=&&join_id=current", result.toString());
  }

  @Test
  public void withQueryParameterPreservesQuestionMarksAndFragment() {
    Uri uri =
        Uri.parse(
            "https://facebook.com/story?next=https%3A%2F%2Fexample.com%2F%3Fa%3D1%26b%3D2&literal=what?still-query#fragment?still-fragment");

    Uri result = UriUtilKt.withQueryParameter(uri, "join_id", "current");

    org.junit.Assert.assertEquals(
        "https://facebook.com/story?next=https%3A%2F%2Fexample.com%2F%3Fa%3D1%26b%3D2&literal=what?still-query&join_id=current#fragment?still-fragment",
        result.toString());
  }

  @Test
  public void withQueryParameterReplacesOnlyExistingParameterWithoutExtraSeparator() {
    Uri uri = Uri.parse("https://facebook.com/story?join_id=old");

    Uri result = UriUtilKt.withQueryParameter(uri, "join_id", "current");

    org.junit.Assert.assertEquals("https://facebook.com/story?join_id=current", result.toString());
  }

  @Test
  public void withQueryParameterReturnsExactlyOneReplacementValue() {
    Uri uri =
        Uri.parse("https://facebook.com/story?id=1&join_id=old&source=messenger&join_id=older");

    Uri result = UriUtilKt.withQueryParameter(uri, "join_id", "current");

    org.junit.Assert.assertEquals(
        Collections.singletonList("current"), result.getQueryParameters("join_id"));
    org.junit.Assert.assertEquals("1", result.getQueryParameter("id"));
    org.junit.Assert.assertEquals("messenger", result.getQueryParameter("source"));
  }

  @Test
  public void withQueryParameterPreservesMultipleValuesForOtherParameters() {
    Uri uri = Uri.parse("https://facebook.com/story?id=1&id=2");

    Uri result = UriUtilKt.withQueryParameter(uri, "join_id", "current");

    org.junit.Assert.assertEquals(Arrays.asList("1", "2"), result.getQueryParameters("id"));
  }

  @Test
  public void withQueryParameterLeavesOpaqueUriUnchanged() {
    Uri uri = Uri.parse("mailto:user@example.com");

    org.junit.Assert.assertSame(uri, UriUtilKt.withQueryParameter(uri, "join_id", "current"));
  }

  private void assertConversionFromUriToUrl(
      String authority, List<String> paths, List<String> keys, List<String> values) {

    Uri.Builder builder = new Uri.Builder().scheme(scheme).authority(authority);

    for (String path : paths) {
      builder.appendPath(path);
    }

    Iterator<String> keyIter = keys.iterator();
    Iterator<String> valIter = values.iterator();
    while (keyIter.hasNext() && valIter.hasNext()) {
      String key = keyIter.next();
      String val = valIter.next();
      builder.appendQueryParameter(key, val);
    }

    Uri uri = builder.build();
    URL url = UriUtil.uriToUrl(uri);

    org.junit.Assert.assertEquals(uri.toString(), url.toString());
  }
}
