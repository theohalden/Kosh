package dev.kosh.financetracker.data.sms

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Reads the entire SMS inbox, oldest history included — no cap. */
    fun readInboxMessages(): List<SmsMessageEntry> {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        val messages = mutableListOf<SmsMessageEntry>()

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                messages += SmsMessageEntry(
                    id = cursor.getString(idIndex) ?: continue,
                    sender = cursor.getString(addressIndex).orEmpty(),
                    body = cursor.getString(bodyIndex).orEmpty(),
                    timestampEpochMillis = cursor.getLong(dateIndex),
                )
            }
        }

        return messages
    }
}
