package com.zhuhuibin.hypermusiccover.duo;

import java.lang.ref.WeakReference;
import java.util.function.BooleanSupplier;

/** One explicitly validated media background; global setters never discover sources. */
final class NativeMaterialSourceBinding<T> {
    private WeakReference<T> source = new WeakReference<>(null), owner = new WeakReference<>(null);
    private WeakReference<Object> parent = new WeakReference<>(null), ownerParent = new WeakReference<>(null);

    boolean isSource(T candidate) { return candidate != null && source.get() == candidate; }
    T source() { return source.get(); }
    T owner() { return owner.get(); }

    boolean matches(T candidate, Object candidateParent, T candidateOwner, Object candidateOwnerParent) {
        return isSource(candidate) && candidateOwner != null && owner.get() == candidateOwner &&
                candidateParent != null && parent.get() == candidateParent &&
                candidateOwnerParent != null && ownerParent.get() == candidateOwnerParent;
    }

    boolean bind(T candidate, Object candidateParent, T candidateOwner, Object candidateOwnerParent,
                 BooleanSupplier validate) {
        if (matches(candidate, candidateParent, candidateOwner, candidateOwnerParent)) return true;
        clear();
        if (candidate == null || candidateOwner == null || candidateParent == null ||
                candidateOwnerParent == null || !validate.getAsBoolean()) return false;
        source = new WeakReference<>(candidate); owner = new WeakReference<>(candidateOwner);
        parent = new WeakReference<>(candidateParent); ownerParent = new WeakReference<>(candidateOwnerParent);
        return true;
    }

    void clear() { source.clear(); owner.clear(); parent.clear(); ownerParent.clear(); }
}
