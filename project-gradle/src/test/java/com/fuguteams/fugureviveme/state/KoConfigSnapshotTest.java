package com.fuguteams.fugureviveme.state;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KoConfigSnapshotTest {
    @Test
    void warnsOncePerOverlapAndKeepsProlongedPriority() {
        List<String> warnings = new ArrayList<>();

        KoConfigSnapshot snapshot = KoConfigSnapshot.create(
                List.of("fugubiomes:shared_b", "fugubiomes:temporary", "fugubiomes:shared_a"),
                List.of("fugubiomes:shared_a", "fugubiomes:prolonged", "fugubiomes:shared_b"),
                warnings::add
        );

        assertEquals(List.of("fugubiomes:shared_a", "fugubiomes:shared_b"), warnings);
        assertEquals(BiomeKoClassifier.KoType.PROLONGED,
                snapshot.classify("fugubiomes:shared_a").type());
        assertEquals(BiomeKoClassifier.KoType.TEMPORARY,
                snapshot.classify("fugubiomes:temporary").type());
    }
}
