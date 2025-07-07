package com.tobacco.weight.databinding;
import com.tobacco.weight.R;
import com.tobacco.weight.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class FragmentPrecheckBindingImpl extends FragmentPrecheckBinding implements com.tobacco.weight.generated.callback.OnClickListener.Listener {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.header_layout, 11);
        sViewsWithIds.put(R.id.btn_back, 12);
        sViewsWithIds.put(R.id.search_card, 13);
        sViewsWithIds.put(R.id.cb_search_batch, 14);
        sViewsWithIds.put(R.id.cb_search_transfer, 15);
        sViewsWithIds.put(R.id.cb_search_code, 16);
        sViewsWithIds.put(R.id.stats_layout, 17);
        sViewsWithIds.put(R.id.rv_precheck_list, 18);
        sViewsWithIds.put(R.id.bottom_action_layout, 19);
    }
    // views
    @NonNull
    private final androidx.constraintlayout.widget.ConstraintLayout mboundView0;
    // variables
    @Nullable
    private final android.view.View.OnClickListener mCallback8;
    @Nullable
    private final android.view.View.OnClickListener mCallback6;
    @Nullable
    private final android.view.View.OnClickListener mCallback9;
    @Nullable
    private final android.view.View.OnClickListener mCallback7;
    // values
    // listeners
    // Inverse Binding Event Handlers
    private androidx.databinding.InverseBindingListener etSearchandroidTextAttrChanged = new androidx.databinding.InverseBindingListener() {
        @Override
        public void onChange() {
            // Inverse of viewModel.searchQuery
            //         is viewModel.setSearchQuery((java.lang.String) callbackArg_0)
            java.lang.String callbackArg_0 = androidx.databinding.adapters.TextViewBindingAdapter.getTextString(etSearch);
            // localize variables for thread safety
            // viewModel.searchQuery
            java.lang.String viewModelSearchQuery = null;
            // viewModel
            com.tobacco.weight.ui.precheck.PrecheckViewModel viewModel = mViewModel;
            // viewModel != null
            boolean viewModelJavaLangObjectNull = false;



            viewModelJavaLangObjectNull = (viewModel) != (null);
            if (viewModelJavaLangObjectNull) {




                viewModel.setSearchQuery(((java.lang.String) (callbackArg_0)));
            }
        }
    };

    public FragmentPrecheckBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 20, sIncludes, sViewsWithIds));
    }
    private FragmentPrecheckBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.LinearLayout) bindings[19]
            , (android.widget.Button) bindings[12]
            , (android.widget.Button) bindings[3]
            , (android.widget.Button) bindings[9]
            , (android.widget.Button) bindings[8]
            , (android.widget.Button) bindings[2]
            , (android.widget.CheckBox) bindings[14]
            , (android.widget.CheckBox) bindings[16]
            , (android.widget.CheckBox) bindings[15]
            , (android.widget.LinearLayout) bindings[6]
            , (android.widget.EditText) bindings[1]
            , (android.widget.LinearLayout) bindings[11]
            , (android.widget.ProgressBar) bindings[10]
            , (androidx.recyclerview.widget.RecyclerView) bindings[18]
            , (androidx.cardview.widget.CardView) bindings[13]
            , (android.widget.LinearLayout) bindings[17]
            , (android.widget.TextView) bindings[5]
            , (android.widget.TextView) bindings[7]
            , (android.widget.TextView) bindings[4]
            );
        this.btnClearSearch.setTag(null);
        this.btnConfirmSelect.setTag(null);
        this.btnRefresh.setTag(null);
        this.btnSearch.setTag(null);
        this.emptyStateLayout.setTag(null);
        this.etSearch.setTag(null);
        this.mboundView0 = (androidx.constraintlayout.widget.ConstraintLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.progressLoading.setTag(null);
        this.tvSelectedInfo.setTag(null);
        this.tvSelectionInfo.setTag(null);
        this.tvTotalCount.setTag(null);
        setRootTag(root);
        // listeners
        mCallback8 = new com.tobacco.weight.generated.callback.OnClickListener(this, 3);
        mCallback6 = new com.tobacco.weight.generated.callback.OnClickListener(this, 1);
        mCallback9 = new com.tobacco.weight.generated.callback.OnClickListener(this, 4);
        mCallback7 = new com.tobacco.weight.generated.callback.OnClickListener(this, 2);
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
            setViewModel((com.tobacco.weight.ui.precheck.PrecheckViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setViewModel(@Nullable com.tobacco.weight.ui.precheck.PrecheckViewModel ViewModel) {
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
        boolean viewModelIsEmptyState = false;
        int viewModelIsEmptyStateViewVISIBLEViewGONE = 0;
        java.lang.String javaLangStringViewModelTotalCountJavaLangString = null;
        boolean viewModelHasSelection = false;
        java.lang.String viewModelSelectionInfo = null;
        java.lang.String viewModelSelectedInfo = null;
        java.lang.String javaLangStringViewModelTotalCount = null;
        int viewModelIsLoadingViewVISIBLEViewGONE = 0;
        java.lang.String viewModelSearchQuery = null;
        int viewModelTotalCount = 0;
        boolean viewModelIsLoading = false;
        com.tobacco.weight.ui.precheck.PrecheckViewModel viewModel = mViewModel;

        if ((dirtyFlags & 0x3L) != 0) {



                if (viewModel != null) {
                    // read viewModel.isEmptyState
                    viewModelIsEmptyState = viewModel.isEmptyState();
                    // read viewModel.hasSelection
                    viewModelHasSelection = viewModel.isHasSelection();
                    // read viewModel.selectionInfo
                    viewModelSelectionInfo = viewModel.getSelectionInfo();
                    // read viewModel.selectedInfo
                    viewModelSelectedInfo = viewModel.getSelectedInfo();
                    // read viewModel.searchQuery
                    viewModelSearchQuery = viewModel.getSearchQuery();
                    // read viewModel.totalCount
                    viewModelTotalCount = viewModel.getTotalCount();
                    // read viewModel.isLoading
                    viewModelIsLoading = viewModel.isLoading();
                }
            if((dirtyFlags & 0x3L) != 0) {
                if(viewModelIsEmptyState) {
                        dirtyFlags |= 0x8L;
                }
                else {
                        dirtyFlags |= 0x4L;
                }
            }
            if((dirtyFlags & 0x3L) != 0) {
                if(viewModelIsLoading) {
                        dirtyFlags |= 0x20L;
                }
                else {
                        dirtyFlags |= 0x10L;
                }
            }


                // read viewModel.isEmptyState ? View.VISIBLE : View.GONE
                viewModelIsEmptyStateViewVISIBLEViewGONE = ((viewModelIsEmptyState) ? (android.view.View.VISIBLE) : (android.view.View.GONE));
                // read ("共找到 ") + (viewModel.totalCount)
                javaLangStringViewModelTotalCount = ("共找到 ") + (viewModelTotalCount);
                // read viewModel.isLoading ? View.VISIBLE : View.GONE
                viewModelIsLoadingViewVISIBLEViewGONE = ((viewModelIsLoading) ? (android.view.View.VISIBLE) : (android.view.View.GONE));


                // read (("共找到 ") + (viewModel.totalCount)) + (" 条记录")
                javaLangStringViewModelTotalCountJavaLangString = (javaLangStringViewModelTotalCount) + (" 条记录");
        }
        // batch finished
        if ((dirtyFlags & 0x2L) != 0) {
            // api target 1

            this.btnClearSearch.setOnClickListener(mCallback7);
            this.btnConfirmSelect.setOnClickListener(mCallback9);
            this.btnRefresh.setOnClickListener(mCallback8);
            this.btnSearch.setOnClickListener(mCallback6);
            androidx.databinding.adapters.TextViewBindingAdapter.setTextWatcher(this.etSearch, (androidx.databinding.adapters.TextViewBindingAdapter.BeforeTextChanged)null, (androidx.databinding.adapters.TextViewBindingAdapter.OnTextChanged)null, (androidx.databinding.adapters.TextViewBindingAdapter.AfterTextChanged)null, etSearchandroidTextAttrChanged);
        }
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            this.btnConfirmSelect.setEnabled(viewModelHasSelection);
            this.emptyStateLayout.setVisibility(viewModelIsEmptyStateViewVISIBLEViewGONE);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.etSearch, viewModelSearchQuery);
            this.progressLoading.setVisibility(viewModelIsLoadingViewVISIBLEViewGONE);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvSelectedInfo, viewModelSelectedInfo);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvSelectionInfo, viewModelSelectionInfo);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvTotalCount, javaLangStringViewModelTotalCountJavaLangString);
        }
    }
    // Listener Stub Implementations
    // callback impls
    public final void _internalCallbackOnClick(int sourceId , android.view.View callbackArg_0) {
        switch(sourceId) {
            case 3: {
                // localize variables for thread safety
                // viewModel
                com.tobacco.weight.ui.precheck.PrecheckViewModel viewModel = mViewModel;
                // viewModel != null
                boolean viewModelJavaLangObjectNull = false;



                viewModelJavaLangObjectNull = (viewModel) != (null);
                if (viewModelJavaLangObjectNull) {


                    viewModel.refreshData();
                }
                break;
            }
            case 1: {
                // localize variables for thread safety
                // viewModel
                com.tobacco.weight.ui.precheck.PrecheckViewModel viewModel = mViewModel;
                // viewModel != null
                boolean viewModelJavaLangObjectNull = false;



                viewModelJavaLangObjectNull = (viewModel) != (null);
                if (viewModelJavaLangObjectNull) {


                    viewModel.searchPrecheck();
                }
                break;
            }
            case 4: {
                // localize variables for thread safety
                // viewModel
                com.tobacco.weight.ui.precheck.PrecheckViewModel viewModel = mViewModel;
                // viewModel != null
                boolean viewModelJavaLangObjectNull = false;



                viewModelJavaLangObjectNull = (viewModel) != (null);
                if (viewModelJavaLangObjectNull) {


                    viewModel.confirmSelection();
                }
                break;
            }
            case 2: {
                // localize variables for thread safety
                // viewModel
                com.tobacco.weight.ui.precheck.PrecheckViewModel viewModel = mViewModel;
                // viewModel != null
                boolean viewModelJavaLangObjectNull = false;



                viewModelJavaLangObjectNull = (viewModel) != (null);
                if (viewModelJavaLangObjectNull) {


                    viewModel.clearSearch();
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
        flag 2 (0x3L): viewModel.isEmptyState ? View.VISIBLE : View.GONE
        flag 3 (0x4L): viewModel.isEmptyState ? View.VISIBLE : View.GONE
        flag 4 (0x5L): viewModel.isLoading ? View.VISIBLE : View.GONE
        flag 5 (0x6L): viewModel.isLoading ? View.VISIBLE : View.GONE
    flag mapping end*/
    //end
}