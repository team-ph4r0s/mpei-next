package kekmech.ru.feature_bars_impl.data.attendance

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kekmech.ru.feature_bars_shared.analyzeAttendance
import kekmech.ru.lib_bars_parser.BarsAttendanceRecord
import kekmech.ru.lib_bars_parser.BarsAttendanceStatus
import kekmech.ru.res_strings.R.string as Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The notifications about the attendance as the system gets them: the channel, the texts, the grouping of the classes of
 * one day, the limit of the list and the way the notification opens the app. Runs on the JVM (Robolectric).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttendanceNotifierTest {

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val manager: NotificationManager get() = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notifier get() = AttendanceNotifier(app)

    private var nextId = 0

    @Before
    fun grantNotificationPermission() {
        // the permission dialog of Android 13+ is not part of this test
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun skip(discipline: String, date: String, pair: Int?, kind: String = "лекция") = BarsAttendanceRecord(
        id = (++nextId).toString(),
        date = date,
        pair = pair,
        timeStart = "09:20",
        timeEnd = "10:55",
        kind = kind,
        teacher = "Петров П.П.",
        discipline = discipline,
        assessmentType = "экзамен",
        group = "А-01-26",
        status = BarsAttendanceStatus.ABSENT,
        badges = emptyList(),
        raw = "",
    )

    private val posted: List<Notification> get() = shadowOf(manager).allNotifications

    private fun title(notification: Notification): String = notification.extras.getString(Notification.EXTRA_TITLE).orEmpty()

    private fun text(notification: Notification): String = notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()

    @Test
    fun aSkipIsANotificationWithTheChannelAndTheTextOfTheClass() {
        notifier.showSkips(listOf(skip("Физика", "28.10.26", pair = 3)))

        val channel = manager.getNotificationChannel("bars_attendance")
        assertNotNull(channel)
        assertEquals(app.getString(Strings.attendance_channel_name), channel.name.toString())

        val notification = posted.single()
        assertEquals("bars_attendance", notification.channelId)
        assertEquals(app.getString(Strings.attendance_notification_skip_title, "Физика"), title(notification))
        val expected = listOf(
            "28.10",
            app.getString(Strings.attendance_pair, 3),
            app.getString(Strings.attendance_kind_lecture),
        ).joinToString(" · ")
        assertEquals(expected, text(notification))
    }

    @Test
    fun twoClassesOfOneDayAreOneNotification() {
        notifier.showSkips(
            listOf(
                skip("Физика", "28.10.26", pair = 3),
                skip("Физика", "28.10.26", pair = 4),
                skip("Химия", "28.10.26", pair = 3),
            ),
        )

        assertEquals(2, posted.size)
        val physics = posted.single { title(it).contains("Физика") }
        assertTrue(text(physics).contains(app.getString(Strings.attendance_pairs, "3, 4")))
    }

    @Test
    fun theListIsLimitedAndTheRestIsToldInOneMore() {
        notifier.showSkips((1..7).map { skip("Предмет $it", "28.10.26", pair = 1) })

        // 5 skips and one "more"
        assertEquals(6, posted.size)
        assertEquals(1, posted.count { text(it) == app.getString(Strings.attendance_notification_more, 2) })
    }

    @Test
    fun nothingNewIsNoNotificationAndNoChannel() {
        notifier.showSkips(emptyList())
        notifier.showCritical(emptyList())

        assertTrue(posted.isEmpty())
        assertEquals(null, manager.getNotificationChannel("bars_attendance"))
    }

    @Test
    fun theRiskOfBeingRefusedNamesTheDisciplineAndTheNumbers() {
        val summary = analyzeAttendance(
            listOf(
                skip("Физика", "01.10.26", pair = 1),
                skip("Физика", "08.10.26", pair = 1),
                skip("Физика", "15.10.26", pair = 1),
            ) + listOf(22, 23, 24, 25).map { day ->
                skip("Физика", "$day.10.26", pair = 1).copy(status = BarsAttendanceStatus.PRESENT)
            },
        )
        val physics = summary.disciplines.single()

        notifier.showCritical(listOf(physics))

        val notification = posted.single()
        assertEquals(app.getString(Strings.attendance_notification_critical_title, "Физика"), title(notification))
        assertTrue(text(notification).isNotBlank())
    }

    @Test
    fun theNotificationOpensTheBarsTab() {
        notifier.showSkips(listOf(skip("Физика", "28.10.26", pair = 3)))

        val intent = shadowOf(posted.single().contentIntent).savedIntent
        assertEquals("mpeihub://bars", intent.dataString)
        assertEquals(app.packageName, intent.`package`)
    }

    @Test
    fun withoutThePermissionTheWorkDoesNotCrash() {
        // Whether the notification is shown is up to Android (Robolectric records every call): the app must only survive it
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(manager).setNotificationsEnabled(false)

        notifier.showSkips(listOf(skip("Физика", "28.10.26", pair = 3)))
        notifier.showCritical(emptyList())
    }

    @Test
    fun theSwitchesAreOffAtFirstAndKeepWhatTheStudentChose() {
        val prefs = AttendancePrefs(app)
        assertFalse(prefs.newSkipEnabled)
        assertFalse(prefs.criticalEnabled)
        assertFalse(prefs.anyEnabled)

        prefs.newSkipEnabled = true

        // a new object reads the same file: the choice survives the restart of the process
        val again = AttendancePrefs(app)
        assertTrue(again.newSkipEnabled)
        assertFalse(again.criticalEnabled)
        assertTrue(again.anyEnabled)
    }
}
