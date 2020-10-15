/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.buildsrc

object GradleDeps {
    
    object Android {
        private const val version = "4.0.1"

        const val gradlePlugin = "com.android.tools.build:gradle:$version"
    }

    object Kotlin {
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Deps.Kotlin.version}"
    }
    
    object Publishing {
        const val bintrayGradlePlugin = "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4"
        const val androidMavenGradlePlugin = "com.github.dcendents:android-maven-gradle-plugin:2.1"
    }
}
