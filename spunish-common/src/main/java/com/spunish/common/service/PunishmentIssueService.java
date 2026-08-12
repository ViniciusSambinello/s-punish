package com.spunish.common.service;

import com.spunish.common.domain.Actor;
import com.spunish.common.domain.PlayerActor;
import com.spunish.common.domain.Punishment;
import com.spunish.common.domain.PunishmentTarget;
import com.spunish.common.domain.SystemActor;
import com.spunish.common.domain.SystemClock;
import com.spunish.common.duration.PunishmentDuration;
import com.spunish.common.platform.PermissionChecker;
import com.spunish.common.platform.ServerIdentity;
import com.spunish.common.storage.InsertPunishmentCommand;
import com.spunish.common.storage.PunishmentRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * Never kicks or mutes anyone itself — enforcement side effects are the
 * caller's job, triggered only on {@link IssueResult.Success}, so a
 * persistence failure can never leave a target half-punished.
 */
public final class PunishmentIssueService {

    private final PunishmentRepository repository;
    private final SystemClock clock;
    private final ServerIdentity serverIdentity;
    private final PermissionChecker permissionChecker;
    private final Supplier<Map<String, String>> durationLimitsSupplier;
    private final DurationLimitPolicy durationLimitPolicy = new DurationLimitPolicy();

    public PunishmentIssueService(
            PunishmentRepository repository,
            SystemClock clock,
            ServerIdentity serverIdentity,
            PermissionChecker permissionChecker,
            Supplier<Map<String, String>> durationLimitsSupplier) {
        this.repository = repository;
        this.clock = clock;
        this.serverIdentity = serverIdentity;
        this.permissionChecker = permissionChecker;
        this.durationLimitsSupplier = durationLimitsSupplier;
    }

    public CompletableFuture<IssueResult> issue(IssueCommand command) {
        Optional<IssueResult.Refused> synchronousRefusal = checkSynchronousRules(command);
        if (synchronousRefusal.isPresent()) {
            return CompletableFuture.completedFuture(synchronousRefusal.get());
        }

        return repository.findActive(command.target().uuid(), command.category())
                .thenCompose(existing -> {
                    if (existing.isPresent()) {
                        return handleExisting(command, existing.get());
                    }
                    return applyNew(command);
                })
                .exceptionally(ex -> new IssueResult.InternalError(unwrap(ex)));
    }

    private Optional<IssueResult.Refused> checkSynchronousRules(IssueCommand command) {
        Actor actor = command.actor();

        if (actor instanceof PlayerActor player
                && player.uuid().equals(command.target().uuid())
                && !permissionChecker.hasPermission(actor, "spunish.punish.self")) {
            return Optional.of(new IssueResult.SelfPunishDenied());
        }

        if (!permissionChecker.hasPermission(actor, "spunish.punish." + command.category().permissionSuffix())) {
            return Optional.of(new IssueResult.CategoryPermissionDenied());
        }

        if (command.reason().isRestricted() && !permissionChecker.hasPermission(actor, command.reason().permission())) {
            return Optional.of(new IssueResult.ReasonPermissionDenied());
        }

        boolean hasOverride = permissionChecker.hasPermission(actor, "spunish.punish.override");

        if (!hasOverride
                && permissionChecker.targetHasPermission(command.target().uuid(), "spunish.exempt." + command.category().permissionSuffix())) {
            return Optional.of(new IssueResult.TargetExempt());
        }

        PunishmentDuration requested = command.effectiveDuration();
        Optional<PunishmentDuration> limit = durationLimitPolicy.effectiveLimit(
                durationLimitsSupplier.get(), permission -> permissionChecker.hasPermission(actor, permission));
        if (limit.isPresent() && durationLimitPolicy.exceeds(requested, limit.get())) {
            return Optional.of(new IssueResult.DurationLimitExceeded(limit.get()));
        }

        return Optional.empty();
    }

    private CompletableFuture<IssueResult> handleExisting(IssueCommand command, Punishment existing) {
        boolean hasOverride = permissionChecker.hasPermission(command.actor(), "spunish.punish.override");
        if (!hasOverride) {
            return CompletableFuture.completedFuture(new IssueResult.AlreadyActive(existing));
        }
        InsertPunishmentCommand insert = buildInsertCommand(command);
        Instant now = clock.now();
        return repository
                .insertWithOverride(insert, existing.id(), SystemActor.INSTANCE, now, "Superseded by a new punishment")
                .thenApply(result -> (IssueResult) new IssueResult.Success(result.applied(), result.revoked()));
    }

    private CompletableFuture<IssueResult> applyNew(IssueCommand command) {
        return repository.insert(buildInsertCommand(command))
                .thenApply(applied -> new IssueResult.Success(applied, null));
    }

    private InsertPunishmentCommand buildInsertCommand(IssueCommand command) {
        Instant appliedAt = clock.now();
        PunishmentTarget target = command.target();
        return new InsertPunishmentCommand(
                command.category(),
                target.uuid(),
                target.name(),
                command.actor(),
                command.reason().id(),
                command.reason().displayName(),
                appliedAt,
                command.effectiveDuration().expiresAt(appliedAt).orElse(null),
                serverIdentity.id());
    }

    private static Throwable unwrap(Throwable ex) {
        return ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
    }
}
