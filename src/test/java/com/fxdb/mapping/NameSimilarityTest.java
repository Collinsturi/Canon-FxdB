package com.fxdb.mapping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameSimilarityTest {

    @Test
    void exactMatchAfterNormalizationScoresOne() {
        assertThat(NameSimilarity.score("customer_id", "Customer Id")).isEqualTo(1.0);
        assertThat(NameSimilarity.score("custId", "cust-id")).isEqualTo(1.0);
    }

    @Test
    void similarNamesScoreHigherThanUnrelatedNames() {
        double similar = NameSimilarity.score("customer_name", "customer_full_name");
        double unrelated = NameSimilarity.score("customer_name", "shipping_zip");

        assertThat(similar).isGreaterThan(unrelated);
        assertThat(similar).isGreaterThan(0.6);
    }

    @Test
    void sharedPrefixBoostsScoreOverNoOverlapAtAll() {
        double sharedPrefix = NameSimilarity.score("cust_id", "cust_no");
        double noOverlap = NameSimilarity.score("cust_id", "wxyzpq");

        assertThat(sharedPrefix).isGreaterThan(noOverlap);
    }

    @Test
    void emptyStringsScoreZeroAgainstNonEmpty() {
        assertThat(NameSimilarity.score("", "name")).isEqualTo(0.0);
    }
}
