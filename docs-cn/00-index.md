---
id: index
title: 引入Fresco
layout: docs-cn
permalink: /docs-cn/index.html
next: compile-in-android-studio.html
---

类库发布到了Maven中央库:

## 1. Android Studio 或者 Gradle

```groovy
dependencies {
  compile 'com.facebook.fresco:fresco:{{site.current_version}}+'
}
```

## 2. Intellij IDEA 或者 Maven:

这是一个在 Intellij IDEA 的工程简单示例： [https://github.com/liaohuqiu/fresco-demo-for-maven](https://github.com/liaohuqiu/fresco-demo-for-maven) ，可供参考。

在[这个 issue](https://github.com/facebook/fresco/issues/239) 解决之前，pom 中相关依赖缺少 type 字段，通过以下方式无法直接引入：

```xml
<dependency>
  <groupId>com.facebook.fresco</groupId>
  <artifactId>fresco</artifactId>
  <version>LATEST</version>
</dependency>
```

需要这样：

```
<!-- use this version, exclude all the other version from the other libraries. -->
<dependency>
    <groupId>com.android.support</groupId>
    <artifactId>support-v4</artifactId>
    <version>21.0.3</version>
    <type>aar</type>
</dependency>

<!-- begin of fresco -->
<dependency>
    <groupId>com.facebook.fresco</groupId>
    <artifactId>fresco</artifactId>
    <version>{{site.current_version}}</version>
    <type>aar</type>
    <exclusions>
        <exclusion>
            <groupId>com.android.support</groupId>
            <artifactId>support-v4</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.facebook.fresco</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.facebook.fresco</groupId>
    <artifactId>fbcore</artifactId>
    <type>aar</type>
    <version>{{site.current_version}}</version>
</dependency>
<dependency>
    <groupId>com.facebook.fresco</groupId>
    <artifactId>drawee</artifactId>
    <type>aar</type>
    <version>{{site.current_version}}</version>
    <exclusions>
        <exclusion>
            <groupId>com.android.support</groupId>
            <artifactId>support-v4</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.facebook.fresco</groupId>
            <artifactId>fbcore</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.facebook.fresco</groupId>
    <artifactId>imagepipeline</artifactId>
    <type>aar</type>
    <version>{{site.current_version}}</version>
    <exclusions>
        <exclusion>
            <groupId>com.android.support</groupId>
            <artifactId>support-v4</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.facebook.fresco</groupId>
            <artifactId>fbcore</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<!-- end of fresco -->
```

很丑陋对吧，抱歉目前暂时只能这样，有更好的办法请一定告诉我。

刷新 Maven 工程，下载引用，下载完成之后，将：

```
gen-external-apklibs/com.facebook.fresco_imagepipeline_{版本号}/jni
```

目录下的三个文件夹：`armeabi`，`armeabi-v7a`，`x86` 这三个文件夹拷贝到 `libs` 文件夹下。


## 3. Eclipse ADT

首先，下载[这个文件](https://github.com/facebook/fresco/releases/download/v{{site.current_version}}/frescolib-v{{site.current_version}}.zip).

解压后，你会看到一个目录：frescolib，注意这个目录。

1. 从菜单 “文件(File)”，选择导入(Import)
2. 展开 Android, 选择 "Existing Android Code into Workspace"， 下一步。
3. 浏览，选中刚才解压的的文件中的 frescolib 目录。
4. 这5个项目应该都会被添加到工程： drawee， fbcore， fresco， imagepipeline， imagepipeline-okhttp。请确认前4个项目一定是被选中的。点击完成。
5. 右键，项目，选择属性，然后选择 Android。
6. 点击右下角的 Add 按钮，选择 fresco，点击 OK，再点击 OK。

现在，fresco 就导入到项目中了，你可以开始编译了。如果编译不通过，可以尝试清理资源，或者重启 Eclipse。

如果你想在网络层使用 OkHttp，请看[这里](using-other-network-layers.html#_).

如果 support-v4 包重复了，删掉 frescolib/imagepipeline/libs 下的即可。

```
建议尽早使用 Android Studio。
```
