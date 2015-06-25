---
layout: default-kr
title: Fresco | 페이스북에서 만든 안드로이드 이미지 라이브러리
id: home
hero: true
---

# 프레스코

Fresco는 안드로이드 어플리케이션을 위해 이미지를 표시하는 강력한 시스템입니다. Fresco는 이미지 로딩과 출력을 처리하기 때문에 이를 위한 추가작업이 필요없습니다.

Fresco *이미지 파이프라인*은 네트워크, 로컬 저장소, 로컬 리소스에서 이미지를 불러옵니다. 데이터를 저장하기 위해 세 단계의 캐쉬를 가집니다. 둘은 메모리에 있고 나머지 하나는 내장 저장소에 둡니다.

Fresco의 *Drawee* 객체는 이미지가 읽어지는 동안 플레이스 홀더(placeholder)를 보여주고 다 받은 후에 자동으로 이미지를 표시합니다. 이미지가 화면에서 벗어나면 자동으로 메모리를 해제합니다.

Fresco는 2.3(진저브레드) 이상 버전의 안드로이드를 지원합니다.

# 특징

### 메모리

압축 해제된 이미지(안드로이드 `Bitmap`)는 많은 메모리를 사용합니다. 메모리 소비는 자바의 가비지 컬렉터의 호출 빈도를 높이고 앱들의 성능을 저하합니다. 안드로이드 5.0에서 향상된 가비지 컬렉터를 쓰지 못하면 문제는 특히 심각합니다.

안드로이드 4.x 이하 버전에서 Fresco는 안드로이드 메모리의 특정 영역에 이미지를 보관합니다. 이미지가 화면에 보이지 않으면 자동으로 메모리 해제를 합니다. 이는 당신에게 작성한 어플리케이션을 빠르게 하며 충돌을 줄입니다.

Fresco를 사용하는 앱은 심지어 저사양의 단말기에서도 수행될 수 있습니다. 지속적으로 이미지 메모리 소비량과 다투지 않으면서 컨트롤할 수 있습니다.

### 스트리밍

점진적 JPEG(Progressive JPEG)은 웹에서 여러 해 사용되었습니다. 이 방식은 먼저 저해상도의 이미지를 읽어들여 화면에 표시하고 점차 고해상도의 이미지를 받아 보여줍니다. 이는 느린 네트워크를 사용하는 사용자들에게 유용합니다.

안드로이드 자체 이미지 라이브러리는 스트리밍을 지원하지 않습니다. Fresco는 스트리밍을 지원합니다. URI만 주어지면 앱은 자동으로 데이터를 받아고 화면을 갱신합니다.

### 애니메이션

앱에서 애니메이션 GIF와 WebP를 사용하는 것은 도전입니다. 각 프레임은 큰 `Bitmap`이고 각 애니메이션은 일련의 프레임입니다. Fresco는 로딩, 프레임의 배치, 메모리 관리를 담당합니다.

### 그리기

Fresco는 `Drawee`를 사용하여 화면을 표시합니다. 이는 몇가지 유용한 기능을 제공합니다.

* 이미지 중앙 대신 사용자가 원하는 지점에서 확대, 축소할 수 있습니다.
* 둥근 모서리나 원형으로 이미지를 표시합니다.
* 네트워크 로딩이 실패했을 때 플레이스 홀더를 클릭하여 이미지를 다시 받습니다.
* 사용자 배경, 오버레이, 프로그레스 바를 이미지에 보여줍니다.
* 이미지를 사용자가 눌렀을 때 커스텀 오버레이를 표시합니다.

### 이미지 불러오기

Fresco 이미지 파이프라인은 다양한 방법으로 불러오기를 개인화합니다.

* 여러 다른 uri를 지정하고 캐쉬에서 하나를 골라 표시합니다.
* 저해상도 이미지를 먼저 보여주고 고해상도 이미지를 받아 교체 합니다.
* 이미지 로딩이 완료되면 앱으로 이벤트를 보냅니다.
* EXIF 썸네일이 있는 이미지면 전체 이미지를 불러오기 전에 썸네일을 보여줍니다. (로컬 이미지만)
* 이미지 리사이즈 및 회전
* 받은 이미지를 즉시 수정
* 제대로 지원못하는 안드로이드 옛 버전에서도 WebP 이미지 디코딩.

# 누가 프레스코를 쓰나요?

* [페이스북](https://play.google.com/store/apps/details?id=com.facebook.katana)
* [메신저](https://play.google.com/store/apps/details?id=com.facebook.orca)
* [페이스북 페이지 매니저](https://play.google.com/store/apps/details?id=com.facebook.pages.app)
* [페이스북 그룹](https://play.google.com/store/apps/details?id=com.facebook.groups)  
* [local.ch](https://play.google.com/store/apps/details?id=ch.local.android)

# 더 많은 정보

* Fresco를 발표하고 기술 설계를 설명한 [블로그 게시물](https://code.facebook.com/posts/366199913563917)
* F8 컨퍼런스의 [발표](https://developers.facebooklive.com/videos/542/move-fast-ensuring-mobile-performance-without-breaking-things) (앞에서 25:00 또는 뒤에서 14:35)
* Fresco [다운로드](docs-kr/index.html)
* [문서](docs-kr/getting-started.html)
* [깃허브](https://github.com/facebook/fresco)의 소스코드
