package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MovieBox", appName)
  }

  @Test
  fun `fetch play streams for dhamaal 4 returns valid streams`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val result = com.example.data.api.MovieBoxApiClient.fetchPlayStreams(
      context = context,
      subjectId = "3383785055142172048",
      detailPath = "dhamaal-4-hindi-sqAJAsPLX14",
      isShort = false,
      season = 0,
      episode = 0
    )
    assertTrue("Should have resource or valid HTTP response", result.httpCode == 200)
    assertTrue("Should contain streams", result.streams.isNotEmpty())
    assertTrue("hasResource should be true", result.hasResource)
  }

  @Test
  fun `verify only Hot Short TV row qualifies as shorts section`() {
    val hotShortSection = com.example.data.model.CategorySection(
      id = "1",
      title = "🔥Hot Short TV",
      type = "CUSTOM",
      items = emptyList()
    )
    val fightZoneShorts = com.example.data.model.CategorySection(
      id = "2",
      title = "🔥Fight Zone Shorts",
      type = "CUSTOM",
      items = emptyList()
    )
    val viralSportsShorts = com.example.data.model.CategorySection(
      id = "3",
      title = "Viral Sports Shorts",
      type = "CUSTOM",
      items = emptyList()
    )
    val regularSection = com.example.data.model.CategorySection(
      id = "4",
      title = "Popular Series",
      type = "CUSTOM",
      items = emptyList()
    )

    assertTrue("🔥Hot Short TV must be shorts section", hotShortSection.isHotShortTvSection)
    assertTrue("🔥Hot Short TV isShortsSection must be true", hotShortSection.isShortsSection)
    org.junit.Assert.assertFalse("Fight Zone Shorts must NOT be shorts section", fightZoneShorts.isHotShortTvSection)
    org.junit.Assert.assertFalse("Viral Sports Shorts must NOT be shorts section", viralSportsShorts.isHotShortTvSection)
    org.junit.Assert.assertFalse("Popular Series must NOT be shorts section", regularSection.isHotShortTvSection)
  }

  @Test
  fun testDownloadHelpers() {
    assertEquals("0 KB/s", com.example.data.download.MovieDownloadManager.formatSpeed(0))
    assertEquals("500 B/s", com.example.data.download.MovieDownloadManager.formatSpeed(500))
    assertEquals("1024 KB/s", com.example.data.download.MovieDownloadManager.formatSpeed(1024 * 1024))
    
    assertEquals("0 MB", com.example.data.download.MovieDownloadManager.formatBytes(0))
    assertEquals("500 B", com.example.data.download.MovieDownloadManager.formatBytes(500))
    assertEquals("1024 KB", com.example.data.download.MovieDownloadManager.formatBytes(1024 * 1024))
    assertEquals("1.0 MB", com.example.data.download.MovieDownloadManager.formatBytes(1048576))
  }
}
