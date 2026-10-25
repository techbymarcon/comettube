package com.newtube.mobile.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RecordingCanvas;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * Floating glass navigation bar. Destinations are regular menu items, so applications can keep
 * using BottomNavigationView listeners and accessibility navigation while customizing the glass.
 */
public class LiquidBottomNavigationView extends BottomNavigationView {

    // AGSL backdrop tint and subtle edge distortion (API 33+).
    private static final String IOS_LIQUID_GLASS_SHADER =
            "uniform shader content;\n" +
            "uniform float2 resolution;\n" +
            "\n" +
            "float sdRoundBox(float2 p, float2 b, float r) {\n" +
            "    float2 q = abs(p) - b + r;\n" +
            "    return min(max(q.x, q.y), 0.0) + length(max(q.xy, 0.0)) - r;\n" +
            "}\n" +
            "\n" +
            "half4 main(float2 fragCoord) {\n" +
            "    float2 center = resolution * 0.5;\n" +
            "    float radius = resolution.y * 0.5;\n" +
            "    float2 p = fragCoord - center;\n" +
            "    \n" +
            "    // Signed distance to capsule edge (negative inside, 0 at border)\n" +
            "    float dist = sdRoundBox(p, center, radius);\n" +
            "    if (dist > 0.0) return half4(0.0);\n" +
            "    \n" +
            "    // Calculate surface normal & lens bevel refraction near the rim (within 42px of edge)\n" +
            "    float bevelWidth = 42.0;\n" +
            "    float edgeFactor = clamp(1.0 - (-dist / bevelWidth), 0.0, 1.0);\n" +
            "    float lensCurve = pow(edgeFactor, 2.4);\n" +
            "    \n" +
            "    // Normal vector pointing outward from center/spine\n" +
            "    float2 spine = float2(clamp(p.x, -(center.x - radius), center.x - radius), 0.0);\n" +
            "    float2 normal = normalize(p - spine + 0.0001);\n" +
            "    \n" +
            "    // Displace UV coordinates with subtle chromatic dispersion at the glass rim\n" +
            "    float2 baseOffset = -normal * lensCurve * 28.0;\n" +
            "    half r = content.eval(fragCoord + baseOffset * 1.12).r;\n" +
            "    half g = content.eval(fragCoord + baseOffset * 1.00).g;\n" +
            "    half b = content.eval(fragCoord + baseOffset * 0.88).b;\n" +
            "    half4 color = half4(r, g, b, 1.0);\n" +
            "    \n" +
            "    // Boost saturation (iOS vibrancy)\n" +
            "    half luma = dot(color.rgb, half3(0.299, 0.587, 0.114));\n" +
            "    color.rgb = mix(half3(luma), color.rgb, 1.35);\n" +
            "    \n" +
            "    // Ultra-clear glass surface tint (NOT muddy dark grey) + top-edge inner reflection\n" +
            "    half3 glassTint = half3(0.92, 0.95, 1.0);\n" +
            "    color.rgb = mix(color.rgb, glassTint, 0.08 + lensCurve * 0.18);\n" +
            "    \n" +
            "    // Rim specular glow\n" +
            "    float rimGlow = smoothstep(0.0, 1.0, pow(edgeFactor, 5.0));\n" +
            "    color.rgb += half3(rimGlow * 0.25);\n" +
            "    \n" +
            "    return color;\n" +
            "}\n";

    // Paints
    private final Paint mClearGlassTintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mOuterBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mIndicatorFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mIndicatorStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLensPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLensRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLensHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Geometry rects & paths
    private final RectF mBoundsRect = new RectF();
    private final RectF mBorderRect = new RectF();
    private final RectF mCurrentIndicatorRect = new RectF();
    private final RectF mTargetIndicatorRect = new RectF();
    private final RectF mAnimIndicatorRect = new RectF();
    private final Path mClipPath = new Path();

    // Backdrop Capture State
    private View mBackdropSourceView;
    private RenderNode mBackdropRenderNode;
    private boolean mNeedsRenderEffectUpdate = true;

