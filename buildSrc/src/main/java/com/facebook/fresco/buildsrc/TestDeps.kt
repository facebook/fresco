/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.buildsrc

object TestDeps {
  const val assertjCore = "org.assertj:assertj-core:2.9.1"

  const val junit = "junit:junit:4.12"

  const val mockitoCore = "org.mockito:mockito-core:2.28.2"

  const val mockitoInline = "org.mockito:mockito-inline:2.28.2"
  const val mockitoKotlin = "org.mockito.kotlin:mockito-kotlin:2.2.11"

  const val festAssertCore = "org.easytesting:fest-assert-core:2.0M10"

  const val robolectric = "org.robolectric:robolectric:4.12.2"

  const val truth = "com.google.truth:truth:1.0.1"

  object AndroidX {
    const val espressoCore = "androidx.test.espresso:espresso-core:3.1.1"
    const val testRules = "androidx.test:rules:1.1.1"
    const val testRunner = "androidx.test:runner:1.1.1"
  }

  object Powermock {
    private const val version = "2.0.9"

    const val apiMockito = "org.powermock:powermock-api-mockito2:$version"
    const val moduleJunit4 = "org.powermock:powermock-module-junit4:$version"
    const val moduleJunit4Rule = "org.powermock:powermock-module-junit4-rule:$version"
    const val classloadingXstream = "org.powermock:powermock-classloading-xstream:$version"
  }
}
