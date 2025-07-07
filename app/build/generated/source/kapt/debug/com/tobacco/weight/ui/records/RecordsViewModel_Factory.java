package com.tobacco.weight.ui.records;

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
public final class RecordsViewModel_Factory implements Factory<RecordsViewModel> {
  @Override
  public RecordsViewModel get() {
    return newInstance();
  }

  public static RecordsViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RecordsViewModel newInstance() {
    return new RecordsViewModel();
  }

  private static final class InstanceHolder {
    private static final RecordsViewModel_Factory INSTANCE = new RecordsViewModel_Factory();
  }
}
