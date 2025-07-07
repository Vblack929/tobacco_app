package com.tobacco.weight.di;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class HardwareModule_ProvideHardwareSimulatorFactory implements Factory<HardwareSimulator> {
  private final HardwareModule module;

  public HardwareModule_ProvideHardwareSimulatorFactory(HardwareModule module) {
    this.module = module;
  }

  @Override
  public HardwareSimulator get() {
    return provideHardwareSimulator(module);
  }

  public static HardwareModule_ProvideHardwareSimulatorFactory create(HardwareModule module) {
    return new HardwareModule_ProvideHardwareSimulatorFactory(module);
  }

  public static HardwareSimulator provideHardwareSimulator(HardwareModule instance) {
    return Preconditions.checkNotNullFromProvides(instance.provideHardwareSimulator());
  }
}
