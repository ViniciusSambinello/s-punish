package com.spunish.common.service;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.PunishmentCategory;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.platform.PermissionChecker;
import com.spunish.common.platform.ServerIdentity;
import com.spunish.common.storage.PunishmentRepository;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class PunishmentRevokeService {

    private final PunishmentRepository repository;
    private final SystemClock clock;
    private final ServerIdentity serverIdentity;
    private final PermissionChecker permissionChecker;

    public PunishmentRevokeService(
            PunishmentRepository repository, SystemClock clock, ServerIdentity serverIdentity, PermissionChecker permissionChecker) {
        this.repository = repository;
        this.clock = clock;
        this.serverIdentity = serverIdentity;
        this.permissionChecker = permissionChecker;
    }

    public CompletableFuture<RevokeResult> revoke(
            UUID targetUuid, PunishmentCategory category, Actor revoker, String revokeReason) {
        if (!permissionChecker.hasPermission(revoker, "spunish.unpunish." + category.permissionSuffix())) {
            return CompletableFuture.completedFuture(new RevokeResult.PermissionDenied());
        }

        return repository.findActive(targetUuid, category)
                .thenCompose(existing -> {
                    if (existing.isEmpty()) {
                        return CompletableFuture.completedFuture((RevokeResult) new RevokeResult.NoActivePunishment());
                    }
                    long id = existing.get().id();
                    Instant revokedAt = clock.now();
                    return repository.revoke(id, revoker, revokedAt, revokeReason, serverIdentity.id()).thenCompose(applied -> {
                        if (!applied) {
                            // A revoke that loses a race against a concurrent revoke is treated
                            // as no active punishment, not an error.
                            return CompletableFuture.completedFuture((RevokeResult) new RevokeResult.NoActivePunishment());
                        }
                        return repository.findById(id)
                                .thenApply(reloaded -> (RevokeResult) new RevokeResult.Success(reloaded.orElseThrow()));
                    });
                })
                .exceptionally(ex -> new RevokeResult.InternalError(unwrap(ex)));
    }

    private static Throwable unwrap(Throwable ex) {
        return ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
    }
}
