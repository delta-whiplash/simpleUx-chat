package chat.simplex.app

import chat.simplex.common.views.ux.update.AppUpdateVersionComparison
import chat.simplex.common.views.ux.update.RELEASES_API_URL
import chat.simplex.common.platform.UpdateChannel
import chat.simplex.common.views.ux.update.compareAppUpdateVersions
import chat.simplex.common.views.ux.update.selectAppUpdateRelease
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the pure logic of the in-app updater (#79, #97): fork-version comparison
 * ("7.0.366-ux.<N>" / "v7.0.366-ux.<N>") and GitHub releases JSON selection.
 * No network: everything parses from String fixtures.
 *
 * Rules under test:
 *  - a candidate that is not a parseable fork version ("rolling", malformed,
 *    missing "-ux." counter) is NEVER proposed as an update;
 *  - NEWER only when the candidate (upstream base tuple, then fork counter)
 *    is strictly greater than the running version;
 *  - ROLLING channel considers all releases and selects the newest by version;
 *  - STABLE channel only considers stable releases (prerelease=false);
 *  - only assets named `simplex-ux-...-arm64-v8a.apk` are downloadable.
 */
class AppUpdaterTest {

  // --- version comparison ---

  @Test
  fun sameVersionIsSameOrOlder() {
    assertEquals(
      AppUpdateVersionComparison.SAME_OR_OLDER,
      compareAppUpdateVersions("7.0.366-ux.5", "7.0.366-ux.5")
    )
  }

  @Test
  fun leadingVIsStripped() {
    assertEquals(
      AppUpdateVersionComparison.SAME_OR_OLDER,
      compareAppUpdateVersions("7.0.366-ux.5", "v7.0.366-ux.5")
    )
    assertEquals(
      AppUpdateVersionComparison.NEWER,
      compareAppUpdateVersions("7.0.366-ux.5", "V7.0.366-ux.6")
    )
  }

  @Test
  fun newerForkCounterIsNewer() {
    assertEquals(
      AppUpdateVersionComparison.NEWER,
      compareAppUpdateVersions("7.0.366-ux.5", "7.0.366-ux.6")
    )
  }

  @Test
  fun olderForkCounterIsSameOrOlder() {
    assertEquals(
      AppUpdateVersionComparison.SAME_OR_OLDER,
      compareAppUpdateVersions("7.0.366-ux.6", "7.0.366-ux.5")
    )
  }

  @Test
  fun newerUpstreamBaseWinsOverForkCounter() {
    assertEquals(
      AppUpdateVersionComparison.NEWER,
      compareAppUpdateVersions("7.0.366-ux.99", "7.0.367-ux.0")
    )
    assertEquals(
      AppUpdateVersionComparison.SAME_OR_OLDER,
      compareAppUpdateVersions("7.0.366-ux.0", "7.0.365-ux.99")
    )
  }

  @Test
  fun baseTuplesOfDifferentLengthPadWithZeros() {
    assertEquals(
      AppUpdateVersionComparison.SAME_OR_OLDER,
      compareAppUpdateVersions("7.0.366-ux.1", "7.0.366.0.0-ux.1")
    )
  }

  @Test
  fun rollingCandidateIsNeverAVersion() {
    assertEquals(
      AppUpdateVersionComparison.NOT_A_VERSION,
      compareAppUpdateVersions("7.0.366-ux.5", "rolling")
    )
  }

  @Test
  fun malformedCandidatesAreNeverAVersion() {
    for (candidate in listOf("", "abc", "7.0.366", "7.0.366-beta.1", "7.0.366-ux.x", "ux.5")) {
      assertEquals(
        AppUpdateVersionComparison.NOT_A_VERSION,
        compareAppUpdateVersions("7.0.366-ux.5", candidate),
        "candidate \"$candidate\" must not be proposed as an update"
      )
    }
  }

  @Test
  fun unparseableCurrentVersionNeverProposesAnUpdate() {
    assertEquals(
      AppUpdateVersionComparison.NOT_A_VERSION,
      compareAppUpdateVersions("rolling", "7.0.366-ux.6")
    )
  }

  @Test
  fun unstampedCurrentVersionCanBeUpdated() {
    // Local builds and pre-#72 installs have no "-ux." counter - the upstream base still
    // decides, with the fork counter treated as 0.
    assertEquals(
      AppUpdateVersionComparison.NEWER,
      compareAppUpdateVersions("7.0.1", "7.0.366-ux.5")
    )
    assertEquals(
      AppUpdateVersionComparison.NEWER,
      compareAppUpdateVersions("7.0.366", "7.0.366-ux.1")
    )
  }

  @Test
  fun newerUnstampedCurrentIsNotProposedAnUpdate() {
    assertEquals(
      AppUpdateVersionComparison.SAME_OR_OLDER,
      compareAppUpdateVersions("9.0.0", "7.0.366-ux.5")
    )
  }

  // --- releases JSON selection ---

  private val releasesFixture = """
    [
      {
        "url": "https://api.github.com/repos/delta-whiplash/simpleUx-chat/releases/1",
        "tag_name": "rolling",
        "prerelease": true,
        "assets": [
          { "name": "SHA256SUMS.txt", "browser_download_url": "https://github.com/delta-whiplash/simpleUx-chat/releases/download/rolling/SHA256SUMS.txt", "size": 642 },
          { "name": "simplex-ux-7.0.367-ux.2-arm64-v8a.apk", "browser_download_url": "https://github.com/delta-whiplash/simpleUx-chat/releases/download/rolling/simplex-ux-7.0.367-ux.2-arm64-v8a.apk", "size": 98765432 }
        ]
      },
      {
        "url": "https://api.github.com/repos/delta-whiplash/simpleUx-chat/releases/2",
        "tag_name": "v7.0.366-ux.5",
        "prerelease": false,
        "assets": [
          { "name": "simplex-ux-7.0.366-ux.5-arm64-v8a.apk", "browser_download_url": "https://github.com/delta-whiplash/simpleUx-chat/releases/download/v7.0.366-ux.5/simplex-ux-7.0.366-ux.5-arm64-v8a.apk", "size": 97654321 }
        ]
      }
    ]
  """.trimIndent()

  @Test
  fun manualCheckTakesFirstReleaseIncludingRolling() {
    val candidate = selectAppUpdateRelease(releasesFixture, UpdateChannel.ROLLING, "7.0.366-ux.1")
    assertNotNull(candidate)
    assertEquals("rolling", candidate.tagName)
    // The rolling tag is not versionable - the version comes from the asset name.
    assertEquals("7.0.367-ux.2", candidate.version)
    assertEquals("simplex-ux-7.0.367-ux.2-arm64-v8a.apk", candidate.apkName)
    assertEquals(
      "https://github.com/delta-whiplash/simpleUx-chat/releases/download/rolling/simplex-ux-7.0.367-ux.2-arm64-v8a.apk",
      candidate.downloadUrl
    )
    assertEquals(98765432L, candidate.sizeBytes)
  }

  @Test
  fun autoCheckSkipsPrereleases() {
    val candidate = selectAppUpdateRelease(releasesFixture, UpdateChannel.STABLE, "7.0.366-ux.1")
    assertNotNull(candidate)
    assertEquals("v7.0.366-ux.5", candidate.tagName)
    assertEquals("7.0.366-ux.5", candidate.version)
    assertEquals("simplex-ux-7.0.366-ux.5-arm64-v8a.apk", candidate.apkName)
  }

  @Test
  fun assetFilteringPicksOnlyArm64Apk() {
    val json = """
      [
        {
          "tag_name": "v7.0.366-ux.7",
          "prerelease": false,
          "assets": [
            { "name": "SHA256SUMS.txt", "browser_download_url": "https://example.com/SHA256SUMS.txt", "size": 100 },
            { "name": "simplex-ux-7.0.366-ux.7-armeabi-v7a.apk", "browser_download_url": "https://example.com/armeabi.apk", "size": 200 },
            { "name": "simplex-ux-7.0.366-ux.7-universal.apk", "browser_download_url": "https://example.com/universal.apk", "size": 300 },
            { "name": "simplex-ux-7.0.366-ux.7-arm64-v8a.apk", "browser_download_url": "https://example.com/arm64.apk", "size": 400 }
          ]
        }
      ]
    """.trimIndent()
    val candidate = selectAppUpdateRelease(json, UpdateChannel.STABLE, "7.0.366-ux.1")
    assertNotNull(candidate)
    assertEquals("simplex-ux-7.0.366-ux.7-arm64-v8a.apk", candidate.apkName)
    assertEquals("https://example.com/arm64.apk", candidate.downloadUrl)
    assertEquals(400L, candidate.sizeBytes)
  }

  @Test
  fun releaseWithoutMatchingAssetYieldsNull() {
    val json = """
      [
        {
          "tag_name": "v7.0.366-ux.7",
          "prerelease": false,
          "assets": [ { "name": "SHA256SUMS.txt", "browser_download_url": "https://example.com/SHA256SUMS.txt", "size": 100 } ]
        }
      ]
    """.trimIndent()
    assertNull(selectAppUpdateRelease(json, UpdateChannel.STABLE, "7.0.366-ux.1"))
  }

  @Test
  fun emptyOrMalformedReleasesJsonYieldsNull() {
    assertNull(selectAppUpdateRelease("[]", UpdateChannel.STABLE, "7.0.366-ux.1"))
    assertNull(selectAppUpdateRelease("not json at all", UpdateChannel.STABLE, "7.0.366-ux.1"))
    assertNull(selectAppUpdateRelease("{}", UpdateChannel.STABLE, "7.0.366-ux.1"))
  }

  @Test
  fun releasesUrlIsHardPinnedToTheForkRepo() {
    assertTrue(RELEASES_API_URL.startsWith("https://api.github.com/repos/delta-whiplash/simpleUx-chat/releases"))
    assertTrue("simplex-chat/simplex-chat" !in RELEASES_API_URL)
  }
}
