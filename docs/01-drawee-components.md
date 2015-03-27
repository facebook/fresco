---
id: drawee-components
title: Drawee Image Branches
layout: docs
permalink: /docs/drawee-components.html
prev: using-drawees-code.html
next: scaling.html
---

## Contents

* [Definitions](#Definitions)
* [Actual](#Actual)
* [Placeholder](#Placeholder)
* [Failure](#Failure)
* [Retry](#Retry)
* [Progress Bar](#ProgressBar)
* [Backgrounds](#Backgrounds)
* [Overlays](#Overlays)
* [Pressed State Overlay](#PressedStateOverlay)

## Definitions

This page outlines the different image branches that can be displayed in a Drawee, and how they are set.

Except for the actual image, all of them can be set by an XML attribute. The value in XML must be either an Android drawable or color resource.

 They can also be set by a method in the [GenericDraweeHierarchyBuilder](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchyBuilder.html) class, if [setting programmatically](using-drawees-code.html). In code, the value can either be from resources or be a custom subclass of [Drawable](http://developer.android.com/reference/android/graphics/drawable/Drawable.html).

Some of the items can even be changed on the fly after the hierarchy has been built. These have a method in the [GenericDraweeHierarchy](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchy.html) class.

Several of the drawables can be [scaled](scaling.html).

## Actual

The _actual_ image is the target; everything else is either an alternative or a decoration. This is specified using a URI, which can point to an image over the Internet, a local file, a resource, or a content provider.

This is a property of the controller, not the hierarchy. It therefore is not set by any of the methods used by the other Drawee components.

Instead, use the `setImageURI` method or [set a controller](using-controllerbuilder.html) programmatically.

In addition to the scale type, the hierarchy exposes other methods only for the actual image. These are:

* the focus point (used for the [focusCrop](scaling.html#FocusCrop) scale type only)
* a color filter

Default scale type: `centerCrop`

## Placeholder

The _placeholder_ is shown in the Drawee when it first appears on screen. After you have called `setController` or `setImageURI` to load an image, the placeholder continues to be shown until the image has loaded. 

In the case of a progressive JPEG, the placeholder only stays until your image has reached the quality threshold, whether the default, or one set by your app.

XML attribute: `placeholderImage`  
Hierarchy builder method: `setPlaceholderImage`  
Hierarchy method: `setPlaceholderImage`  
Default value: a transparent [ColorDrawable](http://developer.android.com/reference/android/graphics/drawable/ColorDrawable.html)  
Default scale type: `centerInside`  

## Failure

The _failure_ image appears if there is an error loading your image. The most common cause of this is an invalid URI, or lack of connection to the network.

XML attribute: `failureImage`  
Hierarchy builder method: `setFailureImage`  
Default value: The placeholder image  
Default scale type: `centerInside`

## Retry

The _retry_ image appears instead of the failure image if you have set your controller to enable the tap-to-retry feature. 

You must [build your own Controller](using-controllerbuilder.html) to do this. Then add the following line

```java
.setTapToRetryEnabled(true)
```

The image pipeline will then attempt to retry an image if the user taps on it. Up to four attempts are allowed before the failure image is shown instead.

XML attribute: `retryImage`  
Hierarchy builder method: `setRetryImage`  
Default value: The placeholder image   
Default scale type: `centerInside`

## <a name="ProgressBar"></a>Progress Bar

If specified, the _progress bar_ image is shown as an overlay over the Drawee until the final image is set.

Currently the progress bar remains the same throughout the image load; actually changing in response to progress is not yet supported.

XML attribute: `progressBarImage`  
Hierarchy builder method: `setProgressBarImage`  
Default value: None   
Default scale type: `centerInside`

## Backgrounds

_Background_ drawables are drawn first, "under" the rest of the hierarchy. 

Only one can be specified in XML, but in code more than one can be set. In that case, the first one in the list is drawn first, at the bottom.

Background images don't support scale-types and are scaled to the Drawee size. 

XML attribute: `backgroundImage`  
Hierarchy builder method: `setBackground,` `setBackgrounds`    
Default value: None   
Default scale type: N/A

## Overlays

_Overlay_ drawables are drawn last, "over" the rest of the hierarchy. 

Only one can be specified in XML, but in code more than one can be set. In that case, the first one in the list is drawn first, at the bottom.

Overlay images don't support scale-types and are scaled to the Drawee size. 

XML attribute: `overlayImage`  
Hierarchy builder method: `setOverlay,` `setOverlays`    
Default value: None   
Default scale type: N/A

## <a name="PressedStateOverlay"></a>Pressed State Overlay

The _pressed state overlay_ is a special overlay shown only when the user presses the screen area of the Drawee. For example, if the Drawee is showing a button, this overlay could have the button change color when pressed.

The pressed state overlay doesn't support scale-types.

XML attribute: `pressedStateOverlayImage`  
Hierarchy builder method: `setPressedStateOverlay`    
Default value: None   
Default scale type: N/A



