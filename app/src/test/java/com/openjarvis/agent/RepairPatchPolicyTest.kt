package com.openjarvis.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairPatchPolicyTest {
    @Test fun allowsOnlyJarvisSourceAndTests() {
        assertTrue(RepairPatchPolicy.isAllowedPath("app/src/main/java/com/openjarvis/agent/Fix.kt"))
        assertTrue(RepairPatchPolicy.isAllowedPath("app/src/test/java/com/openjarvis/agent/FixTest.kt"))
        assertFalse(RepairPatchPolicy.isAllowedPath("app/src/main/assets/euronics/index.html"))
        assertFalse(RepairPatchPolicy.isAllowedPath("app/build.gradle"))
        assertFalse(RepairPatchPolicy.isAllowedPath("app/src/main/java/com/openjarvis/../../secrets.txt"))
    }

    @Test fun rejectsDiffsWithoutSafeSourcePaths() {
        assertFalse(RepairPatchPolicy.validateDiff("NO_SAFE_PATCH"))
        assertFalse(RepairPatchPolicy.validateDiff("--- a/app/src/main/assets/euronics/index.html\n+++ b/app/src/main/assets/euronics/index.html"))
        assertTrue(RepairPatchPolicy.validateDiff("--- a/app/src/main/java/com/openjarvis/Fix.kt\n+++ b/app/src/main/java/com/openjarvis/Fix.kt\n@@ -1 +1 @@\n-old\n+new"))
    }
}
