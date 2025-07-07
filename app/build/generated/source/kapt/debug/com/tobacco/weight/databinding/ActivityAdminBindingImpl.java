package com.tobacco.weight.databinding;
import com.tobacco.weight.R;
import com.tobacco.weight.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ActivityAdminBindingImpl extends ActivityAdminBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.tv_total_precheck_count, 1);
        sViewsWithIds.put(R.id.tv_current_precheck_count, 2);
        sViewsWithIds.put(R.id.tv_total_precheck_weight, 3);
        sViewsWithIds.put(R.id.tv_average_weight, 4);
        sViewsWithIds.put(R.id.tv_grade_a_precheck_weight, 5);
        sViewsWithIds.put(R.id.tv_grade_a_actual_weight, 6);
        sViewsWithIds.put(R.id.tv_grade_b_precheck_weight, 7);
        sViewsWithIds.put(R.id.tv_grade_b_actual_weight, 8);
        sViewsWithIds.put(R.id.tv_grade_c_precheck_weight, 9);
        sViewsWithIds.put(R.id.tv_grade_c_actual_weight, 10);
        sViewsWithIds.put(R.id.tv_grade_d_precheck_weight, 11);
        sViewsWithIds.put(R.id.tv_grade_d_actual_weight, 12);
        sViewsWithIds.put(R.id.btn_refresh_data, 13);
        sViewsWithIds.put(R.id.btn_export_data, 14);
    }
    // views
    @NonNull
    private final android.widget.ScrollView mboundView0;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityAdminBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 15, sIncludes, sViewsWithIds));
    }
    private ActivityAdminBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.Button) bindings[14]
            , (android.widget.Button) bindings[13]
            , (android.widget.TextView) bindings[4]
            , (android.widget.TextView) bindings[2]
            , (android.widget.TextView) bindings[6]
            , (android.widget.TextView) bindings[5]
            , (android.widget.TextView) bindings[8]
            , (android.widget.TextView) bindings[7]
            , (android.widget.TextView) bindings[10]
            , (android.widget.TextView) bindings[9]
            , (android.widget.TextView) bindings[12]
            , (android.widget.TextView) bindings[11]
            , (android.widget.TextView) bindings[1]
            , (android.widget.TextView) bindings[3]
            );
        this.mboundView0 = (android.widget.ScrollView) bindings[0];
        this.mboundView0.setTag(null);
        setRootTag(root);
        // listeners
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
            setViewModel((com.tobacco.weight.ui.admin.AdminViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setViewModel(@Nullable com.tobacco.weight.ui.admin.AdminViewModel ViewModel) {
        this.mViewModel = ViewModel;
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
        // batch finished
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): viewModel
        flag 1 (0x2L): null
    flag mapping end*/
    //end
}