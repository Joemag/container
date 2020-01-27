package org.opentosca.planbuilder.core.bpel.typebasedplanbuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.container.core.tosca.convention.Interfaces;
import org.opentosca.container.core.tosca.convention.Types;
import org.opentosca.planbuilder.AbstractVolatilePlanBuilder;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright 2020 IAAS University of Stuttgart <br>
 * <br>
 *
 * Plan builder to create plans that provision the volatile components of a ServiceTemplate.
 */
public class BPELVolatileBuildProcessBuilder extends AbstractVolatilePlanBuilder {

    final static Logger LOG = LoggerFactory.getLogger(BPELVolatileBuildProcessBuilder.class);

    private BPELPlanHandler planHandler;

    public BPELVolatileBuildProcessBuilder() {
        try {
            this.planHandler = new BPELPlanHandler();
        }
        catch (final ParserConfigurationException e) {
            LOG.error("Error while initializing BPELPlanHandler", e);
        }
    }

    @Override
    public BPELPlan buildPlan(final String csarName, final AbstractDefinitions definitions,
                              final AbstractServiceTemplate serviceTemplate) {

        final String processName = ModelUtils.makeValidNCName(serviceTemplate.getId() + "_volatileBuildPlan");
        final String processNamespace = serviceTemplate.getTargetNamespace() + "_volatileBuildPlan";

        final AbstractPlan volatileBuildPlan =
            AbstractVolatilePlanBuilder.generateVOG(new QName(processName, processNamespace).toString(), definitions,
                                                    serviceTemplate);
        LOG.debug("Abstract volatile build plan: {}", volatileBuildPlan.toString());

        final BPELPlan volatileBPELBuildPlan =
            this.planHandler.createEmptyBPELPlan(processNamespace, processName, volatileBuildPlan,
                                                 Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_PLAN_LIFECYCLE_INITIATE_VOLATILE);

        volatileBPELBuildPlan.setTOSCAInterfaceName(Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_PLAN_LIFECYCLE);
        volatileBPELBuildPlan.setTOSCAOperationname(Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_PLAN_LIFECYCLE_INITIATE_VOLATILE);

        // TODO: add provisioning logic

        return volatileBPELBuildPlan;
    }

    @Override
    public List<AbstractPlan> buildPlans(final String csarName, final AbstractDefinitions definitions) {
        final List<AbstractPlan> plans = new ArrayList<>();
        for (final AbstractServiceTemplate serviceTemplate : definitions.getServiceTemplates()) {

            if (ModelUtils.containsPolicyWithName(serviceTemplate, Types.volatilePolicyType)) {
                LOG.debug("Generating VolatileBuildPlan for ServiceTemplate {}", serviceTemplate.getQName().toString());
                final BPELPlan newVolatileBuildPlan = buildPlan(csarName, definitions, serviceTemplate);

                if (Objects.nonNull(newVolatileBuildPlan)) {
                    LOG.debug("Created VolatileBuildPlan successfully.");
                    plans.add(newVolatileBuildPlan);
                }
            }
        }
        return plans;
    }
}
