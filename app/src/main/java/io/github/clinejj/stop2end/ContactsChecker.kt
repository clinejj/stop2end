package io.github.clinejj.stop2end

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.clinejj.stop2end.filter.IncomingMessage
import io.github.clinejj.stop2end.filter.PhoneNumbers

class ContactsChecker(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun isContact(message: IncomingMessage): Boolean {
        if (message.senderUri?.startsWith(ContactsContract.AUTHORITY_URI.toString()) == true) return true

        val name = message.senderName?.takeIf { PhoneNumbers.digitsOf(it) == null }
        val number = message.senderNumber ?: message.senderName?.takeIf { PhoneNumbers.digitsOf(it) != null }

        if (!hasPermission()) {
            // Messaging apps show unknown senders by number, so without contact access treat any
            // sender shown by name as a contact. This also spares RCS businesses shown by brand name.
            return name != null
        }
        return try {
            (number != null && hasContactWithNumber(number)) || (name != null && hasContactNamed(name))
        } catch (e: SecurityException) {
            Log.w(TAG, "Contacts lookup failed", e)
            name != null
        }
    }

    private fun hasContactWithNumber(number: String): Boolean {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return context.contentResolver
            .query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
            ?.use { it.moveToFirst() } ?: false
    }

    private fun hasContactNamed(name: String): Boolean =
        context.contentResolver
            .query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                "${ContactsContract.Contacts.DISPLAY_NAME} = ? COLLATE NOCASE",
                arrayOf(name),
                null,
            )
            ?.use { it.moveToFirst() } ?: false

    private companion object {
        const val TAG = "Stop2End"
    }
}
