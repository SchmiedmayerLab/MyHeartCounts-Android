//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.requireDependency

/**
 * Receives the remote nudges the backend's `sendNudges` function pushes.
 *
 * The study's own reminders are local notifications owned by Grove's scheduler; this channel carries
 * only the server-decided nudges, so the two never collide.
 */
class MHCMessagingService : FirebaseMessagingService() {

    private val logger by groveLogger(tag = "MHCFirebase")

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // The token outlives any one process, so it goes onto the account document rather than being
        // kept here; MHCPushTokenSynchronizer owns reconciling it with what is already stored.
        //
        // Deliberately not scoped to this service: the system may destroy it as soon as this
        // returns, which would cancel the write part-way. A write lost to the process dying is
        // reconciled by `MHCPushTokenSynchronizer.synchronize()` on the next launch.
        requireDependency<Concurrency>().ioCoroutineScope().launch {
            requireDependency<MHCPushTokenSynchronizer>().onTokenRefreshed(token = token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Nudges are sent as notification messages, which the system tray displays on its own while
        // the app is backgrounded. Nothing to do here beyond noting that one arrived.
        logger.i { "Received a remote nudge (${message.messageId ?: "no id"})." }
    }
}
