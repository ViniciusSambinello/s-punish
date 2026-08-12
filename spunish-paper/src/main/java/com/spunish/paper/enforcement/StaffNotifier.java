package com.spunish.paper.enforcement;

import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.message.MessageService;
import com.spunish.common.message.PunishmentPlaceholders;
import com.spunish.common.platform.AudienceResolver;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

/**
 * Announces a punishment or revocation to {@code spunish.notify} holders and,
 * unless turned off, the whole server (task 7.12). "Off" is the same
 * empty-message convention as everywhere else — {@code punish.public-announcement}
 * left blank in messages.yml — so {@link #send} disabling itself on an
 * empty render is the only toggle needed; there is no separate boolean flag.
 * Called both right after a local apply/revoke and whenever a sync event for
 * one reaches this instance from elsewhere — the network-wide reach comes
 * from every instance doing this for its own local audience, not from one
 * instance messaging another's.
 */
public final class StaffNotifier {

    private final AudienceResolver audienceResolver;
    private final MessageService messageService;
    private final SystemClock clock;

    public StaffNotifier(AudienceResolver audienceResolver, MessageService messageService, SystemClock clock) {
        this.audienceResolver = audienceResolver;
        this.messageService = messageService;
        this.clock = clock;
    }

    public void announcePunishment(Punishment punishment) {
        announce(punishment, "punish.staff-announcement", "punish.public-announcement");
    }

    public void announceRevocation(Punishment punishment) {
        announce(punishment, "unpunish.staff-announcement", null);
    }

    private void announce(Punishment punishment, String staffKey, String publicKey) {
        Map<String, String> placeholders = PunishmentPlaceholders.build(punishment, messageService, clock.now());

        send(audienceResolver.onlineWithPermission("spunish.notify"), staffKey, placeholders);
        if (publicKey != null) {
            send(List.of(audienceResolver.broadcastAudience()), publicKey, placeholders);
        }
    }

    private void send(Iterable<Audience> audiences, String key, Map<String, String> placeholders) {
        List<Component> lines = messageService.render(key, placeholders);
        if (lines.isEmpty()) {
            return;
        }
        for (Audience audience : audiences) {
            for (Component line : lines) {
                audience.sendMessage(line);
            }
        }
    }
}
