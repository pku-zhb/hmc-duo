package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class InnerScreenAodPolicyTest {
    @Test public void preservesEveryNativePositiveAnswer() {
        for (int bits = 0; bits < 8; bits++) assertTrue(InnerScreenAodPolicy.enabled(
                true, (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0));
    }

    @Test public void extendsOnlyOptedInInnerWithNativeMasterAndStyleAllowed() {
        for (int bits = 0; bits < 8; bits++) assertEquals("gate " + bits, bits == 7,
                InnerScreenAodPolicy.enabled(false, (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0));
    }
}
