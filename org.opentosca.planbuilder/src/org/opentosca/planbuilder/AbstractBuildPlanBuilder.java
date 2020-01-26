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
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;;

public abstract class AbstractBuildPlanBuilder extends AbstractSimplePlanBuilder {

    final static Logger LOG = LoggerFactory.getLogger(AbstractBuildPlanBuilder.class);

    protected static AbstractPlan generatePOG(final String id, final AbstractDefinitions definitions,
                                              final AbstractServiceTemplate serviceTemplate,
                                              final Collection<AbstractNodeTemplate> nodeTemplates,
                                              final Collection<AbstractRelationshipTemplate> relationshipTemplates) {
        final Collection<AbstractActivity> activities = new ArrayList<>();
        final Set<Link> links = new HashSet<>();
        generatePOGActivitesAndLinks(activities, links, nodeTemplates, relationshipTemplates);

        final AbstractPlan plan =
            new AbstractPlan(id, PlanType.BUILD, definitions, serviceTemplate, activities, links) {

            };
        return plan;
    }

    protected static AbstractPlan generatePOG(final String id, final AbstractDefinitions definitions,
                                              final AbstractServiceTemplate serviceTemplate) {

        final Collection<AbstractActivity> activities = new ArrayList<>();
        final Set<Link> links = new HashSet<>();

        final AbstractTopologyTemplate topology = serviceTemplate.getTopologyTemplate();
        generatePOGActivitesAndLinks(activities, links, topology.getNodeTemplates(),
                                     topology.getRelationshipTemplates());

        final AbstractPlan plan =
            new AbstractPlan(id, PlanType.BUILD, definitions, serviceTemplate, activities, links) {

            };
        return plan;
    }

    private static void generatePOGActivitesAndLinks(final Collection<AbstractActivity> activities,
                                                     final Set<Link> links,
                                                     final Collection<AbstractNodeTemplate> nodeTemplates,
                                                     final Collection<AbstractRelationshipTemplate> relationshipTemplates) {
        final Map<AbstractNodeTemplate, AbstractActivity> nodeMapping = new HashMap<>();
        final Map<AbstractRelationshipTemplate, AbstractActivity> relationMapping = new HashMap<>();

        for (final AbstractNodeTemplate nodeTemplate : nodeTemplates) {
            // filter volatile components from the build plan
            if (ModelUtils.containsPolicyWithName(nodeTemplate, Types.volatilePolicyType)) {
                LOG.debug("Skipping NodeTemplate {} as it has a volatile policy attached!", nodeTemplate.getId());
                continue;
            }

            final AbstractActivity activity = new NodeTemplateActivity(nodeTemplate.getId() + "_provisioning_activity",
                ActivityType.PROVISIONING, nodeTemplate);
            activities.add(activity);
            nodeMapping.put(nodeTemplate, activity);
        }

        for (final AbstractRelationshipTemplate relationshipTemplate : relationshipTemplates) {
            // filter relations between volatile components and other components from the build plan
            if (Objects.isNull(nodeMapping.get(relationshipTemplate.getSource()))
                || Objects.isNull(nodeMapping.get(relationshipTemplate.getTarget()))) {
                LOG.debug("Skipping RelationshipTemplate {} as it has a volatile source or target component!",
                          relationshipTemplate.getId());
                continue;
            }

            final AbstractActivity activity =
                new RelationshipTemplateActivity(relationshipTemplate.getId() + "_provisioning_activity",
                    ActivityType.PROVISIONING, relationshipTemplate);
            activities.add(activity);
            relationMapping.put(relationshipTemplate, activity);
        }

        for (final Entry<AbstractRelationshipTemplate, AbstractActivity> entry : relationMapping.entrySet()) {

            final AbstractActivity activity = entry.getValue();
            final QName baseType = ModelUtils.getRelationshipBaseType(entry.getKey());

            final AbstractActivity sourceActivity = nodeMapping.get(entry.getKey().getSource());
            final AbstractActivity targetActivity = nodeMapping.get(entry.getKey().getTarget());
            if (baseType.equals(Types.connectsToRelationType)) {
                if (sourceActivity != null) {
                    links.add(new Link(sourceActivity, activity));
                }
                if (targetActivity != null) {
                    links.add(new Link(targetActivity, activity));
                }
            } else if (baseType.equals(Types.dependsOnRelationType) | baseType.equals(Types.hostedOnRelationType)
                | baseType.equals(Types.deployedOnRelationType)) {
                    if (targetActivity != null) {
                        links.add(new Link(targetActivity, activity));
                    }
                    if (sourceActivity != null) {
                        links.add(new Link(activity, sourceActivity));
                    }
                }
        }
    }
}
