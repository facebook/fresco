/*
 * Copyright (c) 2017-present, Facebook, Inc.
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
