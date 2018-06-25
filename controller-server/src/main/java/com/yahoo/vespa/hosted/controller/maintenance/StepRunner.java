package com.yahoo.vespa.hosted.controller.maintenance;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.vespa.hosted.controller.api.integration.deployment.JobType;
import com.yahoo.vespa.hosted.controller.deployment.RunStatus;
import com.yahoo.vespa.hosted.controller.deployment.Step;

/**
 * Advances a given job run by running the appropriate {@link Step}s, based on their current status.
 *
 * When an attempt is made to advance a given job, a lock for that job (application and type) is
 * taken, and released again only when the attempt finishes. Multiple other attempts may be made in
 * the meantime, but they should give up unless the lock is promptly acquired.
 *
 * @author jonmv
 */
public interface StepRunner {

    /**
     * Attempts to run the given step, and returns the new status.
     */
    default RunStatus run(Step step, RunStatus run) {
        switch (step) {
            default: throw new AssertionError();
        }
    }

    default Step.Status deployInitialReal(ApplicationId id, JobType type) {
        throw new AssertionError();
    }

}