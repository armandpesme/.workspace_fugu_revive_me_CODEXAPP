package com.fuguteams.fugureviveme.state;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeKoClassifierTest {
    @Test
    void prolongedKoHasPriorityAndReportsAnOverlap() {
        BiomeKoClassifier classifier = new BiomeKoClassifier(
                List.of("fugubiomes:shared", "fugubiomes:temporary"),
                List.of("fugubiomes:shared", "fugubiomes:prolonged")
        );

        BiomeKoClassifier.Result result = classifier.classify("fugubiomes:shared");

        assertEquals(BiomeKoClassifier.KoType.PROLONGED, result.type());
        assertTrue(result.overlap());
    }

    @Test
    void supportsLargeConfiguredBiomeLists() {
        List<String> temporary = IntStream.range(0, 5_000)
                .mapToObj(index -> "fugubiomes:temporary_" + index)
                .toList();
        List<String> prolonged = IntStream.range(0, 5_000)
                .mapToObj(index -> "fugubiomes:prolonged_" + index)
                .toList();
        BiomeKoClassifier classifier = new BiomeKoClassifier(temporary, prolonged);

        assertEquals(BiomeKoClassifier.KoType.TEMPORARY,
                classifier.classify("fugubiomes:temporary_4999").type());
        assertEquals(BiomeKoClassifier.KoType.PROLONGED,
                classifier.classify("fugubiomes:prolonged_4999").type());
        assertEquals(BiomeKoClassifier.KoType.NONE,
                classifier.classify("fugubiomes:unconfigured").type());
        assertFalse(classifier.classify("fugubiomes:temporary_4999").overlap());
    }

    @Test
    void rejectsInvalidConfiguredBiomeIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new BiomeKoClassifier(List.of("invalid id"), List.of()));
    }
}
