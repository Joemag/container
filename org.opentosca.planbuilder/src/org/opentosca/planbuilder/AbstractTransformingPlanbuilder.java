package org.opentosca.planbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.opentosca.container.core.tosca.convention.Types;
import org.opentosca.planbuilder.model.plan.AbstractActivity;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.AbstractPlan.Link;
import org.opentosca.planbuilder.model.plan.AbstractPlan.PlanType;
import org.opentosca.planbuilder.model.plan.AbstractTransformationPlan;
import org.opentosca.planbuilder.model.plan.ActivityType;
import org.opentosca.planbuilder.model.plan.NodeTemplateActivity;
import org.opentosca.planbuilder.model.plan.RelationshipTemplateActivity;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractDeploymentArtifact;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractTopologyTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.opentosca.planbuilder.plugins.registry.PluginRegistry;
import org.opentosca.planbuilder.plugins.typebased.IPlanBuilderTypePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Class for generating an AbstractPlan implementing a Transformation Function from a
 * Source Model to a Target Model and their respective instances
 *
 * @author Kálmán Képes - kalman.kepes@iaas.uni-stuttgart.de
 *
 */
public abstract class AbstractTransformingPlanbuilder extends AbstractPlanBuilder {

    protected final PluginRegistry pluginRegistry = new PluginRegistry();

    private final static Logger LOG = LoggerFactory.getLogger(AbstractTransformingPlanbuilder.class);

    @Override
    public PlanType createdPlanType() {
        return PlanType.MANAGE;
    }

    /**
     * <p>
     * Creates a BuildPlan in WS-BPEL 2.0 by using the the referenced source and target service
     * templates as the transforming function between two models.
     * </p>
     *
     * @param sourceCsarName the name of the source csar
     * @param sourceDefinitions the id of the source definitions inside the referenced source csar
     * @param sourceServiceTemplateId the id of the source service templates inside the referenced
     *        definitions
     * @param targetCsarName the name of the target csar
     * @param targetDefinitions the id of the target definitions inside the referenced target csar
     * @param targetServiceTemplateId the id of the target service templates inside the referenced
     *        definitions
     * @return a single AbstractPlan with a concrete implementation of a transformation function from
     *         the source to the target topology
     */
    abstract public AbstractPlan buildPlan(String sourceCsarName, AbstractDefinitions sourceDefinitions,
                                           QName sourceServiceTemplateId, String targetCsarName,
                                           AbstractDefinitions targetDefinitions, QName targetServiceTemplateId);

    /**
     * Generates a Set of Plans that is generated based on the given source and target definitions. This
     * generation is done for each Topology Template defined in both definitions therefore for each
     * combination of source and target topology template a plan is generated
     *
     * @param sourceCsarName the name of the source csar
     * @param sourceDefinitions the id of the source definitions inside the referenced source csar
     * @param targetCsarName the name of the target csar
     * @param targetDefinitions the id of the target definitions inside the referenced target csar
     * @return a List of AbstractPlans
     */
    abstract public List<AbstractPlan> buildPlans(String sourceCsarName, AbstractDefinitions sourceDefinitions,
                                                  String targetCsarName, AbstractDefinitions targetDefinitions);


