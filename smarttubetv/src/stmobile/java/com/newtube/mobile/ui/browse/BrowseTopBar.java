package com.newtube.mobile.ui.browse;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * CometTube: Browse top bar.
 * On main tabs, it displays the crisp transparent Comet banner mark (photo_2026-10-25_00-09-38).
 * On sub-screens (e.g. You subsections like Settings or Playlists), it displays the back arrow
 * and the subscreen title text.
 */
final class BrowseTopBar {
    private static final float WORDMARK_SP = 22f;
    private static final float SUBSCREEN_TITLE_SP = 20f;

    private final TextView mTitle;
    private final View mBack;
    private final ImageView mBanner;
    private final int mWordmarkInset;
    private final int mBesideBackInset;

    BrowseTopBar(Activity activity, Runnable onBack) {
        mTitle = activity.findViewById(R.id.mobile_title_bar);
        mBack = activity.findViewById(R.id.mobile_title_back);
        mBanner = activity.findViewById(R.id.mobile_title_banner);
        mBack.setOnClickListener(v -> onBack.run());
        mWordmarkInset = mTitle.getPaddingStart();
        mBesideBackInset = activity.getResources()
                .getDimensionPixelSize(R.dimen.mobile_dialog_title_inset_with_back);
    }

    /**
     * A tab shows the Comet banner; a sub-screen shows the back arrow and title.
     */
    void show(boolean subScreen, CharSequence title) {
        mBack.setVisibility(subScreen ? View.VISIBLE : View.GONE);
        if (mBanner != null) {
            mBanner.setVisibility(subScreen ? View.GONE : View.VISIBLE);
        }
        if (subScreen) {
            mTitle.setVisibility(View.VISIBLE);
            mTitle.setText(title != null && title.length() > 0
                    ? title : mTitle.getContext().getString(R.string.app_name));
            mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, SUBSCREEN_TITLE_SP);
            mTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            mTitle.setPaddingRelative(mBesideBackInset,
                    mTitle.getPaddingTop(), mTitle.getPaddingEnd(), mTitle.getPaddingBottom());
        } else {
            mTitle.setVisibility(View.GONE);
        }
    }
}
