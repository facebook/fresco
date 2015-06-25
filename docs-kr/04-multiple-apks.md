---
id: multiple-apks
title: 여러개의 APK 등록하기
layout: docs-kr
permalink: /docs-kr/multiple-apks.html
prev: proguard.html
next: building-from-source.html
---

프레스코는 대부분 자바로 만들어 졌습니다. 하지만 C++코드도 조금 들어있습니다. C++코드는 CPU종류("ABIs")에 따라 컴파일 되어야만 하며, 안드로이드에서 할 수 있습니다. 현재 프레스코는 5가지 ABIs를 지원합니다.

1. `armeabiv-v7a`: ARM프로세서 7 혹은 상위버전. 2011-15년 사이 출시된  대부분의 안드로이드 폰은 이것을 사용해 출시되었습니다.
2. `arm64-v8a`: 64비트 ARM 프로세서. 삼성 갤럭시s6같은 새로운 장치.
3. `armeabi`: v5나 v6의 ARM프로세서를 사용한 옛날 폰.
4. `x86`: 타블렛에서 에뮬레이터로 돌아가는 것.
5. `x86_64`: 64비트 타블렛에서 사용되는 것.


내려받을 수 있는 프레스코의 바이너리는 이 5가지 플랫폼의 네이티브`.so`파일 복사본을 가집니다. 프로세서 종류에 따라 APK를 분리하여 만들도록 고려하면 앱의 크기를 줄일 수 있습니다.

앱이 안드로이드 2.3(Gingerbread)에서 돌아가지 않는다면 `armeabi`는 필요 없습니다.

### Android Studio / Gradle

`build.gradle`을 아래와 같이 수정하세요:

```groovy
android {
  // 앱 로직의 남은 부분
    splits {
    abi {
        enable true
        reset()
        include 'x86', 'x86_64', 'arm64-v8a', 'armeabi-v7a', 'armeabi'
        universalApk false
    }
  }
}
```

CPU종류에 따라 더 자세한 것을 보려면 [Android Gradle 문서](http://tools.android.com/tech-docs/new-build-system/user-guide/apk-splits)을 확인하세요.

### 이클립스

기본적으로 이클립스는 모든 CPU를 위해 하나의 APK를 만듭니다. CPU종류에 따라 분리하려면 그래들보다 더 복잡한 노력이 필요합니다.

프레스코의 이클립스를 위한 표준 압축파일 대신, [multi-APK 압축 파일](https://github.com/facebook/fresco/releases/download/v{{site.current_version}}/frescolib-v{{site.current_version}}-multi.zip)을 내려받으세요. 하지만 내려받는 것으론 부족합니다. 프로젝트 설정도 바꿔야 합니다.

1. multi-APK 프로젝트로 만들려면 [Android's 교본](http://developer.android.com/training/multiple-apks/api.html)을 따르세요. 같은  `AndroidManifest.xml`을 각각의 프로젝트에 사용할 수 있습니다.
2. 각각의 프로젝트는`fresco-<flavor>` 프로젝트에 맞게 의존해야 합니다. ( OkHttp를 사용한다면 , `imagepipeline-okhttp-<flavor>`프로젝트를 의존해야 합니다.) ?? 의존하다를 다른걸로 바꿔야 할듯


### Play Store에 올리기

모든 사용자가 받을 수 있는 하나의 APK를 업로드 할 수 있거나 각각의 CPU에 맞는 더 작은 APK로 분리할 수 있습니다. [Play Store 문서](http://developer.android.com/google/play/publishing/multiple-apks.html)가 제공하는 교본을 보고 따라해보세요.