    public AbstractTransformationPlan generateTFOG(final String sourceCsarName,
                                                   final AbstractDefinitions sourceDefinitions,
                                                   final AbstractServiceTemplate sourceServiceTemplate,
                                                   final Collection<AbstractNodeTemplate> sourceNodeTemplates,
                                                   final Collection<AbstractRelationshipTemplate> sourceRelationshipTemplates,
                                                   final String targetCsarName,
                                                   final AbstractDefinitions targetDefinitions,
                                                   final AbstractServiceTemplate targetServiceTemplate,
                                                   final Collection<AbstractNodeTemplate> targetNodeTemplates,
                                                   final Collection<AbstractRelationshipTemplate> targetRelationshipTemplates) {

        final Set<AbstractNodeTemplate> maxCommonSubgraph =
            getMaxCommonSubgraph(new HashSet<>(sourceNodeTemplates), new HashSet<>(sourceNodeTemplates),
                                 new HashSet<>(targetNodeTemplates), new HashSet<AbstractNodeTemplate>());

        // find valid subset inside common subgraph, i.e.:
        // any component that is a platform node (every node without outgoing
        // hostedOn edges), or is a node in the subgraph where its (transitive) platform
        // nodes are also in the subgraph are valid

        final Set<AbstractNodeTemplate> deployableMaxCommonSubgraph =
            this.getDeployableSubgraph(new HashSet<>(getCorrespondingNodes(maxCommonSubgraph, targetNodeTemplates)));

        // determine steps which have to be deleted from the original topology
        final Set<AbstractNodeTemplate> nodesToTerminate = new HashSet<>(sourceNodeTemplates);
        nodesToTerminate.removeAll(deployableMaxCommonSubgraph);
        final Collection<AbstractRelationshipTemplate> relationsToTerminate = getOutgoingRelations(nodesToTerminate);

        final AbstractPlan termPlan =
            AbstractTerminationPlanBuilder.generateTOG("transformTerminate" + sourceDefinitions.getId() + "_to_"
                + targetDefinitions.getId(), sourceDefinitions, sourceServiceTemplate, nodesToTerminate,
                                                       relationsToTerminate);


        // migrate node instances from old service instance to new service instance
        final AbstractPlan migrateInstancePlan =
            generateInstanceMigrationPlan(deployableMaxCommonSubgraph,
                                          getConnectingEdges(sourceRelationshipTemplates, deployableMaxCommonSubgraph),
                                          sourceDefinitions, targetDefinitions, sourceServiceTemplate,
                                          targetServiceTemplate);

        // determine steps which have to be start within the new topology
        final Set<AbstractNodeTemplate> nodesToStart = new HashSet<>(targetNodeTemplates);
        nodesToStart.removeAll(getCorrespondingNodes(deployableMaxCommonSubgraph, targetNodeTemplates));

        final Collection<AbstractRelationshipTemplate> relationsToStart =
            this.getDeployableSubgraph(targetNodeTemplates, getOutgoingRelations(nodesToStart));

        final AbstractPlan startPlan = AbstractBuildPlanBuilder.generatePOG(
                                                                            "transformStart" + sourceDefinitions.getId()
                                                                                + "_to_" + targetDefinitions.getId(),
                                                                            targetDefinitions, targetServiceTemplate,
                                                                            nodesToStart, relationsToStart, false);


        AbstractTransformationPlan transPlan = mergePlans(
                                                          "transformationPlan_" + termPlan.getServiceTemplate().getId()
                                                              + "_to_" + startPlan.getServiceTemplate().getId(),
                                                          PlanType.TRANSFORM, termPlan, migrateInstancePlan);

        transPlan = mergePlans("transformationPlan_" + termPlan.getServiceTemplate().getId() + "_to_"
            + startPlan.getServiceTemplate().getId(), PlanType.TRANSFORM, transPlan, startPlan);


        return transPlan;
    }

    public Collection<AbstractRelationshipTemplate> getDeployableSubgraph(final Collection<AbstractNodeTemplate> nodes,
                                                                          final Collection<AbstractRelationshipTemplate> relations) {
        final Collection<AbstractRelationshipTemplate> result = new HashSet<>();
        for (final AbstractRelationshipTemplate rel : relations) {
            if (nodes.contains(rel.getSource()) && nodes.contains(rel.getTarget())) {
                result.add(rel);
            }
        }
        return result;
    }

