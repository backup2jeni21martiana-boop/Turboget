package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DownloadCategory
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
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
    assertEquals("TurboGet", appName)
  }

  @Test
  fun `test format bytes`() {
    assertEquals("500 B", FormatUtils.formatBytes(500))
    assertEquals("1.0 KB", FormatUtils.formatBytes(1024))
    assertEquals("10.0 MB", FormatUtils.formatBytes(10 * 1024 * 1024))
  }

  @Test
  fun `test category detection`() {
    assertEquals(DownloadCategory.VIDEO, DownloadCategory.fromFileName("movie.mp4"))
    assertEquals(DownloadCategory.ARCHIVE, DownloadCategory.fromFileName("backup.zip"))
    assertEquals(DownloadCategory.DOCUMENT, DownloadCategory.fromFileName("specs.pdf"))
    assertEquals(DownloadCategory.AUDIO, DownloadCategory.fromFileName("song.mp3"))
  }
}
