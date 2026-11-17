package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailShortcutPlacementTest {
    @Test public void repeatedNativeMetadataRepairsDoNotRelayoutAnAlreadyPlacedShortcut() {
        assertEquals(RailShortcutPlacement.Action.METADATA,
                RailShortcutPlacement.action(true, true, true));
    }

    @Test public void firstPlacementAndWindowChangesStillRequestNativeLayout() {
        assertEquals(RailShortcutPlacement.Action.LAYOUT,
                RailShortcutPlacement.action(true, false, true));
    }

    @Test public void nativeAnimationWithoutAParameterChangeDoesNotAcquireLayoutOwnership() {
        assertEquals(RailShortcutPlacement.Action.NONE,
                RailShortcutPlacement.action(false, false, true));
    }

    @Test public void rtlAndUnknownParentContractsKeepTheNativeSetter() {
        assertEquals(RailShortcutPlacement.Action.LAYOUT,
                RailShortcutPlacement.action(true, true, false));
    }

    @Test public void inheritedFrameworkCallbackAllowsMetadataRepair() {
        assertTrue(callback(DefaultFrame.class));
        assertTrue(callback(NestedFrame.class));
        assertTrue(callback(OverloadedFrame.class));
        assertTrue(child(DefaultChild.class, "setLayoutParams", Object.class));
        assertTrue(child(DefaultChild.class, "resolveLayoutParams"));
        assertTrue(child(DefaultChild.class, "requestLayout"));
        DefaultResolvingFrame group = new DefaultResolvingFrame();
        ResolveChild child = new ResolveChild();
        group.child = child;
        assertTrue(RailShortcutPlacement.resolve(group, RailShortcutPlacement.defaultMethod(
                DefaultResolvingFrame.class, FrameworkResolvingGroup.class, "resolveLayoutParams")));
        assertEquals(1, group.resolutions);
        assertEquals(1, child.customResolutions);
    }

    @Test public void customOrUnknownCallbacksRetainTheirNotifications() {
        assertFalse(callback(CustomFrame.class));
        assertFalse(callback(InheritsCustomFrame.class));
        assertFalse(callback(Object.class));
        assertFalse(child(SetterChild.class, "setLayoutParams", Object.class));
        assertFalse(child(ResolveChild.class, "resolveLayoutParams"));
        assertFalse(child(RequestChild.class, "requestLayout"));
        DefaultResolvingFrame group = new DefaultResolvingFrame();
        group.child = new FailingResolveChild();
        assertFalse(RailShortcutPlacement.resolve(group, RailShortcutPlacement.defaultMethod(
                DefaultResolvingFrame.class, FrameworkResolvingGroup.class, "resolveLayoutParams")));
        assertFalse(RailShortcutPlacement.resolve(group, null));
    }

    private static boolean callback(Class<?> type) {
        return RailShortcutPlacement.inheritsDefaultMethod(type, FrameworkGroup.class,
                "onSetLayoutParams", Object.class, Number.class);
    }

    private static boolean child(Class<?> type, String method, Class<?>... parameters) {
        return RailShortcutPlacement.inheritsDefaultMethod(type, FrameworkView.class, method, parameters);
    }

    public static class FrameworkView {
        int resolutions;
        public void setLayoutParams(Object params) {}
        public void resolveLayoutParams() { resolutions++; }
        public void requestLayout() {}
    }
    private static class DefaultChild extends FrameworkView {}
    private static class SetterChild extends DefaultChild {
        @Override public void setLayoutParams(Object params) {}
    }
    private static class ResolveChild extends DefaultChild {
        int customResolutions;
        @Override public void resolveLayoutParams() { customResolutions++; }
    }
    private static class RequestChild extends DefaultChild {
        @Override public void requestLayout() {}
    }
    public static class FrameworkResolvingGroup extends FrameworkView {
        FrameworkView child;
        @Override public void resolveLayoutParams() {
            super.resolveLayoutParams();
            if (child != null) child.resolveLayoutParams();
        }
    }
    private static class DefaultResolvingFrame extends FrameworkResolvingGroup {}
    private static class FailingResolveChild extends DefaultChild {
        @Override public void resolveLayoutParams() { throw new IllegalStateException("native child failure"); }
    }

    private static class FrameworkGroup {
        protected void onSetLayoutParams(Object child, Number params) {}
    }
    private static class DefaultFrame extends FrameworkGroup {}
    private static class NestedFrame extends DefaultFrame {}
    private static class CustomFrame extends DefaultFrame {
        @Override protected void onSetLayoutParams(Object child, Number params) {}
    }
    private static class InheritsCustomFrame extends CustomFrame {}
    private static class OverloadedFrame extends DefaultFrame {
        protected void onSetLayoutParams(Object child, String params) {}
    }
}