    // Physics & Motion State
    private ValueAnimator mIndicatorAnimator;
    private float mAnimProgress = 1f;
    private float mMotionVelocity = 0f;
    private float mPressScale = 1f;
    private ValueAnimator mPressAnimator;
    private ValueAnimator mLensAnimator;
    private float mLensProgress;
    private boolean mExpandedLensEnabled;
    private boolean mHoldLensEnabled;
    private boolean mDragToSelectEnabled;
    private boolean mDragging;
    private boolean mHoldActivated;
    private float mTouchDownX;
    private int mDragItemId;
    private float mIndicatorDiameterDp = 72f;
    private float mLensDiameterDp = 108f;
    private float mDockHeightDp = 88f;
    private float mGlassTintOpacity = 0.12f;
    private float mBlurRadiusDp = 12f;
    private float mSpringDampingRatio = 0.65f;
    private long mSpringDurationMs = 280L;
    private float mPressDownScale = 0.965f;
    private int mSelectedColor = Color.rgb(7, 140, 255);
    private int mUnselectedColor = Color.WHITE;
    private long mHoldDurationMs = 450L;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mHoldRunnable;

    // Liquid Glass Ball Hold & Glide Physics
    private float mBallExpandScale = 1.0f;
    private ValueAnimator mBallExpandAnimator;
    private boolean mBallGliding;
    private float mLastTouchX;
    private final Runnable mBallHoldRunnable = this::triggerBallHold;

    private NavigationBarView.OnItemSelectedListener mExternalSelectedListener;

    public LiquidBottomNavigationView(@NonNull Context context) {
        this(context, null);
    }

