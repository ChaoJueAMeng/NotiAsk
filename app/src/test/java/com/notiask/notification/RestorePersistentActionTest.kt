package com.notiask.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestorePersistentActionTest {
    @Test
    fun restoreActionIsStableExplicitPackageAction() {
        assertEquals("com.notiask.action.RESTORE_PERSISTENT", NotificationController.ACTION_RESTORE_PERSISTENT)
        assertTrue(NotificationController.ACTION_RESTORE_PERSISTENT.startsWith("com.notiask.action."))
    }

    @Test
    fun foregroundNotificationIdIsStable() {
        assertEquals(1001, NotificationController.FOREGROUND_ID)
    }
}
