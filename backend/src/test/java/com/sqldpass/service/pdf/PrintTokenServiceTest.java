package com.sqldpass.service.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PrintTokenServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";

    private final PrintTokenService service = new PrintTokenService(SECRET);

    @Test
    void issuedTokenValidatesAgainstSameMockExamId() {
        String token = service.issue(123L);

        assertThat(service.validate(token, 123L)).isTrue();
    }

    @Test
    void tokenForOneMockExamRejectedForAnother() {
        String token = service.issue(123L);

        assertThat(service.validate(token, 456L)).isFalse();
    }

    @Test
    void nullOrEmptyTokenRejected() {
        assertThat(service.validate(null, 1L)).isFalse();
        assertThat(service.validate("", 1L)).isFalse();
    }

    @Test
    void tokenSignedByDifferentSecretRejected() {
        String foreign = new PrintTokenService("other-secret-other-secret-other-secret-1234")
                .issue(123L);

        assertThat(service.validate(foreign, 123L)).isFalse();
    }
}
