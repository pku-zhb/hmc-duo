package com.zhuhuibin.hypermusiccover.duo.updater

import org.junit.Assert.*
import org.junit.Test

class ForkReleaseAssetTest {
    private fun release(vararg names: String) = GithubRelease(
        tagName = "v0.1.0", prerelease = false, body = "",
        htmlUrl = "https://github.com/pku-zhb/hypermusiccover-duo/releases/tag/v0.1.0",
        assets = names.map { GithubAsset(it, "https://example.com/$it") },
    )

    @Test fun selectsPublishedDuoAssetEvenWhenAnUpstreamApkComesFirst() {
        val selected = UpdateApi.releaseApk(release(
            "HyperMusicCover-v0.1.0.apk", "hypermusiccover-duo-v0.1.0.apk",
        ))
        assertEquals("hypermusiccover-duo-v0.1.0.apk", selected?.name)
    }

    @Test fun upstreamAssetsAndNonApkAttachmentsDoNotBecomeForkUpdates() {
        assertNull(UpdateApi.releaseApk(release("HyperMusicCover-v0.1.0.apk")))
        assertNull(UpdateApi.releaseApk(release("HyperMusicCover-Enhanced-v0.1.0.apk")))
        assertNull(UpdateApi.releaseApk(release("hypermusiccover-duo-v0.1.0.apk.sha256")))
        assertNull(UpdateApi.releaseApk(release()))
    }

    @Test fun aSmallUpdateIsNewerThanItsBaseAndOlderThanTheNextVersion() {
        assertEquals(true, isNewerVersion("1.1.0-1", "1.1.0"))
        assertEquals(true, isNewerVersion("1.1.0-2", "1.1.0-1"))
        assertEquals(true, isNewerVersion("1.1.0-10", "1.1.0-9"))
        assertEquals(true, isNewerVersion("1.1.0-rev2", "1.1.0-rev1"))
        assertEquals(true, isNewerVersion("1.1.1", "1.1.0-5"))
        assertEquals(false, isNewerVersion("1.1.0-1", "1.1.0-1"))
        assertEquals(false, isNewerVersion("1.1.0", "1.1.0-1"))
        assertEquals(false, isNewerVersion("1.1.0-1", "1.1.1"))
    }

    @Test fun devAndNightlySuffixesAreNotSmallUpdates() {
        assertEquals(true, isNewerVersion("1.1.0-1", "1.1.0-dev 925"))
        assertEquals(false, isNewerVersion("1.1.0", "1.1.0-dev 925"))
        assertEquals(true, isNewerVersion("1.1.0-1", "1.1.0-nightly.20261014.abc1234"))
        assertNull(isNewerVersion("1.1.0-1", "local"))
    }

    @Test fun smallUpdateTagsMatchTheirOwnAsset() {
        val small = GithubRelease("v1.1.0-1", false, "", "", listOf(
            GithubAsset("hypermusiccover-duo-v1.1.0.apk", "a"),
            GithubAsset("hypermusiccover-duo-v1.1.0-1.apk", "b"),
        ))
        assertEquals("b", UpdateApi.releaseApk(small)?.url)
    }

    @Test fun matchingTagTakesPrecedenceOverFallbackAssets() {
        val selected = UpdateApi.releaseApk(release(
            "hypermusiccover-duo-v0.0.9.apk", "hypermusiccover-duo-v0.1.0.apk",
        ))
        assertEquals("hypermusiccover-duo-v0.1.0.apk", selected?.name)
    }
    @Test fun aStableReleaseReplacesItsPreview() {
        assertEquals(true, isNewerVersion("0.3.0", "0.3.0-preview.1"))
        assertEquals(false, isNewerVersion("0.3.0-preview.1", "0.3.0"))
    }

    @Test fun aDifferentVersionAssetIsNeverUsedAsFallback() {
        assertNull(UpdateApi.releaseApk(release("hypermusiccover-duo-v0.0.9.apk")))
    }
    @Test fun signedSuccessorsAreAcceptedButOldKeysAndAdditionalSignersAreRejected() {
        assertTrue(acceptsSigningLineage(listOf("old"), listOf("old"), emptyList()))
        assertTrue(acceptsSigningLineage(listOf("old"), listOf("new"), listOf("old", "new")))
        assertFalse(acceptsSigningLineage(listOf("new"), listOf("old"), listOf("old")))
        assertFalse(acceptsSigningLineage(listOf("old"), listOf("other"), listOf("old", "new")))
        assertFalse(acceptsSigningLineage(listOf("old"), listOf("old", "other"), emptyList()))
        assertFalse(acceptsSigningLineage(emptyList(), emptyList(), emptyList()))
    }
}
