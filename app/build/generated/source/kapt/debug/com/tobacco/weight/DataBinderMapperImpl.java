package com.tobacco.weight;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import androidx.databinding.DataBinderMapper;
import androidx.databinding.DataBindingComponent;
import androidx.databinding.ViewDataBinding;
import com.tobacco.weight.databinding.ActivityAdminBindingImpl;
import com.tobacco.weight.databinding.FragmentPrecheckBindingImpl;
import com.tobacco.weight.databinding.FragmentRecordsBindingImpl;
import com.tobacco.weight.databinding.FragmentWeighingBindingImpl;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataBinderMapperImpl extends DataBinderMapper {
  private static final int LAYOUT_ACTIVITYADMIN = 1;

  private static final int LAYOUT_FRAGMENTPRECHECK = 2;

  private static final int LAYOUT_FRAGMENTRECORDS = 3;

  private static final int LAYOUT_FRAGMENTWEIGHING = 4;

  private static final SparseIntArray INTERNAL_LAYOUT_ID_LOOKUP = new SparseIntArray(4);

  static {
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.tobacco.weight.R.layout.activity_admin, LAYOUT_ACTIVITYADMIN);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.tobacco.weight.R.layout.fragment_precheck, LAYOUT_FRAGMENTPRECHECK);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.tobacco.weight.R.layout.fragment_records, LAYOUT_FRAGMENTRECORDS);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.tobacco.weight.R.layout.fragment_weighing, LAYOUT_FRAGMENTWEIGHING);
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View view, int layoutId) {
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = view.getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
        case  LAYOUT_ACTIVITYADMIN: {
          if ("layout/activity_admin_0".equals(tag)) {
            return new ActivityAdminBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_admin is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTPRECHECK: {
          if ("layout/fragment_precheck_0".equals(tag)) {
            return new FragmentPrecheckBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_precheck is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTRECORDS: {
          if ("layout/fragment_records_0".equals(tag)) {
            return new FragmentRecordsBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_records is invalid. Received: " + tag);
        }
        case  LAYOUT_FRAGMENTWEIGHING: {
          if ("layout/fragment_weighing_0".equals(tag)) {
            return new FragmentWeighingBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for fragment_weighing is invalid. Received: " + tag);
        }
      }
    }
    return null;
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View[] views, int layoutId) {
    if(views == null || views.length == 0) {
      return null;
    }
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = views[0].getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
      }
    }
    return null;
  }

  @Override
  public int getLayoutId(String tag) {
    if (tag == null) {
      return 0;
    }
    Integer tmpVal = InnerLayoutIdLookup.sKeys.get(tag);
    return tmpVal == null ? 0 : tmpVal;
  }

  @Override
  public String convertBrIdToString(int localId) {
    String tmpVal = InnerBrLookup.sKeys.get(localId);
    return tmpVal;
  }

  @Override
  public List<DataBinderMapper> collectDependencies() {
    ArrayList<DataBinderMapper> result = new ArrayList<DataBinderMapper>(1);
    result.add(new androidx.databinding.library.baseAdapters.DataBinderMapperImpl());
    return result;
  }

  private static class InnerBrLookup {
    static final SparseArray<String> sKeys = new SparseArray<String>(2);

    static {
      sKeys.put(0, "_all");
      sKeys.put(1, "viewModel");
    }
  }

  private static class InnerLayoutIdLookup {
    static final HashMap<String, Integer> sKeys = new HashMap<String, Integer>(4);

    static {
      sKeys.put("layout/activity_admin_0", com.tobacco.weight.R.layout.activity_admin);
      sKeys.put("layout/fragment_precheck_0", com.tobacco.weight.R.layout.fragment_precheck);
      sKeys.put("layout/fragment_records_0", com.tobacco.weight.R.layout.fragment_records);
      sKeys.put("layout/fragment_weighing_0", com.tobacco.weight.R.layout.fragment_weighing);
    }
  }
}
