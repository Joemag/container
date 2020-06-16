package org.opentosca.deployment.checks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.opentosca.container.core.model.csar.Csar;
import org.opentosca.container.core.model.csar.CsarId;
import org.opentosca.container.core.next.model.DeploymentTest;
import org.opentosca.container.core.next.model.DeploymentTestState;
import org.opentosca.container.core.next.model.PlanInstance;
import org.opentosca.container.core.next.model.PlanInstanceState;
import org.opentosca.container.core.next.model.ServiceTemplateInstance;
import org.opentosca.container.core.next.repository.DeploymentTestRepository;
import org.opentosca.container.core.next.repository.PlanInstanceRepository;
import org.opentosca.container.core.service.CsarStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentTestService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentTestService.class);

    private final DeploymentTestRepository repository = new DeploymentTestRepository();
    private final CsarStorageService csarStorage;

    private final ExecutorService pool = Executors.newFixedThreadPool(5);

    private final TestExecutor executor;

    @Inject
    public DeploymentTestService(TestExecutor executor, CsarStorageService csarStorage) {
        logger.debug("Instantiating DeploymentTestService");
        this.executor = executor;
        this.csarStorage = csarStorage;
    }

    /**
     * Runs a deployment test if a plan with the given correlation id is in state FINISHED.
     *
     * @param csarId        The corresponding CSAR
     * @param correlationId The correlation ID of a plan
     */
    public void runAfterPlan(final CsarId csarId, final String correlationId) {
        logger.info("Trigger deployment test after plan has been finished; correlation_id={}, csar={}", correlationId,
            csarId);
        this.pool.submit(() -> {
            final long sleep = 1000;
            final long timeout = TimeUnit.MINUTES.toMillis(45);
            long waited = 0;
            while (true) {
                PlanInstance pi = null;
                boolean finished = false;
                try {
                    pi = new PlanInstanceRepository().findByCorrelationId(correlationId);
                    finished = pi.getState().equals(PlanInstanceState.FINISHED);
                } catch (final Exception e) {
                    finished = false;
                }
                if (finished) {
                    run(csarId, pi.getServiceTemplateInstance());
                    break;
                }
                if (waited >= timeout) {
                    logger.warn("Timeout reached, deployment test has not been executed");
                    break;
                }
                try {
                    Thread.sleep(sleep);
                } catch (final InterruptedException e) {
                }
                waited += sleep;
            }
        });
    }

    /**
     * Runs a deployment test for a certain service template instance.
     *
     * @param csarId                  The corresponding CSAR
     * @param serviceTemplateInstance The service template instance
     * @return The created Verification object
     */
    public DeploymentTest run(final CsarId csarId, final ServiceTemplateInstance serviceTemplateInstance) {

        logger.info("Trigger deployment test for service template instance \"{}\" of CSAR \"{}\"",
            serviceTemplateInstance.getId(), csarId);

        // Prepare
        final DeploymentTest result = new DeploymentTest();
        result.setServiceTemplateInstance(serviceTemplateInstance);
        result.setState(DeploymentTestState.STARTED);
        this.repository.add(result);

        // Execute
        this.pool.submit(() -> {
            logger.info("Executing deployment test...");
            // Prepare the context
            final Csar csar = csarStorage.findById(csarId);
            final TServiceTemplate entryServiceTemplate = csar.entryServiceTemplate();
            final TestContext context = new TestContext(csar, entryServiceTemplate, serviceTemplateInstance, result);
            final CompletableFuture<Void> future = this.executor.verify(context);
            logger.info("Wait until jobs has been finished...");
            try {
                future.join();
                logger.info("Jobs has been finished");
                result.setState(DeploymentTestState.FINISHED);
            } catch (final Exception e) {
                logger.error("Jobs completed with exception: {}", e.getMessage(), e);
                result.setState(DeploymentTestState.FAILED);
            }
            this.repository.update(result);
        });
        logger.info("Deployment test is running in background...");

        return result;
    }
}
