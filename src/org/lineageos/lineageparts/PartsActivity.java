/*
 * Copyright (C) 2016 The CyanogenMod Project
 *               2017-2019,2021-2022 The PadrePioUI Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.lineageparts;

import android.content.ComponentName;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.MainSwitchBar;

import lineageos.preference.PartInfo;
import lineageos.preference.PartsList;

import org.lineageos.lineageparts.profiles.NFCProfileTagCallback;

public class PartsActivity extends CollapsingToolbarBaseActivity implements
        PreferenceFragment.OnPreferenceStartFragmentCallback,
        PreferenceFragment.OnPreferenceStartScreenCallback {

    private static final String TAG = "PartsActivity";

    // Settings compatibility
    public static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title";
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RESID =
            ":settings:show_fragment_title_resid";

    private NFCProfileTagCallback mNfcProfileCallback;

    private CharSequence mInitialTitle;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.lineageparts);

        String action = getIntent().getAction();
        ComponentName cn = getIntent().getComponent();

        PartInfo info = null;
        String partExtra = null;

        // Parts are launched by setting the action to PARTS_ACTION_PREFIX.part_key
        // and using an explcit intent to get here
        if (action != null && action.startsWith(PartsList.PARTS_ACTION_PREFIX) &&
                getClass().getName().equals(cn.getClassName())) {
            partExtra = action.substring(PartsList.PARTS_ACTION_PREFIX.length() + 1);
        }

        // Settings compatibility
        String fragmentClass = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        String component = getIntent().getComponent().getClassName();
        Bundle initialArgs = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        if (initialArgs == null) {
            initialArgs = new Bundle();
        }
        String argKey = getIntent().getStringExtra(EXTRA_FRAGMENT_ARG_KEY);
        initialArgs.putString(EXTRA_FRAGMENT_ARG_KEY, argKey);

        Log.d(TAG, "Launched with: " + getIntent().toString() + " action: " +
                getIntent().getAction() + " component: " + component +
                " part: " + partExtra + " fragment: " + fragmentClass);

        if (fragmentClass == null) {
            if (partExtra != null) {
                // Parts mode
                info = PartsList.get(this).getPartInfo(partExtra);
            } else {
                // Alias mode
                info = PartsList.get(this).getPartInfoForClass(
                        getIntent().getComponent().getClassName());
            }
            if (info == null) {
                throw new UnsupportedOperationException(
                        "Unable to get part info: " + getIntent().toString());
            }
            fragmentClass = info.getFragmentClass();
        }

        if (fragmentClass == null) {
            throw new UnsupportedOperationException(
                    "Unable to get fragment class: " + getIntent().toString());
        }

        setTitleFromIntent(getIntent(), info);

        switchToFragment(fragmentClass, initialArgs, -1, mInitialTitle);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        startPreferencePanel(pref.getFragment(), pref.getExtras(), -1, pref.getTitle(),
                null, 0);
        return true;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment caller, PreferenceScreen pref) {
        startPreferencePanel(pref.getFragment(), pref.getExtras(), -1, pref.getTitle(),
                null, 0);
        return true;
    }

    public void setNfcProfileCallback(NFCProfileTagCallback callback) {
        mNfcProfileCallback = callback;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (mNfcProfileCallback != null) {
                mNfcProfileCallback.onTagRead(detectedTag);
            }
            return;
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onBackPressed() {
        setTitle(mInitialTitle);

        if (!getFragmentManager().popBackStackImmediate()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes,
                                     CharSequence titleText, Fragment resultTo,
                                     int resultRequestCode) {
        String title = null;
        if (titleRes < 0) {
            if (titleText != null) {
                title = titleText.toString();
            } else {
                // There not much we can do in that case
                title = "";
            }
        }

        Intent intent = new Intent();
        intent.setComponent(PartsList.LINEAGEPARTS_ACTIVITY);
        intent.putExtra(EXTRA_SHOW_FRAGMENT, fragmentClass);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, titleRes);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, titleText);

        if (resultTo == null) {
            startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        setResult(resultCode, resultData);
        finish();
    }

    public boolean switchToFragment(String fragmentClass, Bundle args, int titleRes,
                                    CharSequence titleText) {
        Fragment fragment = Fragment.instantiate(this, fragmentClass);
        if (fragment == null) {
            Log.e(TAG, "Invalid fragment! " + fragmentClass);
            return false;
        }
        return switchToFragment(fragment, args, titleRes, titleText);
    }

    private  boolean switchToFragment(Fragment fragment, Bundle args, int titleRes,
                                    CharSequence titleText) {
        Log.d(TAG, "Launching fragment: " + fragment.getClass().getName());

        fragment.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, fragment);
        if (titleRes > 0) {
            transaction.setBreadCrumbTitle(titleRes);
        } else if (titleText != null) {
            transaction.setBreadCrumbTitle(titleText);
        }
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.commit();
        return true;
    }

    public TextView getTopIntro() {
        return (TextView) findViewById(R.id.top_intro);
    }

    public void showTopIntro(boolean show) {
        findViewById(R.id.top_intro).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public Button getBackButton() {
        return (Button) findViewById(R.id.back_button);
    }

    public Button getNextButton() {
        return (Button) findViewById(R.id.next_button);
    }

    public void showButtonBar(boolean show) {
        findViewById(R.id.button_bar).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public MainSwitchBar getMainSwitchBar() {
        return (MainSwitchBar) findViewById(R.id.main_switch_bar);
    }

    private void setTitleFromIntent(Intent intent, PartInfo part) {
        if (part != null) {
            mInitialTitle = part.getTitle();
        } else {
            final int initialTitleResId = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
            if (initialTitleResId > 0) {
                mInitialTitle = getResources().getString(initialTitleResId);
            } else {
                final String initialTitle = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
                mInitialTitle = (initialTitle != null) ? initialTitle : getTitle();
            }
        }
        setTitle(mInitialTitle);
    }
}
