# Liquid glass navigation

`LiquidBottomNavigationView` is a reusable Android Views component built on
Material's `BottomNavigationView`. Destinations are normal menu items; application
selection and reselection callbacks, accessibility focus, and keyboard navigation
continue to use the standard navigation API.

## Integration

```java
LiquidBottomNavigationView nav = findViewById(R.id.bottom_nav);
nav.getMenu().add(0, HOME, 0, "Home").setIcon(R.drawable.ic_home);
nav.getMenu().add(0, LIBRARY, 1, "Library").setIcon(R.drawable.ic_library);
nav.setSelectedItemId(HOME);
nav.setOnItemSelectedListener(item -> {
    showDestination(item.getItemId());
    return true;
});
nav.setBackdropSourceView(contentBehindTheBar);

nav.setSelectedColor(Color.rgb(7, 140, 255));
nav.setUnselectedColor(Color.WHITE);
nav.setIndicatorDiameterDp(72);
nav.setGlassTintOpacity(.12f);
nav.setBlurRadiusDp(12); // RenderEffect on API 31+, translucent fallback below
nav.setExpandedLensEnabled(true);
nav.setLensDiameterDp(110);
nav.setHoldDurationMs(450);
nav.setHoldToExpandLensEnabled(true);
nav.setDragToSelectEnabled(true);
```

The selected item can also be expanded programmatically with
`setExpandedLensVisible(true, true)`. If the host reserves extra vertical space,
`setDockHeightDp(88)` keeps the capsule at its normal height while the lens has
room to grow. Place the view in a parent that does not clip children when making
the lens extend beyond the capsule. Insets remain the host's responsibility.

## Preview

`LiquidGlassNavigationDemoActivity` is an unexported preview activity containing
the five Italian destinations from the supplied reference. From a debug install:

```bash
adb shell am start -n com.comettube/com.newtube.mobile.ui.common.LiquidGlassNavigationDemoActivity
```

Tap tabs to move the indicator, tap Plugin again to toggle the lens, hold a tab to
expand it, and drag across the dock to scrub. The production CometTube browse
screen uses its existing dynamic menu and keeps its existing section long-press
actions.

## Rendering notes

The base dock samples the host content into a `RenderNode`; API 31+ applies a
small `RenderEffect` blur and API 33+ also applies the AGSL color/refraction
effect. Older Android versions keep the tint, edge highlights, and selection
surface without the backdrop shader. The expanded sphere uses GPU radial gradients
and a cool rim. It is a visual approximation: Android does not expose the iOS
backdrop refraction pipeline, so text and icons beneath the lens are not physically
warped. Reducing Android's animator duration scale to zero disables the lens
transition animation.
