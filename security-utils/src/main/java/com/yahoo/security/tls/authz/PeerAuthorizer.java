// Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.security.tls.authz;

import com.yahoo.security.SubjectAlternativeName;
import com.yahoo.security.X509CertificateUtils;
import com.yahoo.security.tls.policy.AuthorizedPeers;
import com.yahoo.security.tls.policy.PeerPolicy;
import com.yahoo.security.tls.policy.RequiredPeerCredential;
import com.yahoo.security.tls.policy.Role;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.yahoo.security.SubjectAlternativeName.Type.DNS_NAME;
import static com.yahoo.security.SubjectAlternativeName.Type.IP_ADDRESS;
import static java.util.stream.Collectors.toList;

/**
 * Uses rules from {@link AuthorizedPeers} to evaluate X509 certificates
 *
 * @author bjorncs
 */
public class PeerAuthorizer {
    private final AuthorizedPeers authorizedPeers;

    public PeerAuthorizer(AuthorizedPeers authorizedPeers) {
        this.authorizedPeers = authorizedPeers;
    }

    public AuthorizationResult authorizePeer(X509Certificate peerCertificate) {
        Set<Role> assumedRoles = new HashSet<>();
        Set<String> matchedPolicies = new HashSet<>();
        String cn = getCommonName(peerCertificate).orElse(null);
        List<String> sans = getSubjectAlternativeNames(peerCertificate);
        for (PeerPolicy peerPolicy : authorizedPeers.peerPolicies()) {
            if (matchesPolicy(peerPolicy, cn, sans)) {
                assumedRoles.addAll(peerPolicy.assumedRoles());
                matchedPolicies.add(peerPolicy.policyName());
            }
        }
        return new AuthorizationResult(assumedRoles, matchedPolicies);
    }

    private static boolean matchesPolicy(PeerPolicy peerPolicy, String cn, List<String> sans) {
        return peerPolicy.requiredCredentials().stream()
                .allMatch(requiredCredential -> matchesRequiredCredentials(requiredCredential, cn, sans));
    }

    private static boolean matchesRequiredCredentials(RequiredPeerCredential requiredCredential, String cn, List<String> sans) {
        switch (requiredCredential.field()) {
            case CN:
                return cn != null && requiredCredential.pattern().matches(cn);
            case SAN_DNS:
                return sans.stream()
                        .anyMatch(san -> requiredCredential.pattern().matches(san));
            default:
                throw new RuntimeException("Unknown field: " + requiredCredential.field());
        }
    }

    private static Optional<String> getCommonName(X509Certificate peerCertificate) {
        return X509CertificateUtils.getSubjectCommonNames(peerCertificate).stream()
                .findFirst();
    }

    private static List<String> getSubjectAlternativeNames(X509Certificate peerCertificate) {
        return X509CertificateUtils.getSubjectAlternativeNames(peerCertificate).stream()
                .filter(san -> san.getType() == DNS_NAME || san.getType() == IP_ADDRESS)
                .map(SubjectAlternativeName::getValue)
                .collect(toList());
    }
}