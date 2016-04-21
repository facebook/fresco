---
docid: gotchas
title: Gotchas
layout: docs
permalink: /docs/gotchas.html
prev: troubleshooting.html
next: wrap-content.html
---

#### Don't use ScrollViews

If you want to scroll through a long list of images, you should use a [RecyclerView](http://developer.android.com/reference/android/support/v7/widget/RecyclerView.html), [ListView](https://developer.android.com/reference/android/widget/ListView.html), or [GridView](https://developer.android.com/reference/android/widget/GridView.html). All of these re-use their child views continually as you scroll through them. Fresco descendant views receive the system events that let them manage memory correctly.

`ScrollView` does not do this. Thus, Fresco views aren't told when they have gone off-screen, and hold onto their image memory until your Fragment or Activity is stopped. Your app will be at a much greater risk of OOMs.

#### Don't downcast

It is tempting to downcast objects returned by Fresco classes into actual objects that appear to give you greater control. At best, this will result in fragile code that gets broken in next release; at worst, it will lead to very subtle bugs.

#### Don't use getTopLevelDrawable

`DraweeHierarchy.getTopLevelDrawable()` should **only** be used by DraweeViews. Client code should almost never interact with it.

The sole exception is [custom views](writing-custom-views.html). Even there, the top-level drawable should never be downcast. We may change the actual type of the drawable in future releases.

#### Don't re-use DraweeHierarchies

Never call ```DraweeView.setHierarchy``` with the same argument on two different views. Hierarchies are made up of Drawables, and Drawables on Android cannot be shared among multiple views.

#### Re-use Drawable resource IDs, not Java Drawable objects

This is for the same reason as the above. Drawables cannot be shared in multiple views.

You can freely use the same `@drawable` resource ID as a placeholder, error, or retry in multiple `SimpleDraweeViews` in XML. If you are using `GenericDraweeHierarchyBuilder`, you must call [Resources.getDrawable](http://developer.android.com/reference/android/content/res/Resources.html#getDrawable(int)) separate for *each* hierarchy. Do not call it just once and pass it to multiple hierarchies!

#### Do not control hierarchy directly

Do not interact with `SettableDraweeHierarchy` methods (`reset`, `setImage`, ...). Those are to be used by controller only. Do NOT be tempted to use `setControllerOverlay` in order to set an overlay. This method is to be called by controller only, and it refers to a very special controller overaly. If you just need to display an overlay see [Drawee branches] (http://frescolib.org/docs/drawee-branches.html#Overlays).

#### Don't set images directly on a DraweeView

Currently ```DraweeView``` is a subclass of Android's ImageView. This has various methods to set an image (such as setImageBitmap, setImageDrawable)

If you set an image directly, you will completely lose your ```DraweeHierarchy```, and will not get any results from the image pipeline.

#### Don't use ImageView attributes or methods with DraweeView

Any XML attribute or method of ImageView not found in [View](http://developer.android.com/reference/android/view/View.html) will not work on a DraweeView. Typical cases are `src`, `scaleType`, `adjustViewBounds`, etc. Don't use those. DraweeView has its own counterparts as explained in the other sections of this documentation. Any ImageView attrribute or method will be removed in the upcoming release, so please don't use those.
