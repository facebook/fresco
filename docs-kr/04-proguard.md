---
id: proguard
title: Fresco에 프로가드 사용하기
layout: docs-kr
permalink: /docs-kr/proguard.html
prev: using-other-image-loaders.html
next: multiple-apks.html
---

프레스코는 내려받기에 용량이 많아 보일 수 있습니다. 하지만 두려워하지 마세요. ProGuard 툴을 강력하게 추천합니다.

프레스코의 ProGuard 파일 [proguard-fresco.pro](https://raw.githubusercontent.com/facebook/fresco/master/proguard-fresco.pro)을 받으세요. 그리고 프로젝트에 추가하세요.

### Android Studio / Gradle

`build.gradle`파일에 다음을 추가하세요.
```groovy
android {
  buildTypes {
    release {
      minifyEnabled true
      proguardFiles getDefaultProguardFile('proguard-android.txt'),
        'proguard-fresco.pro'
    }
  }
}
```

### Eclipse

[proguard-fresco.pro](https://raw.githubusercontent.com/facebook/fresco/master/proguard-fresco.pro)를 포함시키기 위해[proguard.cfg](http://developer.android.com/tools/help/proguard.html#enabling)을 수정하세요.
