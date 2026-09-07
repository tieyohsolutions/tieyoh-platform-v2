package com.tieyoh.mobile

import android.util.Log

// Stand-in for a real SMS delivery service (Twilio or similar). There's no
// such service connected yet. Today, an invite that goes out by phone number
// is a *text message* -- conceptually Tieyoh itself sending an SMS to the
// recipient -- as opposed to the "Invite Friends" share flow, which just
// hands a link to the OS share sheet and lets the person pick where it goes
// (Messages, WhatsApp, email, whatever). A real "send text message" action
// can't be demonstrated on an emulator (no working SMS app, no real carrier),
// so until a real provider is wired up, sending a text message means logging
// its exact body and destination here -- visible in Android Studio's Logcat
// panel while the emulator is running. Filter on the tag below to find it.
//
// This is deliberately the ONLY place that builds/emits a text invite's
// final message text, specifically so that wiring up a real provider later
// is a one-function change: swap the body of sendTextMessage() for a real
// API call, and every call site below keeps working unchanged.
object InviteMessageSender {
    private const val TAG = "TieyohInviteMessage"

    fun buildMessage(inviteUrl: String): String = "Join me on Tieyoh! $inviteUrl"

    fun sendTextMessage(displayName: String, phone: String, inviteUrl: String) {
        Log.i(TAG, "========== SEND TEXT MESSAGE (no SMS service connected yet) ==========")
        Log.i(TAG, "To: $displayName <$phone>")
        Log.i(TAG, "Message: ${buildMessage(inviteUrl)}")
        Log.i(TAG, "========================================================================")
    }
}
