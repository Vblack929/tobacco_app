package com.tobacco.weight.hardware.idcard;

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
public final class IdCardManager_Factory implements Factory<IdCardManager> {
  private final Provider<HardwareSimulator> simulatorProvider;

  public IdCardManager_Factory(Provider<HardwareSimulator> simulatorProvider) {
    this.simulatorProvider = simulatorProvider;
  }

  @Override
  public IdCardManager get() {
    return newInstance(simulatorProvider.get());
  }

  public static IdCardManager_Factory create(Provider<HardwareSimulator> simulatorProvider) {
    return new IdCardManager_Factory(simulatorProvider);
  }

  public static IdCardManager newInstance(HardwareSimulator simulator) {
    return new IdCardManager(simulator);
  }
}
