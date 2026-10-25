package com.newtube.mobile.ui.setup;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.LanguageSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.common.ThemeMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * First-time modern setup flow for CometTube.
 */
public class CometSetupActivity extends Activity {

    private static final String PREF_SETUP = "comettube_setup";
    private static final String KEY_COMPLETED = "setup_completed";

    public static boolean isCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SETUP, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_COMPLETED, false);
    }

    public static void setCompleted(Context context, boolean completed) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SETUP, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_COMPLETED, completed).apply();
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private FrameLayout mSetupRoot;
    private View mStepIntro;
    private ImageView mIntroLogo;
    private View mIntroTextContainer;
    private View mBtnProceedIntro;

    private View mStepTheme;
    private TextView mTitleTheme;
    private TextView mSubTheme;
    private MaterialCardView mCardDark;
    private MaterialCardView mCardLight;
    private ImageView mCheckDark;
    private ImageView mCheckLight;
    private View mBtnProceedTheme;
    private int mSelectedTheme = ThemeMode.DARK;

    private View mStepLanguage;
    private ImageView mIvGlobe;
    private LinearLayout mLangListContainer;
    private View mBtnProceedLanguage;
    private String mSelectedLangCode = "en";

    private View mStepWelcome;
    private TextView mTxtFinalWelcome;

    private static class LanguageItem {
        final String flag;
        final String name;
        final String code;

        LanguageItem(String flag, String name, String code) {
            this.flag = flag;
            this.name = name;
            this.code = code;
        }
    }

    private final List<LanguageItem> mLanguages = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comet_setup);

        mSetupRoot = findViewById(R.id.setup_root);

        // Step 1: Intro
        mStepIntro = findViewById(R.id.step_intro);
        mIntroLogo = findViewById(R.id.intro_logo);
        mIntroTextContainer = findViewById(R.id.intro_text_container);
        mBtnProceedIntro = findViewById(R.id.btn_proceed_intro);

        // Step 2: Theme
        mStepTheme = findViewById(R.id.step_theme);
        mTitleTheme = findViewById(R.id.title_theme);
        mSubTheme = findViewById(R.id.sub_theme);
        mCardDark = findViewById(R.id.card_theme_dark);
        mCardLight = findViewById(R.id.card_theme_light);
        mCheckDark = findViewById(R.id.check_dark);
        mCheckLight = findViewById(R.id.check_light);
        mBtnProceedTheme = findViewById(R.id.btn_proceed_theme);

        // Step 3: Language
        mStepLanguage = findViewById(R.id.step_language);
        mIvGlobe = findViewById(R.id.iv_rotating_globe);
        mLangListContainer = findViewById(R.id.language_list_container);
        mBtnProceedLanguage = findViewById(R.id.btn_proceed_language);

        // Step 4: Final Welcome
        mStepWelcome = findViewById(R.id.step_welcome);
        mTxtFinalWelcome = findViewById(R.id.txt_final_welcome);

        initLanguages();
        setupThemeStep();
        setupLanguageStep();

        startIntroAnimation();
    }

    private void initLanguages() {
        mLanguages.add(new LanguageItem("🇺🇸", "English", "en"));
        mLanguages.add(new LanguageItem("🇮🇹", "Italiano", "it"));
        mLanguages.add(new LanguageItem("🇪🇸", "Español", "es"));
        mLanguages.add(new LanguageItem("🇫🇷", "Français", "fr"));
        mLanguages.add(new LanguageItem("🇩🇪", "Deutsch", "de"));
        mLanguages.add(new LanguageItem("🇷🇺", "Русский", "ru"));
        mLanguages.add(new LanguageItem("🇵🇹", "Português", "pt"));
        mLanguages.add(new LanguageItem("🇯🇵", "日本語", "ja"));
        mLanguages.add(new LanguageItem("🇨🇳", "简体中文", "zh-CN"));
        mLanguages.add(new LanguageItem("🇹🇼", "繁體中文", "zh-TW"));
        mLanguages.add(new LanguageItem("🇰🇷", "한국어", "ko"));
        mLanguages.add(new LanguageItem("🇸🇦", "العربية", "ar"));
        mLanguages.add(new LanguageItem("🇮🇳", "हिन्दी", "hi"));
        mLanguages.add(new LanguageItem("🇹🇷", "Türkçe", "tr"));
        mLanguages.add(new LanguageItem("🇵🇱", "Polski", "pl"));

        // Match current default locale if available
        String currentLang = Locale.getDefault().getLanguage();
        for (LanguageItem item : mLanguages) {
            if (item.code.startsWith(currentLang)) {
                mSelectedLangCode = item.code;
                break;
            }
        }
    }

    private void startIntroAnimation() {
        // Logo starts translated above viewport
        mIntroLogo.setTranslationY(-800f);
        mIntroLogo.animate()
                .translationY(0f)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .setDuration(900)
                .start();

        // 1 second later: "welcome to", COMET text, and Tube fade in together
        mHandler.postDelayed(() -> {
            mIntroTextContainer.animate()
                    .alpha(1f)
                    .setDuration(700)
                    .start();
        }, 1000);

        // 1 second after text (2s total): pill-shaped liquid glass "Proceed" button appears
        mHandler.postDelayed(() -> {
            mBtnProceedIntro.setScaleX(0.85f);
            mBtnProceedIntro.setScaleY(0.85f);
            mBtnProceedIntro.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(new OvershootInterpolator(1.3f))
                    .setDuration(600)
                    .start();
        }, 2000);

        mBtnProceedIntro.setOnClickListener(v -> {
            mStepIntro.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        mStepIntro.setVisibility(View.GONE);
                        mStepTheme.setVisibility(View.VISIBLE);
                        mStepTheme.animate().alpha(1f).setDuration(400).start();
                    })
                    .start();
        });
    }

    private void setupThemeStep() {
        mCardDark.setOnClickListener(v -> selectTheme(ThemeMode.DARK));
        mCardLight.setOnClickListener(v -> selectTheme(ThemeMode.LIGHT));

        mBtnProceedTheme.setOnClickListener(v -> {
            ThemeMode.set(this, mSelectedTheme);
            mStepTheme.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        mStepTheme.setVisibility(View.GONE);
                        mStepLanguage.setVisibility(View.VISIBLE);
                        startRotatingGlobe();
                        mStepLanguage.animate().alpha(1f).setDuration(400).start();
                    })
                    .start();
        });
    }

    private void selectTheme(int theme) {
        if (mSelectedTheme == theme) return;
        mSelectedTheme = theme;

        int fromBg = theme == ThemeMode.LIGHT ? Color.BLACK : Color.parseColor("#F8F8FA");
        int toBg = theme == ThemeMode.LIGHT ? Color.parseColor("#F8F8FA") : Color.BLACK;

        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), fromBg, toBg);
        colorAnim.setDuration(350);
        colorAnim.addUpdateListener(animator -> {
            int val = (int) animator.getAnimatedValue();
            mSetupRoot.setBackgroundColor(val);
        });
        colorAnim.start();

        boolean isDark = (theme == ThemeMode.DARK);
        mCheckDark.setVisibility(isDark ? View.VISIBLE : View.GONE);
        mCheckLight.setVisibility(!isDark ? View.VISIBLE : View.GONE);

        mCardDark.setStrokeColor(isDark ? Color.parseColor("#007AFF") : Color.parseColor("#33FFFFFF"));
        mCardDark.setStrokeWidth(isDark ? 5 : 2);

        mCardLight.setStrokeColor(!isDark ? Color.parseColor("#007AFF") : Color.parseColor("#33FFFFFF"));
        mCardLight.setStrokeWidth(!isDark ? 5 : 2);

        mTitleTheme.setTextColor(isDark ? Color.WHITE : Color.parseColor("#111111"));
        mSubTheme.setTextColor(isDark ? Color.parseColor("#8E8E93") : Color.parseColor("#6E6E73"));
    }

    private void startRotatingGlobe() {
        RotateAnimation rotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(4500);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        mIvGlobe.startAnimation(rotate);
    }

    private void setupLanguageStep() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (LanguageItem item : mLanguages) {
            View row = inflater.inflate(R.layout.item_setup_language_row, mLangListContainer, false);
            TextView flag = row.findViewById(R.id.lang_flag);
            TextView name = row.findViewById(R.id.lang_name);
            ImageView check = row.findViewById(R.id.lang_check);

            flag.setText(item.flag);
            name.setText(item.name);
            check.setVisibility(item.code.equals(mSelectedLangCode) ? View.VISIBLE : View.GONE);

            row.setOnClickListener(v -> {
                mSelectedLangCode = item.code;
                updateLanguageSelection();
            });

            mLangListContainer.addView(row);
        }

        mBtnProceedLanguage.setOnClickListener(v -> {
            applyLanguageSelection();
            setCompleted(this, true);

            // Step 4: Fade to blank screen, then center "Welcome." fades in
            mStepLanguage.animate()
                    .alpha(0f)
                    .setDuration(350)
                    .withEndAction(() -> {
                        mStepLanguage.setVisibility(View.GONE);
                        mSetupRoot.setBackgroundColor(Color.BLACK);
                        mStepWelcome.setVisibility(View.VISIBLE);
                        mStepWelcome.animate()
                                .alpha(1f)
                                .setDuration(800)
                                .withEndAction(() -> {
                                    // Pause for 1.2s then fade into home screen
                                    mHandler.postDelayed(() -> {
                                        mStepWelcome.animate()
                                                .alpha(0f)
                                                .setDuration(600)
                                                .withEndAction(() -> {
                                                    finish();
                                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                                })
                                                .start();
                                    }, 1200);
                                })
                                .start();
                    })
                    .start();
        });
    }

    private void updateLanguageSelection() {
        for (int i = 0; i < mLangListContainer.getChildCount(); i++) {
            View child = mLangListContainer.getChildAt(i);
            ImageView check = child.findViewById(R.id.lang_check);
            LanguageItem item = mLanguages.get(i);
            check.setVisibility(item.code.equals(mSelectedLangCode) ? View.VISIBLE : View.GONE);
        }
    }

    private void applyLanguageSelection() {
        try {
            new com.liskovsoft.sharedutils.locale.LocaleUpdater(this).setPreferredLanguage(mSelectedLangCode);
        } catch (Exception ignored) {}
    }
}
