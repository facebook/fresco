// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fresco.vito.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.net.Uri;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.litho.Diff;
import com.facebook.litho.annotations.Prop;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public abstract class BaseImageSpecTest {

  private static final Set<String> SHOULD_UPDATE_PROPS_WHITELIST =
      new HashSet<String>() {
        {
          add("callerContext");
          add("imageListener");
        }
      };

  private static final Diff<Uri> NULL_URI_DIFF = new Diff<>(null, null);
  private static final Diff<ImageOptions> NULL_IMAGE_OPTIONS_DIFF = new Diff<>(null, null);
  private static final Diff<FrescoContext> NULL_FRESCO_CONTEXT_DIFF = new Diff<>(null, null);
  private static final Diff<Float> NULL_ASPECT_RATIO_DIFF = new Diff<>(null, null);

  private final Method mShouldUpdateMethod;

  public BaseImageSpecTest() {
    final Method[] methods = getSpecClassName().getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      if ("shouldUpdate".equals(methods[i].getName())) {
        mShouldUpdateMethod = methods[i];
        return;
      }
    }
    throw new IllegalStateException("Could not find shouldUpdate method");
  }

  abstract Class getSpecClassName();

  @Test
  public void testShouldUpdateMethodChecksForEveryProp() {
    // Get number of props in FrescoImage.java
    Field[] fields = getSpecClassName().getDeclaredFields();
    List<String> propNames = new LinkedList<>();
    for (int i = 0; i < fields.length; i++) {
      if (fields[i].getAnnotation(Prop.class) != null) {
        propNames.add(fields[i].getName());
      }
    }

    propNames.removeAll(SHOULD_UPDATE_PROPS_WHITELIST);
    final int propsCount = propNames.size();

    // Check number of args in should update method
    final Method[] methods = getSpecClassName().getDeclaredMethods();
    final int parametersCount = mShouldUpdateMethod.getParameterCount();
    assertThat(parametersCount)
        .withFailMessage(
            "Expecting %s props to be checked %s, but shouldUpdate accepts only %d parameters",
            propsCount, propNames, parametersCount)
        .isGreaterThanOrEqualTo(propsCount);
  }

  @Test
  public void test_shouldUpdate_whenNull_thenDoNothing()
      throws InvocationTargetException, IllegalAccessException {
    boolean shouldUpdate =
        (boolean)
            mShouldUpdateMethod.invoke(
                null,
                NULL_URI_DIFF,
                NULL_IMAGE_OPTIONS_DIFF,
                NULL_FRESCO_CONTEXT_DIFF,
                NULL_ASPECT_RATIO_DIFF);

    assertThat(shouldUpdate).isFalse();
  }

  @Test
  public void test_shouldUpdate_whenUriChanged_thenUpdate()
      throws InvocationTargetException, IllegalAccessException {
    Uri uri1 = mock(Uri.class);
    Uri uri2 = mock(Uri.class);

    Diff<Uri> diff = new Diff<>(null, uri1);

    boolean shouldUpdate =
        (boolean)
            mShouldUpdateMethod.invoke(
                null,
                diff,
                NULL_IMAGE_OPTIONS_DIFF,
                NULL_FRESCO_CONTEXT_DIFF,
                NULL_ASPECT_RATIO_DIFF);

    assertThat(shouldUpdate).isTrue();

    diff = new Diff<>(uri1, uri2);

    shouldUpdate =
        (boolean)
            mShouldUpdateMethod.invoke(
                null,
                diff,
                NULL_IMAGE_OPTIONS_DIFF,
                NULL_FRESCO_CONTEXT_DIFF,
                NULL_ASPECT_RATIO_DIFF);

    assertThat(shouldUpdate).isTrue();
  }

  @Test
  public void test_shouldUpdate_whenImageOptionsChanged_thenUpdate()
      throws InvocationTargetException, IllegalAccessException {
    ImageOptions options1 = ImageOptions.create().build();
    ImageOptions options2 = ImageOptions.create().placeholderRes(123).build();

    Diff<ImageOptions> diff = new Diff<>(null, options1);

    boolean shouldUpdate =
        (boolean)
            mShouldUpdateMethod.invoke(
                null,
                NULL_URI_DIFF,
                diff,
                NULL_FRESCO_CONTEXT_DIFF,
                NULL_ASPECT_RATIO_DIFF);

    assertThat(shouldUpdate).isTrue();

    diff = new Diff<>(ImageOptions.create().build(), options2);

    shouldUpdate =
        (boolean)
            mShouldUpdateMethod.invoke(
                null,
                NULL_URI_DIFF,
                diff,
                NULL_FRESCO_CONTEXT_DIFF,
                NULL_ASPECT_RATIO_DIFF);

    assertThat(shouldUpdate).isTrue();
  }

  @Test
  public void test_shouldUpdate_whenFrescoContextChanged_thenUpdate()
      throws InvocationTargetException, IllegalAccessException {
    FrescoContext context1 = mock(FrescoContext.class);
    FrescoContext context2 = mock(FrescoContext.class);

    Diff<FrescoContext> diff = new Diff<>(null, context1);

    boolean shouldUpdate =
        (boolean)
            mShouldUpdateMethod.invoke(
                null,
                NULL_URI_DIFF,
                NULL_IMAGE_OPTIONS_DIFF,
                diff,
                NULL_ASPECT_RATIO_DIFF);

    assertThat(shouldUpdate).isTrue();

    diff = new Diff<>(context1, context2);

    shouldUpdate =
        (boolean)
            mShouldUpdateMethod.invoke(
                null,
                NULL_URI_DIFF,
                NULL_IMAGE_OPTIONS_DIFF,
                diff,
                NULL_ASPECT_RATIO_DIFF);

    assertThat(shouldUpdate).isTrue();
  }

  @Test
  public void test_shouldUpdate_whenAspectRatioChanged_thenUpdate()
      throws InvocationTargetException, IllegalAccessException {
    Float ratio1 = 1f;
    Float ratio2 = 0.5f;

    Diff<Float> diff = new Diff<>(null, ratio1);

    boolean shouldUpdate =
        (boolean)
            mShouldUpdateMethod.invoke(
                null,
                NULL_URI_DIFF,
                NULL_IMAGE_OPTIONS_DIFF,
                NULL_FRESCO_CONTEXT_DIFF,
                diff);

    assertThat(shouldUpdate).isTrue();

    diff = new Diff<>(ratio1, ratio2);

    shouldUpdate =
        (boolean)
            mShouldUpdateMethod.invoke(
                null,
                NULL_URI_DIFF,
                NULL_IMAGE_OPTIONS_DIFF,
                NULL_FRESCO_CONTEXT_DIFF,
                diff);

    assertThat(shouldUpdate).isTrue();
  }
}