    public AbstractTransformationPlan generateTFOG(final String sourceCsarName,
                                                   final AbstractDefinitions sourceDefinitions,
                                                   final AbstractServiceTemplate sourceServiceTemplate,
                                                   final String targetCsarName,
                                                   final AbstractDefinitions targetDefinitions,
                                                   final AbstractServiceTemplate targetServiceTemplate) {
        return this.generateTFOG(sourceCsarName, sourceDefinitions, sourceServiceTemplate,
                                 sourceServiceTemplate.getTopologyTemplate().getNodeTemplates(),
                                 sourceServiceTemplate.getTopologyTemplate().getRelationshipTemplates(), targetCsarName,
                                 targetDefinitions, targetServiceTemplate,
                                 targetServiceTemplate.getTopologyTemplate().getNodeTemplates(),
                                 targetServiceTemplate.getTopologyTemplate().getRelationshipTemplates());
    }

    /**
     * Generates an abstract order of activities to transform from the source service template to the
     * target service template
     *
     * @param sourceCsarName the name of the source csar
     * @param sourceDefinitions the id of the source definitions inside the referenced source csar
     * @param sourceServiceTemplateId the id of the source service templates inside the referenced
     *        definitions
     * @param targetCsarName the name of the target csar
     * @param targetDefinitions the id of the target definitions inside the referenced target csar
     * @param targetServiceTemplateId the id of the target service templates inside the referenced
     *        definitions
     * @return a single AbstractPlan containing abstract activities for a transformation function from
     *         the source to the target topology
     */
    public AbstractTransformationPlan _generateTFOG(final String sourceCsarName,
                                                    final AbstractDefinitions sourceDefinitions,
                                                    final AbstractServiceTemplate sourceServiceTemplate,
                                                    final String targetCsarName,
                                                    final AbstractDefinitions targetDefinitions,
                                                    final AbstractServiceTemplate targetServiceTemplate) {
        final AbstractTopologyTemplate sourceTopology = sourceServiceTemplate.getTopologyTemplate();
        final AbstractTopologyTemplate targetTopology = targetServiceTemplate.getTopologyTemplate();

        final Set<AbstractNodeTemplate> maxCommonSubgraph =
            getMaxCommonSubgraph(new HashSet<>(sourceTopology.getNodeTemplates()),
                                 new HashSet<>(sourceTopology.getNodeTemplates()),
                                 new HashSet<>(targetTopology.getNodeTemplates()), new HashSet<AbstractNodeTemplate>());

        // find valid subset inside common subgraph, i.e.:
        // any component that is a platform node (every node without outgoing
        // hostedOn edges), or is a node in the subgraph where its (transitive) platform
        // nodes are also in the subgraph are valid
        final Set<AbstractNodeTemplate> deployableMaxCommonSubgraph = this.getDeployableSubgraph(maxCommonSubgraph);

        // determine steps which have to be deleted from the original topology
        final Set<AbstractNodeTemplate> nodesToTerminate = new HashSet<>(sourceTopology.getNodeTemplates());
        nodesToTerminate.removeAll(deployableMaxCommonSubgraph);
        final Collection<AbstractRelationshipTemplate> relationsToTerminate = getOutgoingRelations(nodesToTerminate);

        final AbstractPlan termPlan =
            AbstractTerminationPlanBuilder.generateTOG("transformTerminate" + sourceDefinitions.getId() + "_to_"
                + targetDefinitions.getId(), sourceDefinitions, sourceServiceTemplate, nodesToTerminate,
                                                       relationsToTerminate);


        // migrate node instances from old service instance to new service instance
        final AbstractPlan migrateInstancePlan =
            generateInstanceMigrationPlan(deployableMaxCommonSubgraph,
                                          getConnectingEdges(sourceTopology.getRelationshipTemplates(),
                                                             deployableMaxCommonSubgraph),
                                          sourceDefinitions, targetDefinitions, sourceServiceTemplate,
                                          targetServiceTemplate);

        // determine steps which have to be start within the new topology
        final Set<AbstractNodeTemplate> nodesToStart = new HashSet<>(targetTopology.getNodeTemplates());
        nodesToStart.removeAll(getCorrespondingNodes(deployableMaxCommonSubgraph, targetTopology.getNodeTemplates()));
        final Collection<AbstractRelationshipTemplate> relationsToStart = getOutgoingRelations(nodesToStart);

        final AbstractPlan startPlan = AbstractBuildPlanBuilder.generatePOG(
                                                                            "transformStart" + sourceDefinitions.getId()
                                                                                + "_to_" + targetDefinitions.getId(),
                                                                            targetDefinitions, targetServiceTemplate,
                                                                            nodesToStart, relationsToStart, false);


        AbstractTransformationPlan transPlan = mergePlans(
                                                          "transformationPlan_" + termPlan.getServiceTemplate().getId()
                                                              + "_to_" + startPlan.getServiceTemplate().getId(),
                                                          PlanType.TRANSFORM, termPlan, migrateInstancePlan);

        transPlan = mergePlans("transformationPlan_" + termPlan.getServiceTemplate().getId() + "_to_"
            + startPlan.getServiceTemplate().getId(), PlanType.TRANSFORM, transPlan, startPlan);


        return transPlan;
    }

