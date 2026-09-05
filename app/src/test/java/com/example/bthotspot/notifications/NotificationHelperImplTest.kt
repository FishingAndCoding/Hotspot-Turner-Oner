package com.example.bthotspot.notifications

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationHelperImplTest {

    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        notificationManager = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
    }

    @Test
    fun `constructor creates exactly two notification channels`() {
        NotificationHelperImpl(ApplicationProvider.getApplicationContext())

        assertEquals(2, notificationManager.notificationChannels.size)
    }

    @Test
    fun `channels use the expected importance`() {
        NotificationHelperImpl(ApplicationProvider.getApplicationContext())

        assertEquals(
            NotificationManager.IMPORTANCE_LOW,
            notificationManager.getNotificationChannel(NotificationHelperImpl.CHANNEL_SERVICE).importance,
        )
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            notificationManager.getNotificationChannel(NotificationHelperImpl.CHANNEL_ERRORS).importance,
        )
    }

    @Test
    fun `postErrorNotification posts title body and error channel`() {
        val helper = NotificationHelperImpl(ApplicationProvider.getApplicationContext())

        helper.postErrorNotification("Test Title", "Test Body")

        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals(NotificationHelperImpl.CHANNEL_ERRORS, notification.channelId)
        assertEquals("Test Title", notification.extras.getString("android.title"))
        assertEquals("Test Body", notification.extras.getString("android.text"))
    }

    @Test
    fun `postErrorNotification uses distinct ids for consecutive calls`() {
        val helper = NotificationHelperImpl(ApplicationProvider.getApplicationContext())

        helper.postErrorNotification("Error 1", "body 1")
        Thread.sleep(2)
        helper.postErrorNotification("Error 2", "body 2")

        assertEquals(2, shadowOf(notificationManager).allNotifications.size)
    }
}
