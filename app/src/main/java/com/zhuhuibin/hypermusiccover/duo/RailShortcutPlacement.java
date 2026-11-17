package com.zhuhuibin.hypermusiccover.duo;

import java.lang.reflect.Method;

/** Separate shortcut parameter repair from a change to its already laid-out box. */
final class RailShortcutPlacement {
    enum Action { NONE, METADATA, LAYOUT }

    static Action action(boolean parametersChanged, boolean boxMatches, boolean metadataSafe) {
        if (!parametersChanged) return Action.NONE;
        return boxMatches && metadataSafe ? Action.METADATA : Action.LAYOUT;
    }

    /** Custom methods may do more than the framework's default traversal request. */
    static boolean inheritsDefaultMethod(Class<?> implementation, Class<?> defaultOwner,
                                        String method, Class<?>... parameters) {
        return defaultMethod(implementation, defaultOwner, method, parameters) != null;
    }

    static Method defaultMethod(Class<?> implementation, Class<?> defaultOwner,
                                String method, Class<?>... parameters) {
        for (Class<?> type = implementation; type != null; type = type.getSuperclass()) {
            try {
                Method found = type.getDeclaredMethod(method, parameters);
                return type == defaultOwner ? found : null;
            } catch (NoSuchMethodException absent) {
                // An inherited callback is checked at its declaring class.
            } catch (SecurityException inaccessible) {
                return null;
            } catch (LinkageError unavailable) {
                return null;
            }
        }
        return null;
    }

    static boolean resolve(Object view, Method method) {
        if (method == null) return false;
        try {
            method.invoke(view);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            return false;
        }
    }

    private RailShortcutPlacement() {}
}
