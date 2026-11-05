package dev.kosh.financetracker.data.sms

/**
 * Stable identity for an SMS derived from its content, not the OS-assigned row id.
 * Historical import (ContentResolver query) and live capture (BroadcastReceiver) see
 * the same physical message through different APIs with different id schemes — using
 * a content-derived key means both paths agree on identity, so the unique index on
 * Transaction.sourceMessageId actually prevents double-importing the same SMS.
 *
 * Timestamp is rounded to the nearest second since the live-broadcast timestamp and
 * the content-provider-stored timestamp for the same message aren't always identical
 * to the millisecond.
 */
fun smsSourceMessageId(sender: String, timestampEpochMillis: Long, body: String): String {
    val roundedSeconds = timestampEpochMillis / 1000
    return "sms:$sender:$roundedSeconds:${body.hashCode()}"
}