    private AbstractTransformationPlan generateInstanceMigrationPlan(final Collection<AbstractNodeTemplate> nodeTemplates,
                                                                     final Collection<AbstractRelationshipTemplate> relationshipTemplates,
                                                                     final AbstractDefinitions sourceDefinitions,
                                                                     final AbstractDefinitions targetDefinitions,
                                                                     final AbstractServiceTemplate sourceServiceTemplate,
                                                                     final AbstractServiceTemplate targetServiceTemplate) {

        // General flow is as within a build plan

        final Collection<AbstractActivity> activities = new ArrayList<>();
        final Set<Link> links = new HashSet<>();
        final Map<AbstractNodeTemplate, AbstractActivity> nodeMapping = new HashMap<>();
        final Map<AbstractRelationshipTemplate, AbstractActivity> relationMapping = new HashMap<>();

        generateIMOGActivitesAndLinks(activities, links, nodeMapping, nodeTemplates, relationMapping,
                                      relationshipTemplates);

        return new AbstractTransformationPlan(
            "migrateInstance" + sourceDefinitions.getId() + "_to_" + targetDefinitions.getId(),
            AbstractPlan.PlanType.TRANSFORM, sourceDefinitions, sourceServiceTemplate, targetDefinitions,
            targetServiceTemplate, activities, links);
    }

    private void generateIMOGActivitesAndLinks(final Collection<AbstractActivity> activities, final Set<Link> links,
                                               final Map<AbstractNodeTemplate, AbstractActivity> nodeActivityMapping,
                                               final Collection<AbstractNodeTemplate> nodeTemplates,
                                               final Map<AbstractRelationshipTemplate, AbstractActivity> relationActivityMapping,
                                               final Collection<AbstractRelationshipTemplate> relationshipTemplates) {
        for (final AbstractNodeTemplate nodeTemplate : nodeTemplates) {
            final AbstractActivity activity = new NodeTemplateActivity(
                nodeTemplate.getId() + "_instance_migration_activity", ActivityType.MIGRATION, nodeTemplate);
            activities.add(activity);
            nodeActivityMapping.put(nodeTemplate, activity);
        }

        for (final AbstractRelationshipTemplate relationshipTemplate : relationshipTemplates) {
            final AbstractActivity activity =
                new RelationshipTemplateActivity(relationshipTemplate.getId() + "_instance_migration_activity",
                    ActivityType.MIGRATION, relationshipTemplate);
            activities.add(activity);
            relationActivityMapping.put(relationshipTemplate, activity);
        }

        for (final AbstractRelationshipTemplate relationshipTemplate : relationshipTemplates) {
            final AbstractActivity activity = relationActivityMapping.get(relationshipTemplate);
            final QName baseType = ModelUtils.getRelationshipBaseType(relationshipTemplate);
            if (baseType.equals(Types.connectsToRelationType)) {
                links.add(new Link(nodeActivityMapping.get(relationshipTemplate.getSource()), activity));
                links.add(new Link(nodeActivityMapping.get(relationshipTemplate.getTarget()), activity));
            } else if (baseType.equals(Types.dependsOnRelationType) | baseType.equals(Types.hostedOnRelationType)
                | baseType.equals(Types.deployedOnRelationType)) {
                    links.add(new Link(nodeActivityMapping.get(relationshipTemplate.getTarget()), activity));
                    links.add(new Link(activity, nodeActivityMapping.get(relationshipTemplate.getSource())));
                }

        }

    }

