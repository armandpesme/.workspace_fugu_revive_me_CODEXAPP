package com.fuguteams.fugureviveme.state;

import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public final class KoConfigSnapshot {
    private final BiomeKoClassifier classifier;

    private KoConfigSnapshot(BiomeKoClassifier classifier) {
        this.classifier = classifier;
    }

    public static KoConfigSnapshot create(
            Collection<? extends String> temporaryBiomes,
            Collection<? extends String> prolongedBiomes,
            WarningSink warningSink
    ) {
        Objects.requireNonNull(warningSink, "warningSink");
        BiomeKoClassifier classifier = new BiomeKoClassifier(temporaryBiomes, prolongedBiomes);

        SortedSet<String> overlaps = new TreeSet<>(temporaryBiomes);
        overlaps.retainAll(prolongedBiomes);
        overlaps.forEach(warningSink::warn);

        return new KoConfigSnapshot(classifier);
    }

    public BiomeKoClassifier.Result classify(String biomeId) {
        return classifier.classify(biomeId);
    }

    @FunctionalInterface
    public interface WarningSink {
        void warn(String resourceLocation);
    }
}
