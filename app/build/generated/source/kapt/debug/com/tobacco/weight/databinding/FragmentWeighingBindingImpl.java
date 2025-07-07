package com.tobacco.weight.databinding;
import com.tobacco.weight.R;
import com.tobacco.weight.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class FragmentWeighingBindingImpl extends FragmentWeighingBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.tv_title, 3);
        sViewsWithIds.put(R.id.card_farmer_info, 4);
        sViewsWithIds.put(R.id.tv_farmer_name, 5);
        sViewsWithIds.put(R.id.tv_contract_number, 6);
        sViewsWithIds.put(R.id.tv_current_weight, 7);
        sViewsWithIds.put(R.id.btn_upper_level, 8);
        sViewsWithIds.put(R.id.btn_middle_level, 9);
        sViewsWithIds.put(R.id.btn_lower_level, 10);
        sViewsWithIds.put(R.id.btn_tare, 11);
        sViewsWithIds.put(R.id.btn_print, 12);
        sViewsWithIds.put(R.id.btn_simulate_light, 13);
        sViewsWithIds.put(R.id.btn_simulate_heavy, 14);
        sViewsWithIds.put(R.id.card_device_status, 15);
        sViewsWithIds.put(R.id.tv_precheck_level, 16);
        sViewsWithIds.put(R.id.btn_read_id_card, 17);
        sViewsWithIds.put(R.id.btn_reset_precheck, 18);
        sViewsWithIds.put(R.id.tv_device_status, 19);
        sViewsWithIds.put(R.id.card_tobacco_classification, 20);
        sViewsWithIds.put(R.id.et_price_a, 21);
        sViewsWithIds.put(R.id.et_price_b, 22);
        sViewsWithIds.put(R.id.et_price_c, 23);
        sViewsWithIds.put(R.id.et_price_d, 24);
        sViewsWithIds.put(R.id.et_actual_price_a, 25);
        sViewsWithIds.put(R.id.et_actual_price_b, 26);
        sViewsWithIds.put(R.id.et_actual_price_c, 27);
        sViewsWithIds.put(R.id.et_actual_price_d, 28);
        sViewsWithIds.put(R.id.btn_select_a, 29);
        sViewsWithIds.put(R.id.btn_select_b, 30);
        sViewsWithIds.put(R.id.btn_select_c, 31);
        sViewsWithIds.put(R.id.btn_select_d, 32);
        sViewsWithIds.put(R.id.card_weight_info, 33);
        sViewsWithIds.put(R.id.tv_current_time, 34);
        sViewsWithIds.put(R.id.et_contract_number, 35);
        sViewsWithIds.put(R.id.et_price, 36);
        sViewsWithIds.put(R.id.tv_settlement_info, 37);
        sViewsWithIds.put(R.id.card_test_controls, 38);
        sViewsWithIds.put(R.id.btn_test_weight_5kg, 39);
        sViewsWithIds.put(R.id.btn_test_weight_10kg, 40);
        sViewsWithIds.put(R.id.btn_test_weight_20kg, 41);
    }
    // views
    @NonNull
    private final androidx.constraintlayout.widget.ConstraintLayout mboundView0;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public FragmentWeighingBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 42, sIncludes, sViewsWithIds));
    }
    private FragmentWeighingBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 2
            , (android.widget.Button) bindings[10]
            , (android.widget.Button) bindings[9]
            , (android.widget.Button) bindings[12]
            , (android.widget.Button) bindings[17]
            , (android.widget.Button) bindings[18]
            , (android.widget.Button) bindings[29]
            , (android.widget.Button) bindings[30]
            , (android.widget.Button) bindings[31]
            , (android.widget.Button) bindings[32]
            , (android.widget.Button) bindings[14]
            , (android.widget.Button) bindings[13]
            , (android.widget.Button) bindings[11]
            , (android.widget.Button) bindings[40]
            , (android.widget.Button) bindings[41]
            , (android.widget.Button) bindings[39]
            , (android.widget.Button) bindings[8]
            , (androidx.cardview.widget.CardView) bindings[15]
            , (androidx.cardview.widget.CardView) bindings[4]
            , (androidx.cardview.widget.CardView) bindings[38]
            , (androidx.cardview.widget.CardView) bindings[20]
            , (androidx.cardview.widget.CardView) bindings[33]
            , (android.widget.EditText) bindings[25]
            , (android.widget.EditText) bindings[26]
            , (android.widget.EditText) bindings[27]
            , (android.widget.EditText) bindings[28]
            , (android.widget.EditText) bindings[35]
            , (android.widget.EditText) bindings[36]
            , (android.widget.EditText) bindings[21]
            , (android.widget.EditText) bindings[22]
            , (android.widget.EditText) bindings[23]
            , (android.widget.EditText) bindings[24]
            , (android.widget.TextView) bindings[6]
            , (android.widget.TextView) bindings[34]
            , (android.widget.TextView) bindings[7]
            , (android.widget.TextView) bindings[19]
            , (android.widget.TextView) bindings[5]
            , (android.widget.TextView) bindings[16]
            , (android.widget.TextView) bindings[1]
            , (android.widget.TextView) bindings[2]
            , (android.widget.TextView) bindings[37]
            , (android.widget.TextView) bindings[3]
            );
        this.mboundView0 = (androidx.constraintlayout.widget.ConstraintLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.tvPrecheckRatio.setTag(null);
        this.tvPrecheckWeight.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x8L;
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
            setViewModel((com.tobacco.weight.ui.weighing.WeighingViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setViewModel(@Nullable com.tobacco.weight.ui.weighing.WeighingViewModel ViewModel) {
        this.mViewModel = ViewModel;
        synchronized(this) {
            mDirtyFlags |= 0x4L;
        }
        notifyPropertyChanged(BR.viewModel);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
            case 0 :
                return onChangeViewModelPrecheckRatio((androidx.lifecycle.LiveData<java.lang.String>) object, fieldId);
            case 1 :
                return onChangeViewModelPrecheckWeight((androidx.lifecycle.LiveData<java.lang.String>) object, fieldId);
        }
        return false;
    }
    private boolean onChangeViewModelPrecheckRatio(androidx.lifecycle.LiveData<java.lang.String> ViewModelPrecheckRatio, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x1L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeViewModelPrecheckWeight(androidx.lifecycle.LiveData<java.lang.String> ViewModelPrecheckWeight, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x2L;
            }
            return true;
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
        java.lang.String viewModelPrecheckWeightGetValue = null;
        java.lang.String viewModelPrecheckRatioGetValue = null;
        androidx.lifecycle.LiveData<java.lang.String> viewModelPrecheckRatio = null;
        com.tobacco.weight.ui.weighing.WeighingViewModel viewModel = mViewModel;
        androidx.lifecycle.LiveData<java.lang.String> viewModelPrecheckWeight = null;

        if ((dirtyFlags & 0xfL) != 0) {


            if ((dirtyFlags & 0xdL) != 0) {

                    if (viewModel != null) {
                        // read viewModel.precheckRatio
                        viewModelPrecheckRatio = viewModel.getPrecheckRatio();
                    }
                    updateLiveDataRegistration(0, viewModelPrecheckRatio);


                    if (viewModelPrecheckRatio != null) {
                        // read viewModel.precheckRatio.getValue()
                        viewModelPrecheckRatioGetValue = viewModelPrecheckRatio.getValue();
                    }
            }
            if ((dirtyFlags & 0xeL) != 0) {

                    if (viewModel != null) {
                        // read viewModel.precheckWeight
                        viewModelPrecheckWeight = viewModel.getPrecheckWeight();
                    }
                    updateLiveDataRegistration(1, viewModelPrecheckWeight);


                    if (viewModelPrecheckWeight != null) {
                        // read viewModel.precheckWeight.getValue()
                        viewModelPrecheckWeightGetValue = viewModelPrecheckWeight.getValue();
                    }
            }
        }
        // batch finished
        if ((dirtyFlags & 0xdL) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvPrecheckRatio, viewModelPrecheckRatioGetValue);
        }
        if ((dirtyFlags & 0xeL) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvPrecheckWeight, viewModelPrecheckWeightGetValue);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): viewModel.precheckRatio
        flag 1 (0x2L): viewModel.precheckWeight
        flag 2 (0x3L): viewModel
        flag 3 (0x4L): null
    flag mapping end*/
    //end
}