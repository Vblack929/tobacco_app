package com.tobacco.weight.ui.admin;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AdminViewModel_Factory implements Factory<AdminViewModel> {
  @Override
  public AdminViewModel get() {
    return newInstance();
  }

  public static AdminViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AdminViewModel newInstance() {
    return new AdminViewModel();
  }

  private static final class InstanceHolder {
    private static final AdminViewModel_Factory INSTANCE = new AdminViewModel_Factory();
  }
}
