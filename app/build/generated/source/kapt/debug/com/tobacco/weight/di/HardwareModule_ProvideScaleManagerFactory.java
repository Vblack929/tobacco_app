package com.tobacco.weight.di;

import com.tobacco.weight.hardware.scale.ScaleManager;
import com.tobacco.weight.hardware.simulator.HardwareSimulator;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class HardwareModule_ProvideScaleManagerFactory implements Factory<ScaleManager> {
  private final HardwareModule module;

  private final Provider<HardwareSimulator> simulatorProvider;

  public HardwareModule_ProvideScaleManagerFactory(HardwareModule module,
      Provider<HardwareSimulator> simulatorProvider) {
    this.module = module;
    this.simulatorProvider = simulatorProvider;
  }

  @Override
  public ScaleManager get() {
    return provideScaleManager(module, simulatorProvider.get());
  }

  public static HardwareModule_ProvideScaleManagerFactory create(HardwareModule module,
      Provider<HardwareSimulator> simulatorProvider) {
    return new HardwareModule_ProvideScaleManagerFactory(module, simulatorProvider);
  }

  public static ScaleManager provideScaleManager(HardwareModule instance,
      HardwareSimulator simulator) {
    return Preconditions.checkNotNullFromProvides(instance.provideScaleManager(simulator));
  }
}
