package com.zhuhuibin.hypermusiccover.duo;

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
        for (Class<?> type = implementation; type != null; type = type.getSuperclass()) {
            try {
                type.getDeclaredMethod(method, parameters);
                return type == defaultOwner;
            } catch (NoSuchMethodException absent) {
                // An inherited callback is checked at its declaring class.
            } catch (SecurityException inaccessible) {
                return false;
            } catch (LinkageError unavailable) {
                return false;
            }
        }
        return false;
    }

    private RailShortcutPlacement() {}
}
