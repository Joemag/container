package org.opentosca.planbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.xml.namespace.QName;

import org.opentosca.container.core.tosca.convention.Types;
import org.opentosca.planbuilder.model.plan.AbstractActivity;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.AbstractPlan.Link;
import org.opentosca.planbuilder.model.plan.AbstractPlan.PlanType;
import org.opentosca.planbuilder.model.plan.ActivityType;
import org.opentosca.planbuilder.model.plan.NodeTemplateActivity;
import org.opentosca.planbuilder.model.plan.RelationshipTemplateActivity;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;

/**
 * Copyright 2020 IAAS University of Stuttgart <br>
 * <br>
 *
 * Abstract plan builder to create plans that operate on the volatile components of a
 * ServiceTemplate.
 */
public abstract class AbstractVolatilePlanBuilder extends AbstractSimplePlanBuilder {

    /**
     * Generate an AbstractPlan for the given ServiceTemplate that has an activity for each volatile
     * NodeTemplate and each RelationshipTemplate connected to at least one volatile NdoeTemplate
     *
     * @param id the ID of the AbstractPlan
     * @param definitions the definitions document containing the ServiceTemplate the plan is generated
     *        for
     * @param serviceTemplate the ServiceTemplate the plan is generated for
     * @return the AbstractPlan
     */
    public static AbstractPlan generateVOG(final String id, final AbstractDefinitions definitions,
                                           final AbstractServiceTemplate serviceTemplate) {
        final Collection<AbstractActivity> activities = new ArrayList<>();
        final Set<Link> links = new HashSet<>();
        final Map<AbstractNodeTemplate, AbstractActivity> nodeMapping = new HashMap<>();
        final Map<AbstractRelationshipTemplate, AbstractActivity> relationMapping = new HashMap<>();

        for (final AbstractNodeTemplate nodeTemplate : serviceTemplate.getTopologyTemplate().getNodeTemplates()) {
            final AbstractActivity activity;

            // only provision volatile components
            if (ModelUtils.containsPolicyWithName(nodeTemplate, Types.volatilePolicyType)) {
                activity = new NodeTemplateActivity(nodeTemplate.getId() + "_provisioning_activity",
                    ActivityType.PROVISIONING, nodeTemplate);
            } else {
                activity =
                    new NodeTemplateActivity(nodeTemplate.getId() + "_none_activity", ActivityType.NONE, nodeTemplate);
            }
            activities.add(activity);
            nodeMapping.put(nodeTemplate, activity);
        }

        for (final AbstractRelationshipTemplate relationshipTemplate : serviceTemplate.getTopologyTemplate()
                                                                                      .getRelationshipTemplates()) {
            final RelationshipTemplateActivity activity;

            // only provision relations that have at least one volatile component as source or target
            if (Objects.nonNull(nodeMapping.get(relationshipTemplate.getSource()))
                || Objects.nonNull(nodeMapping.get(relationshipTemplate.getTarget()))) {
                activity = new RelationshipTemplateActivity(relationshipTemplate.getId() + "_provisioning_activity",
                    ActivityType.PROVISIONING, relationshipTemplate);
            } else {
                activity = new RelationshipTemplateActivity(relationshipTemplate.getId() + "_none_activity",
                    ActivityType.NONE, relationshipTemplate);
            }
            activities.add(activity);
            relationMapping.put(relationshipTemplate, activity);
        }

        // add links to define the order of the resulting BPEL plan
        for (final Entry<AbstractRelationshipTemplate, AbstractActivity> entry : relationMapping.entrySet()) {

            final AbstractActivity activity = entry.getValue();
            final QName baseType = ModelUtils.getRelationshipBaseType(entry.getKey());

            final AbstractActivity sourceActivity = nodeMapping.get(entry.getKey().getSource());
            final AbstractActivity targetActivity = nodeMapping.get(entry.getKey().getTarget());

            if (baseType.equals(Types.connectsToRelationType)) {
                if (Objects.nonNull(sourceActivity)) {
                    links.add(new Link(sourceActivity, activity));
                }
                if (Objects.nonNull(targetActivity)) {
                    links.add(new Link(targetActivity, activity));
                }
            } else if (baseType.equals(Types.dependsOnRelationType) | baseType.equals(Types.hostedOnRelationType)
                | baseType.equals(Types.deployedOnRelationType)) {
                    if (Objects.nonNull(targetActivity)) {
                        links.add(new Link(targetActivity, activity));
                    }
                    if (Objects.nonNull(sourceActivity)) {
                        links.add(new Link(activity, sourceActivity));
                    }
                }
        }

        final AbstractPlan plan =
            new AbstractPlan(id, PlanType.MANAGE, definitions, serviceTemplate, activities, links) {

            };
        return plan;
    }
}
