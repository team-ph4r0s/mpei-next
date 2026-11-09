package kekmech.ru.feature_bars_impl.data.attendance

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kekmech.ru.ext_android.AppLocales
import kekmech.ru.feature_bars_impl.domain.AttendanceKind
import kekmech.ru.feature_bars_shared.AttendanceRules
import kekmech.ru.feature_bars_shared.DisciplineAttendance
import kekmech.ru.lib_bars_parser.BarsAttendanceRecord
import timber.log.Timber
import kotlin.math.roundToInt
import kekmech.ru.res_icons.R.drawable as ResIcons
import kekmech.ru.res_strings.R.string as Strings

/** The two notifications about the attendance: a new skip and the risk of being refused. */
internal class AttendanceNotifier(
    private val context: Context,
) {

    // the language chosen in the app, not the one of the system
    private val localized get() = AppLocales.localized(context)

    /**
     * One notification for a discipline and a day (a lecture of two classes is one skip for the student, not two), at
     * most [MAX_SKIPS] of them; the rest is told in one more.
     */
    fun showSkips(skips: List<BarsAttendanceRecord>) {
        if (skips.isEmpty()) return
        createChannel()
        val groups = skips.groupBy { it.discipline to it.date }.values.toList()
        groups.take(MAX_SKIPS).forEach { group -> notify(SKIP_TAG, skipKey(group.first()), skipNotification(group)) }
        val rest = groups.size - MAX_SKIPS
        if (rest > 0) {
            val text = localized.getString(Strings.attendance_notification_more, rest)
            notify(SKIP_TAG, MORE_ID, builder(localized.getString(Strings.attendance_channel_name), text).build())
        }
    }

    /** One for every discipline that has become critical. */
    fun showCritical(disciplines: List<DisciplineAttendance>) {
        if (disciplines.isEmpty()) return
        createChannel()
        for (discipline in disciplines) {
            val text = if (discipline.currentSkipStreak >= AttendanceRules.CRITICAL_STREAK) {
                localized.getString(Strings.attendance_notification_critical_streak, discipline.currentSkipStreak)
            } else {
                localized.getString(
                    Strings.attendance_notification_critical_share,
                    discipline.absent,
                    discipline.total,
                    (discipline.skipShare * PERCENT).roundToInt(),
                )
            }
            val notification = builder(
                title = localized.getString(Strings.attendance_notification_critical_title, discipline.name),
                text = text,
            ).setPriority(NotificationCompat.PRIORITY_HIGH).build()
            notify(CRITICAL_TAG, discipline.name.hashCode(), notification)
        }
    }

    private fun skipNotification(group: List<BarsAttendanceRecord>): android.app.Notification {
        val first = group.first()
        val pairs = group.mapNotNull { it.pair }.distinct().sorted()
        val pairText = when {
            pairs.isEmpty() -> null
            pairs.size == 1 -> localized.getString(Strings.attendance_pair, pairs.first())
            else -> localized.getString(Strings.attendance_pairs, pairs.joinToString(", "))
        }
        val text = listOfNotNull(
            first.date.substringBeforeLast('.'),
            pairText,
            localized.getString(AttendanceKind.of(first.kind).label),
        ).joinToString(" · ")
        return builder(
            title = localized.getString(Strings.attendance_notification_skip_title, first.discipline),
            text = text,
        ).build()
    }

    private fun builder(title: String, text: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ResIcons.ic_notification_schedule_24)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openBarsIntent())

    private fun notify(tag: String, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(tag, id, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS is not granted: the notification is silently dropped
            Timber.w(e, "AttendanceNotifier: permission denied")
        }
    }

    private fun skipKey(record: BarsAttendanceRecord): Int = (record.discipline + record.date).hashCode()

    /** Opens the BARS tab through the app deeplink */
    private fun openBarsIntent(): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BARS_DEEPLINK))
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            BARS_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                localized.getString(Strings.attendance_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = localized.getString(Strings.attendance_channel_description)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private companion object {
        const val CHANNEL_ID = "bars_attendance"
        const val SKIP_TAG = "bars_attendance_skip"
        const val CRITICAL_TAG = "bars_attendance_critical"
        const val MORE_ID = 1
        const val MAX_SKIPS = 5
        const val PERCENT = 100
        const val BARS_DEEPLINK = "mpeihub://bars"
        const val BARS_REQUEST_CODE = 4001
    }
}
