package com.fxdb.mapping;

import com.fxdb.core.model.Column;
import com.fxdb.core.model.TableSchema;
import com.fxdb.mapping.model.ColumnMapping;
import com.fxdb.mapping.model.ColumnTransform;
import com.fxdb.mapping.model.TableMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Auto-matches source columns to target columns by combined name/type score, greedily
 * assigning the highest-scoring unclaimed pairs first. Results are meant to be reviewed and
 * overridden in the mapping grid (Phase 4), not applied blindly.
 */
public final class MappingEngine {

    private static final double NAME_WEIGHT = 0.6;
    private static final double TYPE_WEIGHT = 0.4;

    private MappingEngine() {
    }

    /**
     * @param target null when the target table doesn't exist yet and will be created with the
     *               same shape as the source (every source column maps 1:1, confidence 1.0).
     */
    public static TableMapping autoMatch(TableSchema source, String targetTableName, TableSchema target) {
        if (target == null) {
            return matchAgainstNewTable(source, targetTableName);
        }
        return matchAgainstExistingTable(source, targetTableName, target);
    }

    private static TableMapping matchAgainstNewTable(TableSchema source, String targetTableName) {
        List<ColumnMapping> columns = source.columns().stream()
                .map(c -> new ColumnMapping(c.name(), c.name(), c.type(), ColumnTransform.NONE, 1.0, true))
                .toList();
        return new TableMapping(source.tableName(), targetTableName, columns, true);
    }

    private static TableMapping matchAgainstExistingTable(TableSchema source, String targetTableName, TableSchema target) {
        record Candidate(Column source, Column target, double score) {
        }

        List<Candidate> candidates = new ArrayList<>();
        for (Column src : source.columns()) {
            for (Column tgt : target.columns()) {
                double score = NAME_WEIGHT * NameSimilarity.score(src.name(), tgt.name())
                        + TYPE_WEIGHT * TypeCompatibility.score(src.type(), tgt.type());
                candidates.add(new Candidate(src, tgt, score));
            }
        }
        candidates.sort((a, b) -> Double.compare(b.score(), a.score()));

        Set<String> claimedSource = new HashSet<>();
        Set<String> claimedTarget = new HashSet<>();
        java.util.Map<String, Candidate> bestMatchBySource = new java.util.LinkedHashMap<>();

        for (Candidate candidate : candidates) {
            String srcName = candidate.source().name();
            String tgtName = candidate.target().name();
            if (claimedSource.contains(srcName) || claimedTarget.contains(tgtName)) {
                continue;
            }
            claimedSource.add(srcName);
            claimedTarget.add(tgtName);
            bestMatchBySource.put(srcName, candidate);
        }

        List<ColumnMapping> columns = new ArrayList<>();
        for (Column src : source.columns()) {
            Candidate match = bestMatchBySource.get(src.name());
            if (match != null) {
                columns.add(new ColumnMapping(src.name(), match.target().name(), match.target().type(),
                        ColumnTransform.NONE, match.score(), true));
            } else {
                // No unclaimed target column was compatible enough; left for manual mapping.
                columns.add(new ColumnMapping(src.name(), null, src.type(), ColumnTransform.NONE, 0.0, false));
            }
        }
        return new TableMapping(source.tableName(), targetTableName, columns, true);
    }
}
