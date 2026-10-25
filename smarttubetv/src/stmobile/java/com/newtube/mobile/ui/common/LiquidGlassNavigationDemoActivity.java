package com.newtube.mobile.ui.common;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Standalone preview for the reusable glass dock. Open with an explicit activity intent. */
public final class LiquidGlassNavigationDemoActivity extends MobileActivity {
    private static final int[] IDS = {1, 2, 3, 4, 5};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        GlassBackdrop backdrop = new GlassBackdrop(this);
        root.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));

        TextView hint = new TextView(this);
        hint.setText("Liquid Glass\nTap a tab · hold to magnify · drag to glide");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(18f);
        hint.setGravity(Gravity.CENTER);
        hint.setShadowLayer(12f, 0f, 2f, 0xAA000000);
        FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        root.addView(hint, hintLp);

        LiquidBottomNavigationView nav = new LiquidBottomNavigationView(this);
        nav.setId(View.generateViewId());
        nav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        nav.getMenu().add(0, IDS[0], 0, "Scopri").setIcon(R.drawable.ic_glass_compass);
        nav.getMenu().add(0, IDS[1], 1, "App").setIcon(R.drawable.ic_glass_grid);
        nav.getMenu().add(0, IDS[2], 2, "Plugin").setIcon(R.drawable.ic_glass_plugin);
        nav.getMenu().add(0, IDS[3], 3, "Account").setIcon(R.drawable.ic_glass_account);
        nav.getMenu().add(0, IDS[4], 4, "Cerca").setIcon(R.drawable.ic_glass_search);
        nav.setSelectedItemId(IDS[2]);
        nav.setBackdropSourceView(backdrop);
        nav.setExpandedLensEnabled(true);
        nav.setHoldToExpandLensEnabled(true);
        nav.setHoldDurationMs(450L);
        nav.setDragToSelectEnabled(true);
        nav.setLensDiameterDp(110f);
        nav.setDockHeightDp(88f);
        nav.setOnItemSelectedListener(item -> {
            nav.setExpandedLensVisible(false, true);
            return true;
        });
        nav.setOnItemReselectedListener(item -> nav.setExpandedLensVisible(
                item.getItemId() == IDS[2], true));

        FrameLayout.LayoutParams navLp = new FrameLayout.LayoutParams(-1, dp(112), Gravity.BOTTOM);
        navLp.leftMargin = dp(12);
        navLp.rightMargin = dp(12);
        navLp.bottomMargin = dp(20);
        navLp.width = -1;
        root.addView(nav, navLp);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        setContentView(root);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** Burgundy-to-navy scene with a low contrast grain of lights, matching the supplied demo. */
    private static final class GlassBackdrop extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        GlassBackdrop(android.content.Context activity) {
            super(activity);
            GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF301018, 0xFF17111F, 0xFF101626});
            setBackground(gradient);
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(0x18FFFFFF);
            float step = getResources().getDisplayMetrics().density * 26f;
            for (float y = 8f; y < getHeight(); y += step) {
                for (float x = 8f + (((int) (y / step) & 1) * step * 0.5f); x < getWidth(); x += step) {
                    canvas.drawCircle(x, y, 1.1f, paint);
                }
            }
        }
    }
}
