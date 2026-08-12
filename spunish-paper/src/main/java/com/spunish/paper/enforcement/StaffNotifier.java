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
 * Announces a punishment to {@code spunish.notify} holders and, unless
 * disabled, to the whole server; a revocation goes to staff only. The public
 * announcement is disabled by leaving {@code punish.public-announcement}
 * blank in messages.yml — there is no separate toggle.
 * Network-wide notification works by every instance separately notifying its
 * own local audience — never by one instance reaching across to message
 * another's players directly.
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
