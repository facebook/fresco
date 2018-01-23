---
docid: customizing-image-formats
title: Customizing Image Formats
layout: docs
permalink: /docs/customizing-image-formats.html
---

In general, two parts are involved until an image can be displayed on screen:
1. decoding the image
2. rendering the decoded image

Fresco allows you to customize both of these parts. For example, it's possible to add a custom image decoder for an existing image format or for a new image format that uses Fresco's built-in rendering architecture to render bitmaps. Or, it's possible to let the built-in decoder handle decoding and then create a custom Drawable used to render the image on screen. And, of course, you can also do both. These customizations can be either registered globally when Fresco is initialized or locally for selected images only.

The (much simplified) decoding and rendering process looks like this:
1. The encoded image is downloaded from the network or loaded from the disk cache.
2. The `ImageFormat` of the `EncodedImage` is determined using a class called `ImageFormatChecker`, which has a list of `ImageFormat.FormatChecker` objects, one for each recognized image format.
3. The `EncodedImage` is decoded using a suitable `ImageDecoder` for the given format and returns an object that extends `CloseableImage`, which represents the decoded image.
4. From a list of `DrawableFactory` objects, the first one that is able to handle the `CloseableImage` is used to create a `Drawable`.
5. The drawable is rendered on screen.

It is possible to add custom image formats by adding an `ImageFormat.FormatChecker` for step 2. You can supply custom `ImageDecoder`s to add decoding support for new image formats or override built-in decoding. Finally, you can supply a custom `DrawableFactory` to use a custom `Drawable` for rendering the image.

All default image formats can be found in `DefaultImageFormats` and `DefaultImageFormatChecker`, the default drawable factory is in `PipelineDraweeController` and several samples for customizing them can be found in the Showcase sample app.

## Custom decoders

Let's start with an example. In order to create a custom decoder, simply implement the `ImageDecoder` interface:

```java
public class CustomDecoder implements ImageDecoder {

  @Override
  public CloseableImage decode(
      EncodedImage encodedImage,
      int length,
      QualityInfo qualityInfo,
      ImageDecodeOptions options) {
    // Decode the given encodedImage and return a
    // corresponding (decoded) CloseableImage.
    CloseableImage closeableImage = ...;
    return closeableImage;
  }
}
```

The given encoded image can be used to return a class that extends `CloseableImage`, which represents the decoded image and which will then be automatically cached for you. You can either return one of the existing `CloseableImage` types, like `CloseableStaticBitmap` for bitmaps, or define your own `CloseableImage` class.

Custom decoders can be set globally or locally on a per-image basis. For local overrides, you can set the custom decoder as follows:

```java
ImageDecoder customDecoder = ...;
Uri uri = ...;
draweeView.setController(
  Fresco.newDraweeControllerBuilder()
        .setImageRequest(
          ImageRequestBuilder.newBuilderWithSource(uri)
              .setImageDecodeOptions(
                  ImageDecodeOptions.newBuilder()
                      .setCustomImageDecoder(customDecoder)
                      .build())
              .build())
        .build());
```

**NOTE:** If you're supplying a custom decoder, it will be used for all images. The default decoder will be completely bypassed.

## Custom image formats

You simply create a new `ImageFormat` object and hold on to it in your code:

```java
private static final ImageFormat CUSTOM_FORMAT = new ImageFormat("format name", "format file extension");
```

All supported default image formats can be found in `DefaultImageFormats`.

Then, we need to create a custom `ImageFormat.FormatChecker` that is used to detect your new image format. The format checker has 2 methods, one to determine the number of header bytes required to make the decision (keep this number as small as possible since this operation is performed for all images) and the actual `determineFormat` method, which should return **the same `ImageFormat` instance**, `CUSTOM_FORMAT` in this example - or `null` if the image is of a different format. A simple format checker could look like this:

```java
public static class ColorFormatChecker implements ImageFormat.FormatChecker {

  private static final byte[] HEADER = ImageFormatCheckerUtils.asciiBytes("my_header");

  @Override
  public int getHeaderSize() {
    return HEADER.length;
  }

  @Nullable
  @Override
  public ImageFormat determineFormat(byte[] headerBytes, int headerSize) {
    if (headerSize < getHeaderSize()) {
      return null;
    }
    if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, HEADER)) {
      return CUSTOM_FORMAT;
    }
    return null;
  }
}
```

The third component required for custom image format is a custom decoder as explained above that can create the actual decoded image.

You have to register your custom image format with Fresco by supplying a `ImageDecoderConfig` to Fresco when it is initialized. Similarly, you can override the default decoding behavior by using a built-in image format:

```java
ImageFormat myFormat = ...;
ImageFormat.FormatChecker myFormatChecker = ...;
ImageDecoder myDecoder = ...;
ImageDecoderConfig imageDecoderConfig = new ImageDecoderConfig.Builder()
  .addDecodingCapability(
    myFormat,
    myFormatChecker,
    myDecoder)
  .build();

ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
  .setImageDecoderConfig(imageDecoderConfig)
  .build();

Fresco.initialize(context, config);
```

## Custom drawables

If a `DraweeController` is used to load the image (e.g. if you're using a `DraweeView`), a corresponding `DrawableFactory` is used to create a drawable to render the decoded image based on the `CloseableImage`. If you're manually using the image pipeline, you have to handle the `CloseableImage` itself.

If you use one of the built-in types, like `CloseableStaticBitmap`, the `PipelineDraweeController` already knows how to handle the format and will create a `BitmapDrawable` for you. If you want to override that behavior or add support for custom `CloseableImage`s, you have to implement a drawable factory:

```java
public static class CustomDrawableFactory implements DrawableFactory {

  @Override
  public boolean supportsImageType(CloseableImage image) {
    // You can either override a built-in format, like `CloseableStaticBitmap`
    // or your own implementations.
    return image instanceof CustomCloseableImage;
  }

  @Nullable
  @Override
  public Drawable createDrawable(CloseableImage image) {
    // Create and return your custom drawable for the given CloseableImage.
    // It is guaranteed that the `CloseableImage` is an instance of the
    // declared classes in `supportsImageType` above.
    CustomCloseableImage myCloseableImage = (CustomCloseableImage) image;
    Drawable myDrawable = ...; //e.g. new CustomDrawable(myCloseableImage)
    return myDrawable;
  }
}
```

In order to use your drawable factory, you can either use a global or local override.

### Global custom drawable override

You have to register all global drawable factories when Fresco is initialized:

```java
DrawableFactory myDrawableFactory = ...;

DraweeConfig draweeConfig = DraweeConfig.newBuilder()
  .addCustomDrawableFactory(myDrawableFactory)
  .build();

Fresco.initialize(this, imagePipelineConfig, draweeConfig);
```

### Local custom drawable override

For local overrides, the `PipelineDraweeControllerBuilder` offers methods to set custom drawable factories:

```java
DrawableFactory myDrawableFactory = ...;
Uri uri = ...;

simpleDraweeView.setController(Fresco.newDraweeControllerBuilder()
  .setUri(uri)
  .setCustomDrawableFactory(factory)
  .build());
```
