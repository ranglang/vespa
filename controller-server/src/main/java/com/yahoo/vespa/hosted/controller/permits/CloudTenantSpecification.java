package com.yahoo.vespa.hosted.controller.permits;

import com.yahoo.config.provision.TenantName;

import java.security.Principal;

import static java.util.Objects.requireNonNull;

/**
 * Wraps the permit data of an Okta tenancy modification.
 *
 * @author jonmv
 */
public class CloudTenantSpecification extends TenantSpecification {

    private final String registrationToken;

    public CloudTenantSpecification(TenantName tenant, Principal user, String registrationToken) {
        super(tenant, user);
        this.registrationToken = requireNonNull(registrationToken);
    }

    /** The cloud issued token proving the user intends to register the given tenant. */
    public String getRegistrationToken() { return registrationToken; }

}
