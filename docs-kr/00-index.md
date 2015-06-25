---
id: index
title: 프로젝트에 Fresco 추가하기
layout: docs-kr
permalink: /docs-kr/index.html
next: getting-started.html
---

**프로젝트에 Fresco 추가하기**

Android프로젝트에 Fresco를 추가하는 방법입니다.

### Android Studio 나 Gradle

'build.gradle'파일을 수정하세요. 그리고 아래 구문을 'dependencies'에 추가하세요.

```groovy
dependencies { }
	//your app's other dependencies
    compile 'com.facebook.fresco:fresco:{{site.current_version}}+'
```

### Eclipse ADT

[압축 파일 다운받기](https://github.com/facebook/fresco/releases/download/v{{site.current_version}}/frescolib-v{{site.current_version}}.zip).

압축해제하면 'frescolib'폴더가 만들어 질 것입니다. 이 위치를 기억해 놓으세요.

1.**File**메뉴에서 **Import**를 누르세요.

2.**Android**를 열고, **Existing Android Code Into Workspace**를 누르고 **Next**를 누르세요.

3.**Browse**를 누르고 아까 알려준 *frescolib* 디렉토리를 찾은 후 **OK**를 누르세요.

4.다섯개의 프로젝트가 추가 될 겁니다. *drawee, fbcore, fresco, imagepipeline, imagepipeline-okhttp*. 처음 4개의 프로젝트가 체크됬는지 다시 확인하고 **Finish**를 누르세요.

5.추가된 프로젝트에서 마우스 우버튼(맥사용자는 Ctrl-클릭)을 누르세요. *Properties*를 누르고 *Android*를 누르세요.

6.우측 아래의 *Add*버튼을 누르고 Fresco를 누른 뒤 *OK*를 누르세요.

이제 Fresco를 빌드할 수 있게 되었습니다.

만약 네트워크 계층의 OkHttp를 사용하고 싶다면, [여기](using-other-network-layers.html#_)를 봐보세요.

`android-support-v4.jar`와 관련된 `Jar mismatch`가 발생하면, `frescolib/imagepipeline/libs`의 저 `android-support-v4.jar`파일 하나를 지우시면 됩니다.
