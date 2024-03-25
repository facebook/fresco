import org.jetbrains.kotlin.cli.jvm.main

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.facebook.avifsupport"

  buildToolsVersion = FrescoConfig.buildToolsVersion
  compileSdkVersion = "android-${FrescoConfig.compileSdkVersion}"

  defaultConfig {
    minSdk = FrescoConfig.minSdkVersion
    targetSdk = FrescoConfig.targetSdkVersion
  }
}

dependencies {
  implementation(project(":imagepipeline-base"))
}