package com.fuguteams.fugureviveme.server;

import com.fuguteams.fugureviveme.state.KoAction;
import com.fuguteams.fugureviveme.state.ReviveActionType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory registry of in-progress revive actions.
 * <p>
 * Two kinds of entries live here:
 * <ul>
 *   <li>target-keyed entries: the player being revived; this is the canonical
 *       lookup used by tick handlers.</li>
 *   <li>helper-keyed entries: index of helpers currently helping a target; this
 *       is used to abort an action quickly when the helper moves, is hurt or
 *       changes slot.</li>
 * </ul>
 * The registry is never persisted: a server restart cleanly drops all
 * in-progress revives and the targets remain in their persistent
 * {@code KnockoutSavedData} record.
 */
public final class KnockoutActionRegistry {
    private final Map<UUID, KoAction> actionsByTarget = new HashMap<>();
    private final Map<UUID, Set<UUID>> helpersByHelper = new HashMap<>();

    public Optional<KoAction> get(UUID targetUuid) {
        Objects.requireNonNull(targetUuid, "targetUuid");
        return Optional.ofNullable(actionsByTarget.get(targetUuid));
    }

    public Optional<KoAction> findActiveForHelper(UUID helperUuid) {
        Objects.requireNonNull(helperUuid, "helperUuid");
        Set<UUID> targets = helpersByHelper.get(helperUuid);
        if (targets == null || targets.isEmpty()) {
            return Optional.empty();
        }
        for (UUID target : targets) {
            KoAction action = actionsByTarget.get(target);
            if (action != null && action.involvesHelper(helperUuid)) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    public Collection<KoAction> all() {
        return Collections.unmodifiableCollection(actionsByTarget.values());
    }

    public boolean start(KoAction action) {
        Objects.requireNonNull(action, "action");
        if (action.type() != ReviveActionType.ALLY_REVIVE && action.type() != ReviveActionType.SELF_REVIVE) {
            throw new IllegalArgumentException("Unsupported revive action type: " + action.type());
        }
        if (actionsByTarget.containsKey(action.targetUuid())) {
            return false;
        }
        actionsByTarget.put(action.targetUuid(), action);
        action.helperUuid().ifPresent(helper ->
                helpersByHelper.computeIfAbsent(helper, ignored -> new LinkedHashSet<>()).add(action.targetUuid()));
        return true;
    }

    public boolean cancelTarget(UUID targetUuid) {
        KoAction removed = actionsByTarget.remove(targetUuid);
        if (removed == null) {
            return false;
        }
        removed.helperUuid().ifPresent(helper -> {
            Set<UUID> targets = helpersByHelper.get(helper);
            if (targets != null) {
                targets.remove(targetUuid);
                if (targets.isEmpty()) {
                    helpersByHelper.remove(helper);
                }
            }
        });
        return true;
    }

    public boolean cancelHelper(UUID helperUuid) {
        Set<UUID> targets = helpersByHelper.remove(helperUuid);
        if (targets == null || targets.isEmpty()) {
            return false;
        }
        boolean anyRemoved = false;
        for (UUID target : targets) {
            if (actionsByTarget.remove(target) != null) {
                anyRemoved = true;
            }
        }
        return anyRemoved;
    }

    public int size() {
        return actionsByTarget.size();
    }
}
