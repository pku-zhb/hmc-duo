package com.zhuhuibin.hypermusiccover.duo;

import android.view.View;

/** Optional material-provider contract; only explicitly registered element/container pairs participate. */
public interface NativeMaterialTarget {
    int MATERIAL_CONTRACT_VERSION = 1;
    int MATERIAL_GLASS = 1;
    int MATERIAL_BLUR = 2;
    int MATERIAL_FOLLOW = 4;
    int MATERIAL_KEYGUARD = 8;
    int MATERIAL_NATIVE_AOD = 16;
    int MATERIAL_CLAIMED = 32;

    default int hmcMaterialContractVersion() { return MATERIAL_CONTRACT_VERSION; }
    default int hmcMaterialState() { return NativeOwnedMaterialBridge.state(this); }
    default View hmcMaterialOwner() { return NativeOwnedMaterialBridge.owner(this); }
    default boolean hmcMaterialClaim(int version) { return NativeOwnedMaterialBridge.claim(this, version); }
    default boolean hmcMaterialReapply() { return NativeOwnedMaterialBridge.reapply(this); }
}
