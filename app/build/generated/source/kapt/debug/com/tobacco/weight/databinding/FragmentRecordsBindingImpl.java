package com.tobacco.weight.databinding;
import com.tobacco.weight.R;
import com.tobacco.weight.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class FragmentRecordsBindingImpl extends FragmentRecordsBinding implements com.tobacco.weight.generated.callback.OnClickListener.Listener {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.header_layout, 13);
        sViewsWithIds.put(R.id.btn_back, 14);
        sViewsWithIds.put(R.id.filter_card, 15);
        sViewsWithIds.put(R.id.spinner_tobacco_part, 16);
        sViewsWithIds.put(R.id.spinner_sort, 17);
        sViewsWithIds.put(R.id.stats_summary_layout, 18);
        sViewsWithIds.put(R.id.rv_records_list, 19);
    }
    // views
    @NonNull
    private final androidx.constraintlayout.widget.ConstraintLayout mboundView0;
    // variables
    @Nullable
    private final android.view.View.OnClickListener mCallback2;
    @Nullable
    private final android.view.View.OnClickListener mCallback5;
    @Nullable
    private final android.view.View.OnClickListener mCallback1;
    @Nullable
    private final android.view.View.OnClickListener mCallback4;
    @Nullable
    private final android.view.View.OnClickListener mCallback3;
    // values
    // listeners
    // Inverse Binding Event Handlers
    private androidx.databinding.InverseBindingListener etSearchRecordsandroidTextAttrChanged = new androidx.databinding.InverseBindingListener() {
        @Override
        public void onChange() {
            // Inverse of viewModel.searchQuery
            //         is viewModel.setSearchQuery((java.lang.String) callbackArg_0)
            java.lang.String callbackArg_0 = androidx.databinding.adapters.TextViewBindingAdapter.getTextString(etSearchRecords);
            // localize variables for thread safety
            // viewModel.searchQuery
            java.lang.String viewModelSearchQuery = null;
            // viewModel
            com.tobacco.weight.ui.records.RecordsViewModel viewModel = mViewModel;
            // viewModel != null
            boolean viewModelJavaLangObjectNull = false;



            viewModelJavaLangObjectNull = (viewModel) != (null);
            if (viewModelJavaLangObjectNull) {




                viewModel.setSearchQuery(((java.lang.String) (callbackArg_0)));
            }
        }
    };

    public FragmentRecordsBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 20, sIncludes, sViewsWithIds));
    }
    private FragmentRecordsBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.Button) bindings[14]
            , (android.widget.Button) bindings[4]
            , (android.widget.Button) bindings[1]
            , (android.widget.Button) bindings[5]
            , (android.widget.Button) bindings[3]
            , (android.widget.LinearLayout) bindings[10]
            , (android.widget.EditText) bindings[2]
            , (com.google.android.material.floatingactionbutton.FloatingActionButton) bindings[12]
            , (androidx.cardview.widget.CardView) bindings[15]
            , (android.widget.LinearLayout) bindings[13]
            , (android.widget.ProgressBar) bindings[11]
            , (androidx.recyclerview.widget.RecyclerView) bindings[19]
            , (android.widget.Spinner) bindings[17]
            , (android.widget.Spinner) bindings[16]
            , (android.widget.LinearLayout) bindings[18]
            , (android.widget.TextView) bindings[9]
            , (android.widget.TextView) bindings[8]
            , (android.widget.TextView) bindings[6]
            , (android.widget.TextView) bindings[7]
            );
        this.btnEndDate.setTag(null);
        this.btnExport.setTag(null);
        this.btnFilter.setTag(null);
        this.btnStartDate.setTag(null);
        this.emptyRecordsLayout.setTag(null);
        this.etSearchRecords.setTag(null);
        this.fabAddRecord.setTag(null);
        this.mboundView0 = (androidx.constraintlayout.widget.ConstraintLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.progressLoadingRecords.setTag(null);
        this.tvAvgPrice.setTag(null);
        this.tvTotalAmount.setTag(null);
        this.tvTotalRecords.setTag(null);
        this.tvTotalWeight.setTag(null);
        setRootTag(root);
        // listeners
        mCallback2 = new com.tobacco.weight.generated.callback.OnClickListener(this, 2);
        mCallback5 = new com.tobacco.weight.generated.callback.OnClickListener(this, 5);
        mCallback1 = new com.tobacco.weight.generated.callback.OnClickListener(this, 1);
        mCallback4 = new com.tobacco.weight.generated.callback.OnClickListener(this, 4);
        mCallback3 = new com.tobacco.weight.generated.callback.OnClickListener(this, 3);
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x2L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.viewModel == variableId) {
            setViewModel((com.tobacco.weight.ui.records.RecordsViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setViewModel(@Nullable com.tobacco.weight.ui.records.RecordsViewModel ViewModel) {
        this.mViewModel = ViewModel;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.viewModel);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        java.lang.String viewModelEndDate = null;
        int viewModelTotalRecords = 0;
        java.lang.String viewModelStartDate = null;
        int viewModelIsLoadingViewVISIBLEViewGONE = 0;
        boolean viewModelIsLoading = false;
        java.lang.String stringFormatJavaLangString1fViewModelTotalWeight = null;
        java.lang.String stringValueOfViewModelTotalRecords = null;
        int viewModelIsEmptyViewVISIBLEViewGONE = 0;
        java.lang.String javaLangStringStringFormatJavaLangString2fViewModelTotalAmount = null;
        java.lang.String stringFormatJavaLangString1fViewModelTotalWeightJavaLangStringKg = null;
        boolean viewModelIsEmpty = false;
        double viewModelTotalWeight = 0.0;
        double viewModelAvgPrice = 0.0;
        double viewModelTotalAmount = 0.0;
        java.lang.String stringFormatJavaLangString2fViewModelAvgPrice = null;
        java.lang.String viewModelSearchQuery = null;
        java.lang.String javaLangStringStringFormatJavaLangString2fViewModelAvgPrice = null;
        com.tobacco.weight.ui.records.RecordsViewModel viewModel = mViewModel;
        java.lang.String stringFormatJavaLangString2fViewModelTotalAmount = null;

        if ((dirtyFlags & 0x3L) != 0) {



                if (viewModel != null) {
                    // read viewModel.endDate
                    viewModelEndDate = viewModel.getEndDate();
                    // read viewModel.totalRecords
                    viewModelTotalRecords = viewModel.getTotalRecords();
                    // read viewModel.startDate
                    viewModelStartDate = viewModel.getStartDate();
                    // read viewModel.isLoading
                    viewModelIsLoading = viewModel.isLoading();
                    // read viewModel.isEmpty
                    viewModelIsEmpty = viewModel.isEmpty();
                    // read viewModel.totalWeight
                    viewModelTotalWeight = viewModel.getTotalWeight();
                    // read viewModel.avgPrice
                    viewModelAvgPrice = viewModel.getAvgPrice();
                    // read viewModel.totalAmount
                    viewModelTotalAmount = viewModel.getTotalAmount();
                    // read viewModel.searchQuery
                    viewModelSearchQuery = viewModel.getSearchQuery();
                }
            if((dirtyFlags & 0x3L) != 0) {
                if(viewModelIsLoading) {
                        dirtyFlags |= 0x8L;
                }
                else {
                        dirtyFlags |= 0x4L;
                }
            }
            if((dirtyFlags & 0x3L) != 0) {
                if(viewModelIsEmpty) {
                        dirtyFlags |= 0x20L;
                }
                else {
                        dirtyFlags |= 0x10L;
                }
            }


                // read String.valueOf(viewModel.totalRecords)
                stringValueOfViewModelTotalRecords = java.lang.String.valueOf(viewModelTotalRecords);
                // read viewModel.isLoading ? View.VISIBLE : View.GONE
                viewModelIsLoadingViewVISIBLEViewGONE = ((viewModelIsLoading) ? (android.view.View.VISIBLE) : (android.view.View.GONE));
                // read viewModel.isEmpty ? View.VISIBLE : View.GONE
                viewModelIsEmptyViewVISIBLEViewGONE = ((viewModelIsEmpty) ? (android.view.View.VISIBLE) : (android.view.View.GONE));
                // read String.format("%.1f", viewModel.totalWeight)
                stringFormatJavaLangString1fViewModelTotalWeight = java.lang.String.format("%.1f", viewModelTotalWeight);
                // read String.format("%.2f", viewModel.avgPrice)
                stringFormatJavaLangString2fViewModelAvgPrice = java.lang.String.format("%.2f", viewModelAvgPrice);
                // read String.format("%.2f", viewModel.totalAmount)
                stringFormatJavaLangString2fViewModelTotalAmount = java.lang.String.format("%.2f", viewModelTotalAmount);


                // read (String.format("%.1f", viewModel.totalWeight)) + ("kg")
                stringFormatJavaLangString1fViewModelTotalWeightJavaLangStringKg = (stringFormatJavaLangString1fViewModelTotalWeight) + ("kg");
                // read ("¥") + (String.format("%.2f", viewModel.avgPrice))
                javaLangStringStringFormatJavaLangString2fViewModelAvgPrice = ("¥") + (stringFormatJavaLangString2fViewModelAvgPrice);
                // read ("¥") + (String.format("%.2f", viewModel.totalAmount))
                javaLangStringStringFormatJavaLangString2fViewModelTotalAmount = ("¥") + (stringFormatJavaLangString2fViewModelTotalAmount);
        }
        // batch finished
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.btnEndDate, viewModelEndDate);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.btnStartDate, viewModelStartDate);
            this.emptyRecordsLayout.setVisibility(viewModelIsEmptyViewVISIBLEViewGONE);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.etSearchRecords, viewModelSearchQuery);
            this.progressLoadingRecords.setVisibility(viewModelIsLoadingViewVISIBLEViewGONE);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvAvgPrice, javaLangStringStringFormatJavaLangString2fViewModelAvgPrice);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvTotalAmount, javaLangStringStringFormatJavaLangString2fViewModelTotalAmount);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvTotalRecords, stringValueOfViewModelTotalRecords);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvTotalWeight, stringFormatJavaLangString1fViewModelTotalWeightJavaLangStringKg);
        }
        if ((dirtyFlags & 0x2L) != 0) {
            // api target 1

            this.btnEndDate.setOnClickListener(mCallback3);
            this.btnExport.setOnClickListener(mCallback1);
            this.btnFilter.setOnClickListener(mCallback4);
            this.btnStartDate.setOnClickListener(mCallback2);
            androidx.databinding.adapters.TextViewBindingAdapter.setTextWatcher(this.etSearchRecords, (androidx.databinding.adapters.TextViewBindingAdapter.BeforeTextChanged)null, (androidx.databinding.adapters.TextViewBindingAdapter.OnTextChanged)null, (androidx.databinding.adapters.TextViewBindingAdapter.AfterTextChanged)null, etSearchRecordsandroidTextAttrChanged);
            this.fabAddRecord.setOnClickListener(mCallback5);
        }
    }
    // Listener Stub Implementations
    // callback impls
    public final void _internalCallbackOnClick(int sourceId , android.view.View callbackArg_0) {
        switch(sourceId) {
            case 2: {
                // localize variables for thread safety
                // viewModel
                com.tobacco.weight.ui.records.RecordsViewModel viewModel = mViewModel;
                // viewModel != null
                boolean viewModelJavaLangObjectNull = false;



                viewModelJavaLangObjectNull = (viewModel) != (null);
                if (viewModelJavaLangObjectNull) {


                    viewModel.selectStartDate();
                }
                break;
            }
            case 5: {
                // localize variables for thread safety
                // viewModel
                com.tobacco.weight.ui.records.RecordsViewModel viewModel = mViewModel;
                // viewModel != null
                boolean viewModelJavaLangObjectNull = false;



                viewModelJavaLangObjectNull = (viewModel) != (null);
                if (viewModelJavaLangObjectNull) {


                    viewModel.addNewRecord();
                }
                break;
            }
            case 1: {
                // localize variables for thread safety
                // viewModel
                com.tobacco.weight.ui.records.RecordsViewModel viewModel = mViewModel;
                // viewModel != null
                boolean viewModelJavaLangObjectNull = false;



                viewModelJavaLangObjectNull = (viewModel) != (null);
                if (viewModelJavaLangObjectNull) {


                    viewModel.exportRecords();
                }
                break;
            }
            case 4: {
                // localize variables for thread safety
                // viewModel
                com.tobacco.weight.ui.records.RecordsViewModel viewModel = mViewModel;
                // viewModel != null
                boolean viewModelJavaLangObjectNull = false;



                viewModelJavaLangObjectNull = (viewModel) != (null);
                if (viewModelJavaLangObjectNull) {


                    viewModel.applyFilter();
                }
                break;
            }
            case 3: {
                // localize variables for thread safety
                // viewModel
                com.tobacco.weight.ui.records.RecordsViewModel viewModel = mViewModel;
                // viewModel != null
                boolean viewModelJavaLangObjectNull = false;



                viewModelJavaLangObjectNull = (viewModel) != (null);
                if (viewModelJavaLangObjectNull) {


                    viewModel.selectEndDate();
                }
                break;
            }
        }
    }
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): viewModel
        flag 1 (0x2L): null
        flag 2 (0x3L): viewModel.isLoading ? View.VISIBLE : View.GONE
        flag 3 (0x4L): viewModel.isLoading ? View.VISIBLE : View.GONE
        flag 4 (0x5L): viewModel.isEmpty ? View.VISIBLE : View.GONE
        flag 5 (0x6L): viewModel.isEmpty ? View.VISIBLE : View.GONE
    flag mapping end*/
    //end
}