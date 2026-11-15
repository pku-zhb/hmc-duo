package com.zhuhuibin.hypermusiccover.duo;

/** Null pipeline data retires only its committed holder, never another owner's replacement. */
final class NativeMediaRemovalState {
    private Object notification, card, owner, data;
    private boolean removed;

    boolean accepts(Object nextNotification, Object nextCard, Object nextOwner, Object nextData,
                    Object topData) {
        return !removed || nextNotification != notification || nextCard != card
                || nextOwner != owner || nextData != data || topData != null;
    }

    void commit(Object notification, Object card, Object owner, Object data) {
        this.notification = notification;
        this.card = card;
        this.owner = owner;
        this.data = data;
        removed = false;
    }

    boolean removeTop(Object eventNotification, Object topData, boolean coherent,
                      Object selectedCard, Object selectedOwner, Object selectedData) {
        if (removed || data == null || eventNotification != notification || topData != null) return false;
        if (coherent && (selectedCard != card || selectedOwner != owner || selectedData != data)) return false;
        removed = true;
        return true;
    }

    boolean removeDetached(boolean committedCardAttached, boolean coherentReplacement) {
        if (removed || data == null || committedCardAttached || coherentReplacement) return false;
        removed = true;
        return true;
    }

    Object card() { return card; }
    Object owner() { return owner; }
}
