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
    }

    @Test public void customOrUnknownCallbacksRetainTheirNotifications() {
        assertFalse(callback(CustomFrame.class));
        assertFalse(callback(InheritsCustomFrame.class));
        assertFalse(callback(Object.class));
        assertFalse(child(SetterChild.class, "setLayoutParams", Object.class));
        assertFalse(child(ResolveChild.class, "resolveLayoutParams"));
        assertFalse(child(RequestChild.class, "requestLayout"));
    }

    private static boolean callback(Class<?> type) {
        return RailShortcutPlacement.inheritsDefaultMethod(type, FrameworkGroup.class,
                "onSetLayoutParams", Object.class, Number.class);
    }

    private static boolean child(Class<?> type, String method, Class<?>... parameters) {
        return RailShortcutPlacement.inheritsDefaultMethod(type, FrameworkView.class, method, parameters);
    }

    private static class FrameworkView {
        public void setLayoutParams(Object params) {}
        public void resolveLayoutParams() {}
        public void requestLayout() {}
    }
    private static class DefaultChild extends FrameworkView {}
    private static class SetterChild extends DefaultChild {
        @Override public void setLayoutParams(Object params) {}
    }
    private static class ResolveChild extends DefaultChild {
        @Override public void resolveLayoutParams() {}
    }
    private static class RequestChild extends DefaultChild {
        @Override public void requestLayout() {}
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
