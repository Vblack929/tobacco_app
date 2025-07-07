package com.tobacco.weight.hardware.printer;

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
public final class PrinterManager_Factory implements Factory<PrinterManager> {
  private final Provider<HardwareSimulator> simulatorProvider;

  public PrinterManager_Factory(Provider<HardwareSimulator> simulatorProvider) {
    this.simulatorProvider = simulatorProvider;
  }

  @Override
  public PrinterManager get() {
    return newInstance(simulatorProvider.get());
  }

  public static PrinterManager_Factory create(Provider<HardwareSimulator> simulatorProvider) {
    return new PrinterManager_Factory(simulatorProvider);
  }

  public static PrinterManager newInstance(HardwareSimulator simulator) {
    return new PrinterManager(simulator);
  }
}
