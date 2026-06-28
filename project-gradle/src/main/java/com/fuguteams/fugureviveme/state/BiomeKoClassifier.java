package com.fuguteams.fugureviveme.state;

import com.fuguteams.fugureviveme.config.ResourceIdValidator;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BiomeKoClassifier {
    private final Set<String> temporaryBiomes;
    private final Set<String> prolongedBiomes;

    public BiomeKoClassifier(
            Collection<? extends String> temporaryBiomes,
            Collection<? extends String> prolongedBiomes
    ) {
        this.temporaryBiomes = copyAndValidate(temporaryBiomes);
        this.prolongedBiomes = copyAndValidate(prolongedBiomes);
    }

    public Result classify(String biomeId) {
        if (!ResourceIdValidator.isValid(biomeId)) {
            throw new IllegalArgumentException("Invalid biome resource location: " + biomeId);
        }

        boolean temporary = temporaryBiomes.contains(biomeId);
        boolean prolonged = prolongedBiomes.contains(biomeId);
        if (prolonged) {
            return new Result(KoType.PROLONGED, temporary);
        }
        if (temporary) {
            return new Result(KoType.TEMPORARY, false);
        }
        return new Result(KoType.NONE, false);
    }

    private static Set<String> copyAndValidate(Collection<? extends String> biomeIds) {
        if (biomeIds == null) {
            throw new IllegalArgumentException("Biome collection cannot be null");
        }

        LinkedHashSet<String> validated = new LinkedHashSet<>();
        for (String biomeId : biomeIds) {
            if (!ResourceIdValidator.isValid(biomeId)) {
                throw new IllegalArgumentException("Invalid biome resource location: " + biomeId);
            }
            validated.add(biomeId);
        }
        return Set.copyOf(validated);
    }

    public enum KoType {
        NONE,
        TEMPORARY,
        PROLONGED
    }

    public record Result(KoType type, boolean overlap) {
    }
}
