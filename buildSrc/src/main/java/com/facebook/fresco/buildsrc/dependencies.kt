/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.buildsrc

object Deps {
  const val javaxAnnotation = "javax.annotation:javax.annotation-api:1.3.2"
  const val jsr305 = "com.google.code.findbugs:jsr305:3.0.2"

  const val inferAnnotation = "com.facebook.infer.annotation:infer-annotation:0.18.0"

  const val okhttp3 = "com.squareup.okhttp3:okhttp:3.14.9"

  const val volley = "com.android.volley:volley:1.2.1"

  const val avifAndroid = "org.aomedia.avif.android:avif:1.0.1.262e11d"

  object AndroidX {
    const val androidxAnnotation = "androidx.annotation:annotation:1.6.0"
    const val core = "androidx.core:core:1.13.1"
    const val exifInterface = "androidx.exifinterface:exifinterface:1.3.7"
    const val legacySupportCoreUtils = "androidx.legacy:legacy-support-core-utils:1.0.0"
  }

  object Bolts {
    const val tasks = "com.parse.bolts:bolts-tasks:1.4.0"
  }

  object Kotlin {
    const val version = GradleDeps.Kotlin.version

    const val stdlibJdk = "org.jetbrains.kotlin:kotlin-stdlib:$version"
  }

  object Litho {
    private const val version = "0.50.1"

    const val core = "com.facebook.litho:litho-core:$version"
    const val lithoAnnotations = "com.facebook.litho:litho-annotations:$version"
    const val processor = "com.facebook.litho:litho-processor:$version"
    const val widget = "com.facebook.litho:litho-widget:$version"

    object Sections {
      const val core = "com.facebook.litho:litho-sections-core:$version"
      const val processor = "com.facebook.litho:litho-sections-processor:$version"
      const val sectionsAnnotations = "com.facebook.litho:litho-sections-annotations:$version"
      const val widget = "com.facebook.litho:litho-sections-widget:$version"
    }
  }

  object SoLoader {
    private const val version = "0.11.0"
    const val soloaderAnnotation = "com.facebook.soloader:annotation:$version"
    const val nativeloader = "com.facebook.soloader:nativeloader:$version"
    const val soloader = "com.facebook.soloader:soloader:$version"
  }

  object Tools {
    object Flipper {
      private const val version = "0.183.0"
      const val flipper = "com.facebook.flipper:flipper:$version"
    }

    object Stetho {
      private const val version = "1.6.0"
      const val stetho = "com.facebook.stetho:stetho:$version"
      const val okhttp3 = "com.facebook.stetho:stetho-okhttp3:$version"
    }
  }
}