    private Collection<AbstractRelationshipTemplate> getConnectingEdges(final Collection<AbstractRelationshipTemplate> allEdges,
                                                                        final Collection<AbstractNodeTemplate> subgraphNodes) {
        final Collection<AbstractRelationshipTemplate> connectingEdges = new HashSet<>();

        for (final AbstractRelationshipTemplate rel : allEdges) {
            if (subgraphNodes.contains(rel.getSource()) && subgraphNodes.contains(rel.getTarget())) {
                connectingEdges.add(rel);
            }
        }

        return connectingEdges;
    }

    private Collection<AbstractNodeTemplate> getCorrespondingNodes(final Collection<AbstractNodeTemplate> subgraph,
                                                                   final Collection<AbstractNodeTemplate> graph) {
        final Collection<AbstractNodeTemplate> correspondingNodes = new HashSet<>();
        for (final AbstractNodeTemplate subgraphNode : subgraph) {
            AbstractNodeTemplate correspondingNode = null;
            if ((correspondingNode = getCorrespondingNode(subgraphNode, graph)) != null) {
                correspondingNodes.add(correspondingNode);
            }
        }

        return correspondingNodes;
    }

    protected AbstractNodeTemplate getCorrespondingNode(final AbstractNodeTemplate subNode,
                                                        final Collection<AbstractNodeTemplate> graph) {
        for (final AbstractNodeTemplate graphNode : graph) {
            if (this.mappingEquals(subNode, graphNode)) {
                return graphNode;
            }
        }
        return null;
    }

    public AbstractRelationshipTemplate getCorrespondingEdge(final AbstractRelationshipTemplate subEdge,
                                                             final Collection<AbstractRelationshipTemplate> graphEdges) {
        for (final AbstractRelationshipTemplate graphEdge : graphEdges) {
            if (this.mappingEquals(subEdge, graphEdge)) {
                return graphEdge;
            }
        }
        return null;
    }

    private AbstractTransformationPlan mergePlans(final String id, final PlanType type, final AbstractPlan plan1,
                                                  final AbstractPlan plan2) {

        final Collection<AbstractActivity> activities = new HashSet<>();
        activities.addAll(plan1.getActivites());
        activities.addAll(plan2.getActivites());

        final Collection<Link> links = new HashSet<>();
        links.addAll(plan1.getLinks());
        links.addAll(plan2.getLinks());

        final Collection<AbstractActivity> sinks = plan1.getSinks();
        final Collection<AbstractActivity> sources = plan2.getSources();

        // naively we connect each sink with each source
        for (final AbstractActivity sink : sinks) {
            for (final AbstractActivity source : sources) {
                links.add(new Link(sink, source));
            }
        }

        return new AbstractTransformationPlan(id, type, plan1.getDefinitions(), plan1.getServiceTemplate(),
            plan2.getDefinitions(), plan2.getServiceTemplate(), activities, links);

    }



    private Collection<AbstractRelationshipTemplate> getOutgoingRelations(final Set<AbstractNodeTemplate> nodes) {
        final Collection<AbstractRelationshipTemplate> relations = new HashSet<>();
        for (final AbstractNodeTemplate node : nodes) {
            relations.addAll(node.getOutgoingRelations());
        }
        return relations;
    }

    private Collection<AbstractNodeTemplate> getNeededNodes(final AbstractNodeTemplate nodeTemplate) {
        for (final IPlanBuilderTypePlugin typePlugin : this.pluginRegistry.getTypePlugins()) {
            if (typePlugin.canHandleCreate(nodeTemplate)) {
                if (typePlugin instanceof IPlanBuilderTypePlugin.NodeDependencyInformationInterface) {
                    return ((IPlanBuilderTypePlugin.NodeDependencyInformationInterface) typePlugin).getCreateDependencies(nodeTemplate);
                }
            }
        }
        return null;
    }

