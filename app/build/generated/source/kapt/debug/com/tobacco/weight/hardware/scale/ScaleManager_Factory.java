package com.tobacco.weight.hardware.scale;

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
public final class ScaleManager_Factory implements Factory<ScaleManager> {
  private final Provider<HardwareSimulator> simulatorProvider;

  public ScaleManager_Factory(Provider<HardwareSimulator> simulatorProvider) {
    this.simulatorProvider = simulatorProvider;
  }

  @Override
  public ScaleManager get() {
    return newInstance(simulatorProvider.get());
  }

  public static ScaleManager_Factory create(Provider<HardwareSimulator> simulatorProvider) {
    return new ScaleManager_Factory(simulatorProvider);
  }

  public static ScaleManager newInstance(HardwareSimulator simulator) {
    return new ScaleManager(simulator);
  }
}
