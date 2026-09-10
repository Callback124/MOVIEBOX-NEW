package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.MovieBoxApiClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MovieBoxFeedRobolectricTest {

    @Test
    fun testBundledFeedLoadsSuccessfully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val feed = MovieBoxApiClient.loadBundledHomeFeed(context)

        assertNotNull(feed)
        assertFalse("Hero banners should not be empty", feed.heroBanners.isEmpty())
        assertFalse("Sections should not be empty", feed.sections.isEmpty())
        assertTrue("Platform hubs should exist", feed.platforms.isNotEmpty())
    }
}