    public Set<AbstractNodeTemplate> getDeployableSubgraph(final Set<AbstractNodeTemplate> graph) {
        final Set<AbstractNodeTemplate> validDeploymentSubgraph = new HashSet<>(graph);
        final Set<AbstractNodeTemplate> toRemove = new HashSet<>();

        for (final AbstractNodeTemplate node : graph) {

            if (isRunning(node) && hasNoHostingNodes(node)) {
                continue;
            }

            final Collection<AbstractNodeTemplate> neededNodes = getNeededNodes(node);

            // no plugin found that can deploy given node on whole topology
            if (neededNodes == null) {
                toRemove.add(node);
                continue;
            }

            // if the needed nodes are not in the graph we cannot deploy it
            if (!contains(graph, neededNodes)) {
                toRemove.add(node);
                continue;
            }

        }

        if (toRemove.isEmpty()) {
            return validDeploymentSubgraph;
        } else {
            validDeploymentSubgraph.removeAll(toRemove);
            return getDeployableSubgraph(validDeploymentSubgraph);
        }
    }

    private boolean hasNoHostingNodes(final AbstractNodeTemplate nodeTemplate) {
        for (final AbstractRelationshipTemplate rel : nodeTemplate.getOutgoingRelations()) {
            if (rel.getType().equals(Types.hostedOnRelationType) | rel.getType().equals(Types.dependsOnRelationType)) {
                return false;
            }
        }

        return true;
    }

