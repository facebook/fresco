/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.buildsrc

object GradleDeps {

  object Android {
    private const val version = "8.1.1"

    const val gradlePlugin = "com.android.tools.build:gradle:$version"
  }

  object Kotlin {
    const val version = "1.8.21"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:$version"
  }

  object Native {
    const val version = "25.2.9519653"
  }

  object Publishing {
    const val gradleMavenPublishPlugin = "com.vanniktech:gradle-maven-publish-plugin:0.25.3"
  }
}
