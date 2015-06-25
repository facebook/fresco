---
id: building-from-source
title: 소스로 빌드하기
layout: docs-kr
permalink: /docs-kr/building-from-source.html
prev: multiple-apks.html
---

프레스코의 코드 자체를 수정하려면 원본 소스에서 빌드해야합니다. 대부분의 어플리케이션 프로젝트에서 프레스코를 [포함](index.html#_)시키면 됩니다.

### 준비물

프레스코를 빌드하려면 아래 도구를 시스템에 추가하세요:

1. 안드로이드[SDK](https://developer.android.com/sdk/index.html#Other)
2. SDK매니저에서 **the Support Library the Support Repository를 설치**하세요. 섹션에서 찾을 수 있습니다.
3. 안드로이드 [NDK](https://developer.android.com/tools/sdk/ndk/index.html). 10c 혹은 이후 버전이 필요합니다.
4. [git](http://git-scm.com/) 버전 컨트롤 시스템.

그래들을 받을 필욘 없습니다. 빌드스크립트나 안드로이드 스튜디오가 역활을 대신할 것입니다.

Eclipse users can already [include](index.html#) inde
프레스코는 이클립스, Ant, Maven에서 원본 소스 빌드를 지원하지 않습니다. 지원할 계획도 없습니다. 이클립스 사용자는 대신 프로젝트에 [Fresco를 포함할 수 있습니다.](index.html#eclipse-adt)

### Gradle 설정하기

커맨드-라인과 안드로이드 스튜디오 사용자는 `gradle.properties`파일을 수정할 필요가 있습니다. 이것은 home 디렉토리 - `.gradle`이라 불리는 하위 디렉토리에 보통 위치합니다. 만약 이 파일이 없다면 만드세요.

유닉스와 비슷한 시스템 - 맥 OS X 같은 - 에선 아래와 같이 라인을 추가하세요:

```groovy
ndk.path=/path/to/android_ndk/r10d
```

윈도우 시스템에선 아래와 같이 하세요:

```groovy
ndk.path=C\:\\path\\to\\android_ndk\\r10d
```

윈도우즈의 백슬래시와 콜론은 그래들이 정확하게 읽을 수 있도록하는 이스캐이프문자 입니다.

### 소스 가져오기

```sh
git clone https://github.com/facebook/fresco.git
```

코드가 상주할 `fresco` 디렉토리를 만듭니다.

### 커맨드 명령으로 빌드하기

유닉스와 비슷한 시스템에선 `cd`로 프레스코 디렉토리로 이동할 수 있습니다. 그리고 아래 명령을 실행하세요.

```sh
./gradlew build
```

윈도우즈에선 명령프롬프트를 열어 `cd`로 프레스코가 있는 디렉토리로 이동 후, 아래 명령을 치세요.

```bat
gradlew.bat build
```

### 안드로이드 스튜디오에서 빌드하기

안드로이드 스튜디오의 빠른 시작 다이얼로그에서 Import Project를 누르세요. 프레스코가 있는 디렉토리로 이동 후 `build.gradle`파일을 클릭하세요.

안드로이드 스튜디오는 프레스코를 자동으로 빌드할겁니다.

### 오프라인 빌드하기

처음으로 Fresco를 빌드할 때, 인터넷에 연결되어 있어야만 합니다. Incremental빌드는 그래들의 `--offline` 옵션으로 사용할 수 있습니다.

### 코드 컨트리뷰트 하기

[CONTRIBUTING](https://github.com/facebook/fresco/blob/master/CONTRIBUTING.md) 페이지를 보세요.