    private boolean contains(final Collection<AbstractNodeTemplate> subgraph1,
                             final Collection<AbstractNodeTemplate> subgraph2) {

        for (final AbstractNodeTemplate nodeInGraph2 : subgraph2) {
            boolean matched = false;
            for (final AbstractNodeTemplate nodeInGraph1 : subgraph1) {
                if (this.mappingEquals(nodeInGraph1, nodeInGraph2)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // element in subgraph2 is not in subgraph 1
                return false;
            }
        }

        return true;
    }



    // TODO FIXME this is a really naive implementation until we can integrate a
    // proper(i.e. efficient) subgraph calculation
    // based on https://stackoverflow.com/a/14644158
    private Set<AbstractNodeTemplate> getMaxCommonSubgraph(final Set<AbstractNodeTemplate> vertices,
                                                           final Set<AbstractNodeTemplate> graph1,
                                                           final Set<AbstractNodeTemplate> graph2,
                                                           final Set<AbstractNodeTemplate> currentSubset) {

        LOG.debug("Finding MaxCommon Subgraph with vertices {}", printCandidate(vertices));
        if (vertices.isEmpty()) {
            if (isCommonSubgraph(graph1, graph2, currentSubset)) {
                LOG.debug("Returning the current subset of {}", printCandidate(currentSubset));
                return new HashSet<>(currentSubset);
            } else {
                return new HashSet<>();
            }
        }



        final AbstractNodeTemplate v = pop(vertices);

        LOG.debug("Removed vertex {}", v.getId());
        final Set<AbstractNodeTemplate> cand1 = getMaxCommonSubgraph(vertices, graph1, graph2, currentSubset);
        currentSubset.add(v);
        LOG.debug("Current subset {}", printCandidate(currentSubset));


        Set<AbstractNodeTemplate> cand2 = new HashSet<>();

        if (isCommonSubgraph(graph1, graph2, currentSubset)) {
            cand2 = getMaxCommonSubgraph(vertices, graph1, graph2, currentSubset);
        } else {
            LOG.debug("Removing vertex {} from current subset {}", v.getId(), printCandidate(currentSubset));
            currentSubset.remove(v);
        }



        LOG.debug("Current candidates:");
        LOG.debug("Candidate1: {}", printCandidate(cand1));
        LOG.debug("Candidate2: {}", printCandidate(cand2));

        if (cand1.size() > cand2.size()) {
            LOG.debug("Returning cand1");
        } else {
            LOG.debug("Returning cand2");
        }

        return cand1.size() > cand2.size() ? cand1 : cand2;
    }

    private String printCandidate(final Collection<AbstractNodeTemplate> nodeTemplates) {
        String print = "{";


        final AbstractNodeTemplate[] nodes = nodeTemplates.toArray(new AbstractNodeTemplate[nodeTemplates.size()]);


        for (int i = 0; i < nodes.length; i++) {

            print += nodes[i].getId();

            if (i + 1 < nodes.length) {
                print += ",";
            }
        }

        print += "}";

        return print;
    }

    private AbstractNodeTemplate pop(final Set<AbstractNodeTemplate> nodes) {
        AbstractNodeTemplate pop = null;

        final Iterator<AbstractNodeTemplate> iter = nodes.iterator();

        if (iter.hasNext()) {
            pop = iter.next();
        }

        nodes.remove(pop);

        return pop;
    }

    private boolean isCommonSubgraph(final Set<AbstractNodeTemplate> graph1, final Set<AbstractNodeTemplate> graph2,
                                     final Set<AbstractNodeTemplate> subgraph) {

        for (final AbstractNodeTemplate nodeTemplate : subgraph) {
            boolean matchedIn1 = false;
            boolean matchedIn2 = false;

            for (final AbstractNodeTemplate nodeIn1 : graph1) {
                if (this.mappingEquals(nodeTemplate, nodeIn1)) {
                    matchedIn1 = true;
                    break;
                }
            }

            for (final AbstractNodeTemplate nodeIn2 : graph2) {
                if (this.mappingEquals(nodeTemplate, nodeIn2)) {
                    matchedIn2 = true;
                    break;
                }
            }

            if (!matchedIn1 | !matchedIn2) {
                return false;
            }
        }

        return true;
    }

    private boolean mappingEquals(final AbstractRelationshipTemplate rel1, final AbstractRelationshipTemplate rel2) {
        if (!rel1.getType().equals(rel2.getType())) {
            return false;
        }

        // really weak and messy check incoming!
        if (!(this.mappingEquals(rel1.getSource(), rel2.getSource())
            && this.mappingEquals(rel1.getTarget(), rel2.getTarget()))) {
            return false;
        }

        if (!rel1.getId().equals(rel2.getId())) {
            return false;
        }

        return true;
    }

    private boolean mappingEquals(final AbstractNodeTemplate node1, final AbstractNodeTemplate node2) {
        LOG.debug("Matching node {} with node {} ", node1.getId(), node2.getId());
        if (!node1.getType().getId().equals(node2.getType().getId())) {
            return false;
        }


        if (node1.getDeploymentArtifacts().size() != node2.getDeploymentArtifacts().size()) {
            return false;
        } else {
            for (final AbstractDeploymentArtifact da : node1.getDeploymentArtifacts()) {
                boolean matched = false;
                for (final AbstractDeploymentArtifact da2 : node2.getDeploymentArtifacts()) {
                    if (da.getArtifactType().equals(da.getArtifactType())) {
                        if (da.getArtifactRef().getId().equals(da2.getArtifactRef().getId())) {
                            // up to this point only the type and id of the artifact template match, a deeper mathcing
                            // would really look at the references and stuff, but we assume that artifact template id's
                            // are unique across multiple service templates
                            matched = true;
                        }
                    }
                }
                if (!matched) {
                    return false;
                }
            }
        }


        // Maybe add it later

        // This check is pretty heavy if i think about the State Property or changes in
        // values etc.
        // if (!(node1.getProperties().equals(node2.getProperties()))) {
        // return false;
        // }

        // if (!(node1.getPolicies().containsAll(node2.getPolicies())
        // && node2.getPolicies().containsAll(node1.getPolicies()))) {
        // return false;
        // }
        LOG.debug("Matched node {} with node {} ", node1.getId(), node2.getId());


        if (!node1.getId().equals(node2.getId())) {
            return false;
        }

        return true;
    }

}
