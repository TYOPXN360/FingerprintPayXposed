package com.surcumference.fingerprint.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.toast.Toaster;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.adapter.PreferenceAdapter;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.DateUtils;
import com.surcumference.fingerprint.util.DpUtils;
import com.surcumference.fingerprint.util.FileUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.log.L;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Created by Jason on 2023/11/11.
 */

public class AdvanceSettingsView extends DialogFrameLayout implements AdapterView.OnItemClickListener {

    private List<PreferenceAdapter.Data> mSettingsDataList = new ArrayList<>();
    private PreferenceAdapter mListAdapter;
    private ListView mListView;


    public AdvanceSettingsView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public AdvanceSettingsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AdvanceSettingsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LinearLayout rootVerticalLayout = new LinearLayout(context);
        rootVerticalLayout.setOrientation(LinearLayout.VERTICAL);

        View lineView = new View(context);
        lineView.setBackgroundColor(Color.TRANSPARENT);

        int defHPadding = DpUtils.dip2px(context, 0);
        int defVPadding = DpUtils.dip2px(context, 12);

        mListView = new ListView(context);
        mListView.setDividerHeight(0);
        mListView.setOnItemClickListener(this);
        mListView.setPadding(defHPadding, defVPadding, defHPadding, defVPadding);
        mListView.setDivider(new ColorDrawable(Color.TRANSPARENT));
        Config config = Config.from(context);
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_no_fingerprint_icon), Lang.getString(R.id.settings_sub_title_no_fingerprint_icon), true, config.isShowFingerprintIcon()));
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_use_biometric_api), Lang.getString(R.id.settings_sub_title_use_biometric_api), true, config.isUseBiometricApi()));
        mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_volume_down_fingerprint_temporary_disable), Lang.getString(R.id.settings_sub_title_volume_down_fingerprint_temporary_disable), true,
                config.isVolumeDownMonitorEnabled() && !config.isUseBiometricApi()));
        mListAdapter = new PreferenceAdapter(mSettingsDataList);
        rootVerticalLayout.addView(lineView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(context, 2)));
        rootVerticalLayout.addView(mListView);
        this.addView(rootVerticalLayout);
    }


    @Override
    public String getDialogTitle() {
        return Lang.getString(R.id.app_settings_name) + " " + BuildConfig.VERSION_NAME;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mListView.setAdapter(mListAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        PreferenceAdapter.Data data = mListAdapter.getItem(position);
        final Context context = getContext();
        final Config config = Config.from(context);
        if (Lang.getString(R.id.settings_title_no_fingerprint_icon).equals(data.title)) {
            data.selectionState = !data.selectionState;
            config.setShowFingerprintIcon(data.selectionState);
            mListAdapter.notifyDataSetChanged();
        } else if (Lang.getString(R.id.settings_title_use_biometric_api).equals(data.title)) {
            data.selectionState = !data.selectionState;
            config.setUseBiometricApi(data.selectionState);
            mListAdapter.notifyDataSetChanged();
        } else if (Lang.getString(R.id.settings_title_volume_down_fingerprint_temporary_disable).equals(data.title)) {
            data.selectionState = !data.selectionState;
            config.setVolumeDownMonitorEnabled(data.selectionState);
            mListAdapter.notifyDataSetChanged();
        }
    }

    private PreferenceAdapter.Data findDataItem(String title) {
        for (PreferenceAdapter.Data data : mSettingsDataList) {
            if (title.equals(data.title)) {
                return data;
            }
        }
        return null;
    }
}
