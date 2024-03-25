import com.facebook.fresco.buildsrc.Deps

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.vanniktech.maven.publish")
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
  compileOnly(Deps.inferAnnotation)

  implementation(project(":middleware"))
  implementation(project(":fbcore"))
  implementation(project(":imagepipeline"))
  implementation(Deps.avifAndroid)
}