    public LiquidBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.bottomNavigationStyle);
    }

    public LiquidBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(Color.TRANSPARENT);
        setItemBackground(null);
        setItemActiveIndicatorEnabled(false); // Eliminates any default Material 3 blue circle
        updateItemColors();

        // Dark, slightly translucent glass lets the content's colors show through.
        mClearGlassTintPaint.setStyle(Paint.Style.FILL);
        mClearGlassTintPaint.setColor(Color.argb(31, 20, 18, 28));

        // 2. 1dp directional specular rim stroke
        mOuterBorderPaint.setStyle(Paint.Style.STROKE);
        mOuterBorderPaint.setStrokeWidth(dpToPx(1.0f));

        // 3. Active tab indicator pill (glassy specular gradient & 0.75dp top-lit border)
        mIndicatorFillPaint.setStyle(Paint.Style.FILL);
        mIndicatorStrokePaint.setStyle(Paint.Style.STROKE);
        mIndicatorStrokePaint.setStrokeWidth(dpToPx(0.75f));
        mLensPaint.setStyle(Paint.Style.FILL);
        mLensRimPaint.setStyle(Paint.Style.STROKE);
        mLensRimPaint.setStrokeWidth(dpToPx(1f));
        mLensHighlightPaint.setStyle(Paint.Style.FILL);
        mFallbackPaint.setColor(Color.argb(38, 255, 255, 255));
        rebuildIndicatorShaders();
        rebuildLensShaders();
        setItemIconSize((int) dpToPx(28f));

        // Capsule hardware elevation shadow
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (view.getWidth() > 0 && view.getHeight() > 0) {
                    float dockHeight = Math.min(view.getHeight(), dpToPx(mDockHeightDp));
                    float top = (view.getHeight() - dockHeight) * 0.5f;
                    outline.setRoundRect(0, (int) top, view.getWidth(), (int) (top + dockHeight), dockHeight / 2f);
                }
            }
        });
        setElevation(dpToPx(14));
        setClipToOutline(false);

        // Fluid indicator selection animation
        super.setOnItemSelectedListener(item -> {
            boolean handled = true;
            if (mExternalSelectedListener != null) {
                handled = mExternalSelectedListener.onNavigationItemSelected(item);
            }
            if (handled) {
                post(() -> updateIndicatorRect(true));
            }
            return handled;
        });
    }

    /**
     * Attaches the scrolling content feed (RecyclerView / SwipeRefreshLayout) for real backdrop
     * sampling and hardware refraction.
     */
    public void setBackdropSourceView(View sourceView) {
        if (this.mBackdropSourceView != sourceView) {
            this.mBackdropSourceView = sourceView;
            mNeedsRenderEffectUpdate = true;
            invalidate();
        }
    }

    /** Configure and control the optional expanded lens. Disabled by default. */
    public void setExpandedLensEnabled(boolean enabled) {
        mExpandedLensEnabled = enabled;
        if (!enabled) setExpandedLensVisible(false, true);
    }

    /** Show or hide the selected item's expanded glass lens. */
    public void setExpandedLensVisible(boolean visible, boolean animate) {
        float target = visible && mExpandedLensEnabled ? 1f : 0f;
        if (mLensAnimator != null) mLensAnimator.cancel();
        if (!animate || isReducedMotionEnabled()) {
            mLensProgress = target;
            invalidate();
            return;
        }
        mLensAnimator = ValueAnimator.ofFloat(mLensProgress, target);
        mLensAnimator.setDuration(visible ? 380L : 260L);
        mLensAnimator.addUpdateListener(a -> {
            mLensProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        mLensAnimator.start();
    }

    /** Opt in to a configurable hold gesture. Existing CometTube tab long presses remain intact. */
    public void setHoldToExpandLensEnabled(boolean enabled) {
        mHoldLensEnabled = enabled;
        if (!enabled) cancelHold();
    }

    public void setHoldDurationMs(long durationMs) {
        mHoldDurationMs = Math.max(250L, Math.min(1200L, durationMs));
    }

    /** Opt in to continuous horizontal scrubbing; selection settles on the nearest destination. */
    public void setDragToSelectEnabled(boolean enabled) {
        mDragToSelectEnabled = enabled;
    }

    public void setIndicatorDiameterDp(float diameterDp) {
        mIndicatorDiameterDp = Math.max(48f, diameterDp);
        rebuildIndicatorShaders();
        post(() -> updateIndicatorRect(false));
    }

    public void setLensDiameterDp(float diameterDp) {
        mLensDiameterDp = Math.max(72f, diameterDp);
        rebuildLensShaders();
        invalidate();
    }

    /** Height of the visible capsule when the host reserves extra vertical room for the lens. */
    public void setDockHeightDp(float heightDp) {
        mDockHeightDp = Math.max(64f, heightDp);
        invalidateOutline();
        invalidate();
    }

    public void setSelectedColor(int color) { mSelectedColor = color; updateItemColors(); invalidate(); }
    public void setUnselectedColor(int color) { mUnselectedColor = color; updateItemColors(); invalidate(); }

    private void updateItemColors() {
        ColorStateList colors = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{mSelectedColor, mUnselectedColor});
        setItemIconTintList(colors);
        setItemTextColor(colors);
    }

    public void setGlassTintOpacity(float opacity) {
        mGlassTintOpacity = Math.max(0f, Math.min(0.4f, opacity));
        mClearGlassTintPaint.setColor(Color.argb((int) (mGlassTintOpacity * 255), 20, 18, 28));
        invalidate();
    }

    public void setBlurRadiusDp(float radiusDp) {
        mBlurRadiusDp = Math.max(0f, Math.min(28f, radiusDp));
        mNeedsRenderEffectUpdate = true;
        invalidate();
    }

    public void setSpring(float dampingRatio, long durationMs) {
        mSpringDampingRatio = Math.max(0.5f, Math.min(0.95f, dampingRatio));
        mSpringDurationMs = Math.max(160L, Math.min(700L, durationMs));
    }

    public void setPressDownScale(float scale) {
        mPressDownScale = Math.max(0.92f, Math.min(0.995f, scale));
    }

    private void rebuildIndicatorShaders() {
        float r = dpToPx(mIndicatorDiameterDp) * 0.5f;
        mIndicatorFillPaint.setShader(new RadialGradient(-r * 0.14f, -r * 0.22f, r * 1.2f,
                new int[]{Color.argb(78, 235, 240, 250), Color.argb(48, 195, 205, 222),
                        Color.argb(27, 120, 130, 153)}, new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        mIndicatorStrokePaint.setShader(new LinearGradient(0, -r, 0, r,
                Color.argb(82, 224, 236, 255), Color.argb(24, 224, 236, 255), Shader.TileMode.CLAMP));
    }

    private void rebuildLensShaders() {
        float r = dpToPx(mLensDiameterDp) * 0.5f;
        mLensPaint.setShader(new RadialGradient(-r * 0.28f, -r * 0.34f, r * 1.24f,
                new int[]{Color.argb(20, 145, 205, 255), Color.argb(36, 100, 145, 205),
                        Color.argb(52, 38, 36, 58), Color.argb(24, 148, 196, 255)},
                new float[]{0f, 0.58f, 0.87f, 1f}, Shader.TileMode.CLAMP));
        mLensRimPaint.setShader(new RadialGradient(0, 0, r,
                new int[]{Color.argb(0, 150, 210, 255), Color.argb(105, 115, 190, 255),
                        Color.argb(78, 155, 110, 255)}, new float[]{0.82f, 0.96f, 1f}, Shader.TileMode.CLAMP));
        mLensHighlightPaint.setShader(new RadialGradient(-r * 0.3f, -r * 0.35f, r * 0.56f,
                new int[]{Color.argb(48, 255, 255, 255), Color.argb(0, 255, 255, 255)},
                null, Shader.TileMode.CLAMP));
    }

    @Override
    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        mExternalSelectedListener = listener;
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelHold();
        if (mIndicatorAnimator != null) mIndicatorAnimator.cancel();
        if (mPressAnimator != null) mPressAnimator.cancel();
        if (mLensAnimator != null) mLensAnimator.cancel();
        mBackdropRenderNode = null;
        mNeedsRenderEffectUpdate = true;
        super.onDetachedFromWindow();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Prevent inner BottomNavigationView from consuming window insets and shifting items off-center
        return insets;
    }

    private void triggerBallHold() {
        mBallGliding = true;
        try {
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        } catch (Exception ignored) {}
        animateBallExpand(1.38f);
    }

    private void cancelBallHold() {
        mHandler.removeCallbacks(mBallHoldRunnable);
    }

    private void animateBallExpand(float targetScale) {
        if (mBallExpandAnimator != null) {
            mBallExpandAnimator.cancel();
        }
        if (isReducedMotionEnabled()) {
            mBallExpandScale = targetScale;
            invalidate();
            return;
        }
        mBallExpandAnimator = ValueAnimator.ofFloat(mBallExpandScale, targetScale);
        mBallExpandAnimator.setDuration(220);
        mBallExpandAnimator.addUpdateListener(anim -> {
            mBallExpandScale = (float) anim.getAnimatedValue();
            invalidate();
        });
        mBallExpandAnimator.start();
    }

    private boolean isTouchNearIndicator(float x, float y) {
        if (mCurrentIndicatorRect.isEmpty()) return false;
        float cx = mCurrentIndicatorRect.centerX();
        float cy = mCurrentIndicatorRect.centerY();
        float touchRadius = Math.max(mCurrentIndicatorRect.width() * 0.85f, dpToPx(44f));
        return Math.hypot(x - cx, y - cy) <= touchRadius;
    }

    private int findClosestItemId(float x) {
        if (getChildCount() == 0 || !(getChildAt(0) instanceof ViewGroup)) return 0;
        ViewGroup menu = (ViewGroup) getChildAt(0);
        int closestId = 0;
        float minDistance = Float.MAX_VALUE;
        for (int i = 0; i < menu.getChildCount(); i++) {
            View item = menu.getChildAt(i);
            if (item.getVisibility() != GONE) {
                float itemCenterX = item.getLeft() + (item.getWidth() / 2f);
                float dist = Math.abs(x - itemCenterX);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestId = item.getId();
                }
            }
        }
        return closestId;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getX();
                mLastTouchX = event.getX();
                if (isTouchNearIndicator(event.getX(), event.getY())) {
                    mHandler.postDelayed(mBallHoldRunnable, 160);
                }
                animatePressScale(mPressDownScale);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mBallGliding) {
                    float newX = event.getX();
                    float deltaX = newX - mLastTouchX;
                    mMotionVelocity = deltaX * 14f;
                    mLastTouchX = newX;

                    float halfW = mCurrentIndicatorRect.width() / 2f;
                    float minX = dpToPx(12f) + halfW;
                    float maxX = getWidth() - dpToPx(12f) - halfW;
                    float clampedX = Math.max(minX, Math.min(maxX, newX));

                    mCurrentIndicatorRect.offsetTo(clampedX - halfW, mCurrentIndicatorRect.top);
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelBallHold();
                animatePressScale(1.0f);
                if (mBallGliding) {
                    mBallGliding = false;
                    animateBallExpand(1.0f);
                    int closestId = findClosestItemId(mCurrentIndicatorRect.centerX());
                    if (closestId != 0) {
                        setSelectedItemId(closestId);
                    } else {
                        post(() -> updateIndicatorRect(true));
                    }
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void animatePressScale(float targetScale) {
        if (isReducedMotionEnabled()) {
            mPressScale = targetScale;
            invalidate();
            return;
        }
        if (mPressAnimator != null) {
            mPressAnimator.cancel();
        }
        mPressAnimator = ValueAnimator.ofFloat(mPressScale, targetScale);
        mPressAnimator.setDuration(160);
        mPressAnimator.addUpdateListener(anim -> {
            mPressScale = (float) anim.getAnimatedValue();
            invalidate();
        });
        mPressAnimator.start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getX();
                mLastTouchX = event.getX();
                if (isTouchNearIndicator(event.getX(), event.getY())) {
                    mHandler.postDelayed(mBallHoldRunnable, 160);
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - mTouchDownX);
                if (mBallGliding) {
                    return true;
                }
                if (dx > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    if (isTouchNearIndicator(mTouchDownX, event.getY())) {
                        cancelBallHold();
                        triggerBallHold();
                        return true;
                    }
                    cancelBallHold();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelBallHold();
                break;
        }
        return mBallGliding;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            animatePressScale(mPressDownScale);
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP && !mBallGliding) {
            animatePressScale(1f);
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL && !mBallGliding) {
            animatePressScale(1f);
        }
        return super.dispatchTouchEvent(event);
    }

    private void scheduleHold() {
        cancelHold();
        if (!mHoldLensEnabled || !mExpandedLensEnabled) return;
        mHoldRunnable = () -> {
            mHoldActivated = true;
            setExpandedLensVisible(true, true);
        };
        mHandler.postDelayed(mHoldRunnable, mHoldDurationMs);
    }

    private void cancelHold() {
        if (mHoldRunnable != null) mHandler.removeCallbacks(mHoldRunnable);
        mHoldRunnable = null;
    }

    private int itemIdAt(float x) {
        if (getChildCount() == 0 || !(getChildAt(0) instanceof ViewGroup)) return 0;
        ViewGroup menu = (ViewGroup) getChildAt(0);
        for (int i = 0; i < menu.getChildCount(); i++) {
            View item = menu.getChildAt(i);
            if (item.getVisibility() != GONE && x >= item.getLeft() && x < item.getRight()) return item.getId();
        }
        return 0;
    }

    private boolean isReducedMotionEnabled() {
        return android.provider.Settings.Global.getFloat(getContext().getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }

        mNeedsRenderEffectUpdate = true;

        // 1dp directional specular rim stroke (white 0.7f -> 0.12f -> 0.45f)
        mOuterBorderPaint.setShader(new LinearGradient(
                0, 0, w, h,
                new int[]{
                        Color.argb((int)(0.70f * 255), 255, 255, 255),
                        Color.argb((int)(0.12f * 255), 255, 255, 255),
                        Color.argb((int)(0.45f * 255), 255, 255, 255)
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        ));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0 || getChildCount() == 0) {
            return;
        }

        View child = getChildAt(0);
        if (child instanceof ViewGroup) {
            ViewGroup menuView = (ViewGroup) child;
            int mh = menuView.getMeasuredHeight();
            int topOffset = (height - mh) / 2;

            menuView.layout(0, topOffset, width, topOffset + mh);

            int visibleCount = 0;
            for (int i = 0; i < menuView.getChildCount(); i++) {
                if (menuView.getChildAt(i).getVisibility() != GONE) {
                    visibleCount++;
                }
            }

            if (visibleCount > 0) {
                int horizontalPadding = (int) dpToPx(12f);
                int availableWidth = width - (2 * horizontalPadding);
                int itemWidth = availableWidth / visibleCount;
                int startX = horizontalPadding + (availableWidth - (itemWidth * visibleCount)) / 2;

                int index = 0;
                for (int i = 0; i < menuView.getChildCount(); i++) {
                    View item = menuView.getChildAt(i);
                    if (item.getVisibility() != GONE) {
                        int itemLeft = startX + (index * itemWidth);
                        int itemRight = itemLeft + itemWidth;
                        item.layout(itemLeft, 0, itemRight, mh);

                        // Enforce horizontal padding and prevent Italian label collisions
                        int tabPad = (int) dpToPx(4f);
                        item.setPadding(tabPad, item.getPaddingTop(), tabPad, item.getPaddingBottom());

                        if (item instanceof ViewGroup) {
                            ViewGroup itemGroup = (ViewGroup) item;
                            for (int c = 0; c < itemGroup.getChildCount(); c++) {
                                View tv = itemGroup.getChildAt(c);
                                if (tv instanceof TextView) {
                                    TextView textView = (TextView) tv;
                                    textView.setMaxLines(1);
                                    textView.setEllipsize(TextUtils.TruncateAt.END);
                                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                                }
                            }
                        }

                        index++;
                    }
                }
            }
        }

        updateIndicatorRect(false);
    }

    @Override
    public void setSelectedItemId(int itemId) {
        super.setSelectedItemId(itemId);
        post(() -> updateIndicatorRect(true));
    }

    /**
     * Compute target bounds for the active capsule pill (height 52dp, width tabWidth - 8dp).
     */
    private void updateIndicatorRect(boolean animate) {
        int selectedId = getSelectedItemId();
        if (selectedId == 0 || getChildCount() == 0) {
            return;
        }

        View child = getChildAt(0);
        if (!(child instanceof ViewGroup)) {
            return;
        }

        ViewGroup menuView = (ViewGroup) child;
        View selectedView = menuView.findViewById(selectedId);
        if (selectedView == null || selectedView.getWidth() <= 0) {
            return;
        }

        float centerX = selectedView.getLeft() + (selectedView.getWidth() / 2f);
        float centerY = menuView.getTop() + (selectedView.getHeight() / 2f);

        float diameter = Math.min(dpToPx(mIndicatorDiameterDp), selectedView.getWidth() - dpToPx(8f));
        float pillWidth = diameter;
        float pillHeight = diameter;

        mTargetIndicatorRect.set(
                centerX - (pillWidth / 2f),
                centerY - (pillHeight / 2f),
                centerX + (pillWidth / 2f),
                centerY + (pillHeight / 2f)
        );

        if (mIndicatorAnimator != null && isReducedMotionEnabled()) mIndicatorAnimator.cancel();
        if (mCurrentIndicatorRect.isEmpty() || !animate || isReducedMotionEnabled()) {
            mCurrentIndicatorRect.set(mTargetIndicatorRect);
            mMotionVelocity = 0f;
            mAnimProgress = 1f;
            invalidate();
            return;
        }

        if (mIndicatorAnimator != null) {
            mIndicatorAnimator.cancel();
        }

        final RectF fromRect = new RectF(mCurrentIndicatorRect);
        final float distanceX = mTargetIndicatorRect.centerX() - fromRect.centerX();

        mIndicatorAnimator = ValueAnimator.ofFloat(0f, 1f);
        mIndicatorAnimator.setDuration(mSpringDurationMs);
        mIndicatorAnimator.addUpdateListener(anim -> {
            float t = anim.getAnimatedFraction();
            mAnimProgress = t;

            // Damped harmonic spring physics: dampingRatio = 0.65, stiffness = 380
            float zeta = mSpringDampingRatio;
            float omega = 12f;
            float omegaD = omega * (float) Math.sqrt(1f - zeta * zeta);
            float decay = (float) Math.exp(-zeta * omega * t);
            float springFactor = 1f - decay * ((float) Math.cos(omegaD * t) + (zeta / (float) Math.sqrt(1f - zeta * zeta)) * (float) Math.sin(omegaD * t));

            float v = (float) (Math.abs(distanceX) * decay * Math.sin(omegaD * t));
            mMotionVelocity = (distanceX >= 0 ? 1f : -1f) * v;

            mCurrentIndicatorRect.left = fromRect.left + (mTargetIndicatorRect.left - fromRect.left) * springFactor;
            mCurrentIndicatorRect.right = fromRect.right + (mTargetIndicatorRect.right - fromRect.right) * springFactor;
            mCurrentIndicatorRect.top = fromRect.top + (mTargetIndicatorRect.top - fromRect.top) * springFactor;
            mCurrentIndicatorRect.bottom = fromRect.bottom + (mTargetIndicatorRect.bottom - fromRect.bottom) * springFactor;

            invalidate();
        });
        mIndicatorAnimator.start();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            super.dispatchDraw(canvas);
            return;
        }

        float dockHeight = Math.min(height, dpToPx(mDockHeightDp));
        float dockTop = (height - dockHeight) * 0.5f;
        float radius = dockHeight / 2f;
        mBoundsRect.set(0, dockTop, width, dockTop + dockHeight);

        // 1. REAL BACKDROP CAPTURE & HARDWARE AGSL REFRACTION PIPELINE
        drawBackdrop(canvas, width, height, radius);

        // 2. Translucent charcoal glass tint.
        canvas.drawRoundRect(mBoundsRect, radius, radius, mClearGlassTintPaint);

        // 3. Active Tab Pill (Bright glassy specular gradient with top-lit border)
        if (!mCurrentIndicatorRect.isEmpty()) {
            drawActiveIndicatorPill(canvas);
        }

        // 4. Draw icons and labels (Selected = pure White, Unselected = 75% White)
        super.dispatchDraw(canvas);

        if (mLensProgress > 0f && !mCurrentIndicatorRect.isEmpty()) drawExpandedLens(canvas);

        // 5. 1dp directional specular rim stroke
        float borderHalf = mOuterBorderPaint.getStrokeWidth() / 2f;
        mBorderRect.set(borderHalf, dockTop + borderHalf, width - borderHalf, dockTop + dockHeight - borderHalf);
        canvas.drawRoundRect(mBorderRect, radius - borderHalf, radius - borderHalf, mOuterBorderPaint);
    }

    /**
     * Captures and renders the underlying scrolling feed through hardware blur and AGSL shader.
     */
    private void drawBackdrop(Canvas canvas, int width, int height, float radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mBackdropSourceView != null) {
            if (mBackdropRenderNode == null) {
                mBackdropRenderNode = new RenderNode("CometLiquidGlassBackdrop");
                mBackdropRenderNode.setClipToBounds(true);
            }
            mBackdropRenderNode.setPosition(0, 0, width, height);

            if (mNeedsRenderEffectUpdate) {
                float blurPx = dpToPx(mBlurRadiusDp);
                RenderEffect blur = blurPx > 0f
                        ? RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.MIRROR) : null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    RuntimeShader agsl = new RuntimeShader(IOS_LIQUID_GLASS_SHADER);
                    agsl.setFloatUniform("resolution", (float) width, (float) height);
                    RenderEffect shaderEffect = RenderEffect.createRuntimeShaderEffect(agsl, "content");
                    RenderEffect chain = blur != null
                            ? RenderEffect.createChainEffect(shaderEffect, blur) : shaderEffect;
                    mBackdropRenderNode.setRenderEffect(chain);
                } else {
                    mBackdropRenderNode.setRenderEffect(blur);
                }
                mNeedsRenderEffectUpdate = false;
            }

            int[] navLoc = new int[2];
            int[] srcLoc = new int[2];
            getLocationInWindow(navLoc);
            mBackdropSourceView.getLocationInWindow(srcLoc);
            int offsetX = navLoc[0] - srcLoc[0];
            int offsetY = navLoc[1] - srcLoc[1];

            RecordingCanvas recCanvas = mBackdropRenderNode.beginRecording();
            recCanvas.save();
            recCanvas.translate(-offsetX, -offsetY);
            mBackdropSourceView.draw(recCanvas);
            recCanvas.restore();
            mBackdropRenderNode.endRecording();

            canvas.save();
            mClipPath.reset();
            mClipPath.addRoundRect(mBoundsRect, radius, radius, Path.Direction.CW);
            canvas.clipPath(mClipPath);
            canvas.drawRenderNode(mBackdropRenderNode);
            canvas.restore();
        } else {
            // Graceful fallback for older APIs
            canvas.drawRoundRect(mBoundsRect, radius, radius, mFallbackPaint);
        }
    }

    /**
     * Active Tab Pill: Horizontal capsule (RoundedCornerShape(50), height 52dp, width tabWidth - 8dp)
     * filled with bright glassy specular gradient and a 0.75dp top-lit border.
     */
    private void drawActiveIndicatorPill(Canvas canvas) {
        float centerX = mCurrentIndicatorRect.centerX();
        float centerY = mCurrentIndicatorRect.centerY();
        float baseWidth = mCurrentIndicatorRect.width();
        float baseHeight = mCurrentIndicatorRect.height();

        // Velocity squash & stretch
        float absV = Math.abs(mMotionVelocity);
        float stretchFactor = Math.min(absV * 0.0016f, 0.25f);
        float squashFactor = Math.min(absV * 0.0008f, 0.12f);

        float animWidth = baseWidth * (1f + stretchFactor) * mPressScale * mBallExpandScale;
        float animHeight = baseHeight * (1f - squashFactor) * mPressScale * mBallExpandScale;

        mAnimIndicatorRect.set(
                centerX - (animWidth / 2f),
                centerY - (animHeight / 2f),
                centerX + (animWidth / 2f),
                centerY + (animHeight / 2f)
        );

        float pillRadius = animHeight / 2f;

        // Draw in indicator-local coordinates so the cached gradients move without per-frame shader allocation.
        canvas.save();
        canvas.translate(centerX, centerY);

        // Expanded Liquid Glass droplet lens refraction when holding or gliding
        if (mBallExpandScale > 1.05f) {
            float lensR = pillRadius * 1.25f;
            int alpha = (int) (190 * Math.min(1f, (mBallExpandScale - 1f) / 0.35f));
            mLensPaint.setAlpha(alpha);
            mLensRimPaint.setAlpha(alpha);
            mLensHighlightPaint.setAlpha(alpha);
            canvas.drawCircle(0, 0, lensR, mLensPaint);
            canvas.drawCircle(0, 0, lensR - dpToPx(0.6f), mLensRimPaint);
            canvas.drawCircle(-lensR * 0.28f, -lensR * 0.32f, lensR * 0.52f, mLensHighlightPaint);
        }

        mAnimIndicatorRect.set(-animWidth / 2f, -animHeight / 2f, animWidth / 2f, animHeight / 2f);
        canvas.drawRoundRect(mAnimIndicatorRect, pillRadius, pillRadius, mIndicatorFillPaint);

        // Quiet cool rim, brightest along the upper edge.
        float strokeHalf = mIndicatorStrokePaint.getStrokeWidth() / 2f;
        mBorderRect.set(-animWidth / 2f + strokeHalf, -animHeight / 2f + strokeHalf,
                animWidth / 2f - strokeHalf, animHeight / 2f - strokeHalf);
        canvas.drawRoundRect(mBorderRect, pillRadius - strokeHalf, pillRadius - strokeHalf, mIndicatorStrokePaint);
        canvas.restore();
    }

    private void drawExpandedLens(Canvas canvas) {
        float cx = mCurrentIndicatorRect.centerX();
        float cy = mCurrentIndicatorRect.centerY();
        float normal = Math.min(mCurrentIndicatorRect.width(), mCurrentIndicatorRect.height());
        float diameter = normal + (dpToPx(mLensDiameterDp) - normal) * mLensProgress;
        float radius = diameter * 0.5f;
        float a = mLensProgress;
        float targetRadius = dpToPx(mLensDiameterDp) * 0.5f;
        float scale = targetRadius > 0f ? radius / targetRadius : 1f;
        int alpha = (int) (255 * a);
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        mLensPaint.setAlpha(alpha);
        mLensRimPaint.setAlpha(alpha);
        mLensHighlightPaint.setAlpha(alpha);
        canvas.drawCircle(0f, 0f, targetRadius, mLensPaint);
        canvas.drawCircle(0f, 0f, targetRadius - dpToPx(0.6f), mLensRimPaint);
        canvas.drawCircle(-targetRadius * 0.3f, -targetRadius * 0.35f,
                targetRadius * 0.56f, mLensHighlightPaint);
        canvas.restore();
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
