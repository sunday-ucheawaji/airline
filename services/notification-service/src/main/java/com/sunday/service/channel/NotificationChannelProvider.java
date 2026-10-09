package com.sunday.service.channel;

import com.sunday.service.model.NotificationChannel;

/**
 * One implementation per delivery channel (Email/SMS). NotificationDispatchService never branches on
 * NotificationChannel itself — it resolves the right provider through NotificationChannelRegistry and
 * calls this interface, so adding a channel (push, WhatsApp, a fallback email vendor) means one new
 * class, zero changes to the dispatch logic. Mirrors payment-service's PaymentGatewayProvider exactly.
 */
public interface NotificationChannelProvider {

    NotificationChannel channelType();

    /** False means the channel is deliberately switched off (e.g. twilio.enabled=false) — the caller
     *  should skip the attempt entirely, the same as it would for a blank recipient, rather than
     *  recording an ambiguous "sent" or "failed" outcome for a send that was never really attempted. */
    default boolean isAvailable() {
        return true;
    }

    /** Sends the already-rendered content. Returns the provider's own message id/reference when it
     *  has one (Twilio's SID), or null when the channel has no such concept (plain SMTP). Throws on
     *  any delivery failure — the caller (NotificationDispatchService) decides what happens next. */
    String send(String recipient, String subject, String body) throws Exception;
}
