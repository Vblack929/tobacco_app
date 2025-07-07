package com.tobacco.weight.ui.precheck;

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
public final class PrecheckViewModel_Factory implements Factory<PrecheckViewModel> {
  @Override
  public PrecheckViewModel get() {
    return newInstance();
  }

  public static PrecheckViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PrecheckViewModel newInstance() {
    return new PrecheckViewModel();
  }

  private static final class InstanceHolder {
    private static final PrecheckViewModel_Factory INSTANCE = new PrecheckViewModel_Factory();
  }
}
