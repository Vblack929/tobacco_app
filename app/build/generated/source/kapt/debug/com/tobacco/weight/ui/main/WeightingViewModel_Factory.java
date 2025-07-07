package com.tobacco.weight.ui.main;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;
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
public final class WeightingViewModel_Factory implements Factory<WeightingViewModel> {
  private final Provider<HardwareSimulator> hardwareSimulatorProvider;

  public WeightingViewModel_Factory(Provider<HardwareSimulator> hardwareSimulatorProvider) {
    this.hardwareSimulatorProvider = hardwareSimulatorProvider;
  }

  @Override
  public WeightingViewModel get() {
    return newInstance(hardwareSimulatorProvider.get());
  }

  public static WeightingViewModel_Factory create(
      Provider<HardwareSimulator> hardwareSimulatorProvider) {
    return new WeightingViewModel_Factory(hardwareSimulatorProvider);
  }

  public static WeightingViewModel newInstance(HardwareSimulator hardwareSimulator) {
    return new WeightingViewModel(hardwareSimulator);
  }
}
