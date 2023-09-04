/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.callercontext;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class ContextChainTest {

  @Test
  public void testGetStringExtra() {
    ContextChain contextChain =
        new ContextChain(
            "grandchild_tag",
            "grandchild_name",
            ImmutableMap.of("keyA", "valueA"),
            new ContextChain(
                "child_tag",
                "child_name",
                ImmutableMap.of("keyB", "valueB"),
                new ContextChain(
                    "root_tag", "root_name", ImmutableMap.of("keyC", "valueC"), null)));
    assertThat(contextChain.getStringExtra("keyA")).isEqualTo("valueA");
    assertThat(contextChain.getStringExtra("keyB")).isEqualTo("valueB");
    assertThat(contextChain.getStringExtra("keyC")).isEqualTo("valueC");
    assertThat(contextChain.getStringExtra("unknownKey")).isNull();
  }

  @Test
  public void testPutStringExtra() {
    ContextChain contextChain =
        new ContextChain(
            "grandchild_tag",
            "grandchild_name",
            null,
            new ContextChain(
                "child_tag",
                "child_name",
                null,
                new ContextChain("root_tag", "root_name", null, null)));
    assertThat(contextChain.getStringExtra("keyA")).isNull();
    contextChain.putObjectExtra("keyA", "valueA");
    assertThat(contextChain.getStringExtra("keyA")).isEqualTo("valueA");
    contextChain.putObjectExtra("keyA", "valueAA");
    assertThat(contextChain.getStringExtra("keyA")).isEqualTo("valueAA");
    assertThat(contextChain.getStringExtra("unknownKey")).isNull();
  }

  @Test
  public void testSerialize() {
    ContextChain contextChain =
        new ContextChain(
            "grandchild_tag",
            "grandchild_name",
            null,
            new ContextChain(
                "child_tag",
                "child_name",
                null,
                new ContextChain("root_tag", "root_name", null, null)));

    assertThat(contextChain.toString())
        .isEqualTo("root_tag:root_name/child_tag:child_name/grandchild_tag:grandchild_name");
  }

  @Test
  public void testSerializeToStringList() {
    ContextChain contextChain =
        new ContextChain(
            "grandchild_tag",
            "grandchild_name",
            null,
            new ContextChain(
                "child_tag",
                "child_name",
                null,
                new ContextChain("root_tag", "root_name", null, null)));

    String[] res = contextChain.toStringArray();

    assertThat(res.length).isEqualTo(3);
    assertThat(res[0]).isEqualTo("root_tag:root_name");
    assertThat(res[1]).isEqualTo("child_tag:child_name");
    assertThat(res[2]).isEqualTo("grandchild_tag:grandchild_name");
  }

  @Test
  public void testSerializedContextChainToArray() {
    ContextChain contextChain =
        new ContextChain(
            "root_tag:root_name/child_tag:child_name/grandchild_tag:grandchild_name", null);
    String[] res = contextChain.toStringArray();
    assertThat(res.length).isEqualTo(3);
    assertThat(res[0]).isEqualTo("root_tag:root_name");
    assertThat(res[1]).isEqualTo("child_tag:child_name");
    assertThat(res[2]).isEqualTo("grandchild_tag:grandchild_name");
  }

  @Test
  public void testSerializedContextChainToString() {
    ContextChain contextChain =
        new ContextChain(
            "root_tag:root_name/child_tag:child_name/grandchild_tag:grandchild_name", null);
    assertThat(contextChain.toString())
        .isEqualTo("root_tag:root_name/child_tag:child_name/grandchild_tag:grandchild_name");
  }

  @Test
  public void testCombiningChains() {
    ContextChain nativeChain =
        new ContextChain("native_tag:native_tag_name/native_child_tag:native_child_tag_name", null);
    ContextChain remoteChain =
        new ContextChain(
            "root_tag:root_name/child_tag:child_name/grandchild_tag:grandchild_name", nativeChain);
    String[] res = remoteChain.toStringArray();
    assertThat(res.length).isEqualTo(5);
    assertThat(res[0]).isEqualTo("native_tag:native_tag_name");
    assertThat(res[1]).isEqualTo("native_child_tag:native_child_tag_name");
    assertThat(res[2]).isEqualTo("root_tag:root_name");
    assertThat(res[3]).isEqualTo("child_tag:child_name");
    assertThat(res[4]).isEqualTo("grandchild_tag:grandchild_name");
  }
}
