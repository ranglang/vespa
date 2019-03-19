package com.yahoo.vespa.hosted.controller.permits;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.TenantName;
import com.yahoo.container.jdisc.HttpRequest;

/**
 * Extracts {@link TenantSpecification}s and {@link ApplicationPermit}s from HTTP requests, to be stored in a {@link AccessControlManager}.
 *
 * @author jonmv
 */
public interface PermitExtractor {

    /** Extracts permit data for a tenant, from the given request. */
    TenantSpecification getTenantPermit(TenantName tenant, HttpRequest request);

    /** Extracts permit data for an application, from the given request. */
    ApplicationPermit getApplicationPermit(ApplicationId application, HttpRequest request);

}
