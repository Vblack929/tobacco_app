package com.tobacco.weight.ui.weighing;

import com.tobacco.weight.hardware.scale.ScaleManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class WeighingViewModel_Factory implements Factory<WeighingViewModel> {
  private final Provider<ScaleManager> scaleManagerProvider;

  public WeighingViewModel_Factory(Provider<ScaleManager> scaleManagerProvider) {
    this.scaleManagerProvider = scaleManagerProvider;
  }

  @Override
  public WeighingViewModel get() {
    return newInstance(scaleManagerProvider.get());
  }

  public static WeighingViewModel_Factory create(Provider<ScaleManager> scaleManagerProvider) {
    return new WeighingViewModel_Factory(scaleManagerProvider);
  }

  public static WeighingViewModel newInstance(ScaleManager scaleManager) {
    return new WeighingViewModel(scaleManager);
  }
}
