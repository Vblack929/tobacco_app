package com.tobacco.weight.ui.main;

import com.tobacco.weight.hardware.idcard.IdCardManager;
import com.tobacco.weight.hardware.printer.PrinterManager;
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<ScaleManager> scaleManagerProvider;

  private final Provider<PrinterManager> printerManagerProvider;

  private final Provider<IdCardManager> idCardManagerProvider;

  public MainViewModel_Factory(Provider<ScaleManager> scaleManagerProvider,
      Provider<PrinterManager> printerManagerProvider,
      Provider<IdCardManager> idCardManagerProvider) {
    this.scaleManagerProvider = scaleManagerProvider;
    this.printerManagerProvider = printerManagerProvider;
    this.idCardManagerProvider = idCardManagerProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(scaleManagerProvider.get(), printerManagerProvider.get(), idCardManagerProvider.get());
  }

  public static MainViewModel_Factory create(Provider<ScaleManager> scaleManagerProvider,
      Provider<PrinterManager> printerManagerProvider,
      Provider<IdCardManager> idCardManagerProvider) {
    return new MainViewModel_Factory(scaleManagerProvider, printerManagerProvider, idCardManagerProvider);
  }

  public static MainViewModel newInstance(ScaleManager scaleManager, PrinterManager printerManager,
      IdCardManager idCardManager) {
    return new MainViewModel(scaleManager, printerManager, idCardManager);
  }
}
