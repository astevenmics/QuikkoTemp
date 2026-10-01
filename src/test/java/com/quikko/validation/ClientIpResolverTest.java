package com.quikko.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void untrustedRemoteAddr_ignoresForwardedHeader() {
        // No trusted proxies configured at all -- the default, and the
        // scenario item 1 fixes: a client's own spoofed XFF must never be
        // honored.
        String resolved = ClientIpResolver.resolve("9.9.9.9", "203.0.113.5", List.of());
        assertThat(resolved).isEqualTo("203.0.113.5");
    }

    @Test
    void untrustedRemoteAddr_ignoresForwardedHeaderEvenIfSomeProxiesAreConfigured() {
        String resolved = ClientIpResolver.resolve("9.9.9.9", "203.0.113.5", List.of("10.0.0.1"));
        assertThat(resolved).isEqualTo("203.0.113.5");
    }

    @Test
    void trustedProxyExactIp_honorsForwardedHeader() {
        String resolved = ClientIpResolver.resolve("9.9.9.9", "10.0.0.1", List.of("10.0.0.1"));
        assertThat(resolved).isEqualTo("9.9.9.9");
    }

    @Test
    void trustedProxyCidr_honorsForwardedHeaderForAnyAddressInRange() {
        String resolved = ClientIpResolver.resolve("9.9.9.9", "10.0.0.42", List.of("10.0.0.0/24"));
        assertThat(resolved).isEqualTo("9.9.9.9");
    }

    @Test
    void trustedProxyCidr_outsideRangeFallsBackToRemoteAddr() {
        String resolved = ClientIpResolver.resolve("9.9.9.9", "10.0.1.42", List.of("10.0.0.0/24"));
        assertThat(resolved).isEqualTo("10.0.1.42");
    }

    @Test
    void malformedForwardedValue_fallsBackEvenWhenProxyIsTrusted() {
        String resolved = ClientIpResolver.resolve("<script>alert(1)</script>", "10.0.0.1", List.of("10.0.0.1"));
        assertThat(resolved).isEqualTo("10.0.0.1");
    }

    @Test
    void multiValueHeader_takesTheFirstHopWhenTrusted() {
        String resolved = ClientIpResolver.resolve("9.9.9.9, 10.0.0.1", "10.0.0.1", List.of("10.0.0.1"));
        assertThat(resolved).isEqualTo("9.9.9.9");
    }

    @Test
    void noForwardedHeader_usesRemoteAddrRegardless() {
        String resolved = ClientIpResolver.resolve((String) null, "10.0.0.1", List.of("10.0.0.1"));
        assertThat(resolved).isEqualTo("10.0.0.1");
    }

    @Test
    void isValidIpLiteral_acceptsIpv4AndIpv6RejectsJunk() {
        assertThat(ClientIpResolver.isValidIpLiteral("203.0.113.5")).isTrue();
        assertThat(ClientIpResolver.isValidIpLiteral("::1")).isTrue();
        assertThat(ClientIpResolver.isValidIpLiteral("not-an-ip")).isFalse();
        assertThat(ClientIpResolver.isValidIpLiteral(null)).isFalse();
    }
}
