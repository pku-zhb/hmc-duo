# Inner-screen full-screen AOD

The experimental `innerFullScreenAod` setting defaults to false. It extends the
native current-display eligibility check; it does not make an invisible keyguard
root visible or write display power state.

## Verified firmware contract

The retained SystemUI APK matches the installed APK, SHA-256
`074486d6856c8eda2764d8864d99365e178d4a4758836e3c44152862ddc8b7e1`.
Its `DozeServiceHostInjector.isCurrentScreenFullAodEnabled()` returns:

```java
mFullAodEnable && (!MiuiConfigs.isFoldLargeScreenAndNotPad(mContext)
                  || MiuiConfigs.INNER_SCREEN_SUPPORT_FULL_AOD)
```

`INNER_SCREEN_SUPPORT_FULL_AOD` comes from the device feature
`inner_screen_support_aod_fullscreen`. The global full-AOD manager can return true
while this current-display check returns false. Before this change, HMC used the
global answer and could report full-screen AOD even though the native inner
keyguard panel was invisible.

The native global `mFullAodEnable` still incorporates the user's AOD master switch,
full-screen preference, device support, wallpaper and clock-template eligibility.
For example, the retained firmware explicitly excludes its `doodle` clock
template. This feature does not bypass those checks. It enables only an opted-in
folded-device inner display when the native global answer already permits it.

## Ownership and lifecycle

The AOD plugin has a second, independent gate:
`com.miui.aod.Utils.isCurrentScreenSupportFsAod(Context)` returns
`!inLargeScreen(context) || isInnerScreenSupportFsAod()`. Extending the SystemUI
gate alone admits lock-screen rendering but leaves the physical inner display
OFF. This was observed on the phone in 0.2.13; an outer-display reference remained
ON with the same full-AOD configuration.

0.2.14 extends both native display eligibility checks using the same explicit
choice. SystemUI publishes only that boolean to its own Global setting key. The
plugin observes it once per process with a change listener, not a frame-time
provider query. Its gate is extended only on a fold device whose native display
eligibility was negative; its callers retain full-AOD master, template, schedule,
brightness and power-state policy. The actual plugin loader is discovered from
the typed `MiuiDozeService.onPluginConnected` callback as well as the AOD package
callback, and each actual utility class is hooked once. No global class-loader
hook is introduced.

- The exact no-argument boolean native method is intercepted. A native positive
  answer is preserved; only an opted-in inner display with native master true can
  extend a negative answer.
- Native current-display callers continue to own lock-screen visibility,
  transition dispatch, doze brightness, power policy and normal restoration.
- HMC's own full-screen-AOD query reads this same native display answer. Older
  firmware without the verified contract retains the existing global fallback.
- The switch is evaluated by native calls; it has no polling timer and requires
  no scope restart. Toggle while awake, then enter AOD for a fresh native cycle.
- No persistent firmware feature, system setting or hardware capability is
  overwritten. Disabling the option restores native decisions on subsequent
  calls. Unknown hook contracts preserve the native result.

## Validation boundary

Policy tests cover all combinations of native answer, user opt-in, native master
and inner-display condition. Device checks must independently confirm panel
visibility, actual display doze state, geometry and restoration on disabling.
A passing policy test or global `fullAod=true` is not evidence that an inner panel
is actually rendered. Physical display support, power consumption and final
visual transitions remain device-level observations.
