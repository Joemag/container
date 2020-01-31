package org.opentosca.planbuilder.core.bpel.typebasedplanbuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.opentosca.container.core.next.model.ServiceTemplateInstanceState;
import org.opentosca.planbuilder.AbstractTransformingPlanbuilder;
import org.opentosca.planbuilder.core.bpel.context.BPELPlanContext;
import org.opentosca.planbuilder.core.bpel.handlers.BPELFinalizer;
import org.opentosca.planbuilder.core.bpel.handlers.BPELPlanHandler;
import org.opentosca.planbuilder.core.bpel.handlers.CorrelationIDInitializer;
import org.opentosca.planbuilder.core.bpel.tosca.handlers.EmptyPropertyToInputHandler;
import org.opentosca.planbuilder.core.bpel.tosca.handlers.NodeRelationInstanceVariablesHandler;
import org.opentosca.planbuilder.core.bpel.tosca.handlers.PropertyVariableHandler;
import org.opentosca.planbuilder.core.bpel.tosca.handlers.ServiceTemplateBoundaryPropertyMappingsToOutputHandler;
import org.opentosca.planbuilder.core.bpel.tosca.handlers.SimplePlanBuilderServiceInstanceHandler;
import org.opentosca.planbuilder.core.bpel.typebasednodehandler.BPELPluginHandler;
import org.opentosca.planbuilder.model.plan.AbstractActivity;
import org.opentosca.planbuilder.model.plan.AbstractPlan;
import org.opentosca.planbuilder.model.plan.AbstractPlan.PlanType;
import org.opentosca.planbuilder.model.plan.AbstractTransformationPlan;
import org.opentosca.planbuilder.model.plan.ActivityType;
import org.opentosca.planbuilder.model.plan.bpel.BPELPlan;
import org.opentosca.planbuilder.model.plan.bpel.BPELScope;
import org.opentosca.planbuilder.model.tosca.AbstractDefinitions;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractServiceTemplate;
import org.opentosca.planbuilder.model.utils.ModelUtils;
import org.opentosca.planbuilder.plugins.context.Property2VariableMapping;
import org.opentosca.planbuilder.plugins.typebased.IPlanBuilderPostPhasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BPELTransformationProcessBuilder extends AbstractTransformingPlanbuilder {

    private final static Logger LOG = LoggerFactory.getLogger(BPELTransformationProcessBuilder.class);

    // class for initializing properties inside the plan
    private final PropertyVariableHandler propertyInitializer;

    // class for initializing output with boundarydefinitions of a serviceTemplate
    private final ServiceTemplateBoundaryPropertyMappingsToOutputHandler propertyOutputInitializer;
    // adds serviceInstance Variable and instanceDataAPIUrl to buildPlans

    private SimplePlanBuilderServiceInstanceHandler serviceInstanceHandler;

    // class for finalizing build plans (e.g when some template didn't receive
    // some provisioning logic and they must be filled with empty elements)
    private final BPELFinalizer finalizer;

    private BPELPlanHandler planHandler;

    private final BPELPluginHandler bpelPluginHandler = new BPELPluginHandler();

    private NodeRelationInstanceVariablesHandler nodeRelationInstanceHandler;

    private final EmptyPropertyToInputHandler emptyPropInit = new EmptyPropertyToInputHandler();

    private CorrelationIDInitializer correlationHandler;

    public BPELTransformationProcessBuilder() {
        try {
            this.planHandler = new BPELPlanHandler();
            this.serviceInstanceHandler = new SimplePlanBuilderServiceInstanceHandler();
            this.nodeRelationInstanceHandler = new NodeRelationInstanceVariablesHandler(this.planHandler);
            this.correlationHandler = new CorrelationIDInitializer();
        }
        catch (final ParserConfigurationException e) {
            LOG.error("Error while initializing BuildPlanHandler", e);
        }
        // TODO seems ugly
        this.propertyInitializer = new PropertyVariableHandler(this.planHandler);
        this.propertyOutputInitializer = new ServiceTemplateBoundaryPropertyMappingsToOutputHandler();
        this.finalizer = new BPELFinalizer();
    }


    /**
     * Creates an Adaptation PLan that can change the configuration of a running Service Instance by
     * transforming the current state of nodes and relations (sourceNodeTemplates and
     * -RelationshipTemplates) to a target configuration (targetNodeTemplates and
     * -RelationshipTemplates).
     *
     * @param planType the type of the plan that should be generated
     * @param csarName the csar of the service template
     * @param definitions the definitions document of the service template
     * @param planInterfaceName the name of the interface containing the operation that is exposed by
     *        the created plan
     * @param planOperationName the name of the operation that is exposed by the created plan
     * @param serviceTemplateId the id of the serviceTemplate to adapt its service instance
     * @param sourceNodeTemplates the nodeTemplates to adapt from
     * @param sourceRelationshipTemplates the relationships to adapt from
     * @param targetNodeTemplates the target configuration of nodes to adapt to
     * @param targetRelationshipTemplates the target configuration of relations to adapt to
     * @return a BPEL Plan that is able to adapt an instance from the given current and target
     *         configurations
     */
    public BPELPlan buildPlan(final PlanType planType, final String csarName, final AbstractDefinitions definitions,
                              final QName serviceTemplateId, final String planInterfaceName,
                              final String planOperationName,
                              final Collection<AbstractNodeTemplate> sourceNodeTemplates,
                              final Collection<AbstractRelationshipTemplate> sourceRelationshipTemplates,
                              final Collection<AbstractNodeTemplate> targetNodeTemplates,
                              final Collection<AbstractRelationshipTemplate> targetRelationshipTemplates) {
        final AbstractServiceTemplate serviceTemplate = getServiceTemplate(definitions, serviceTemplateId);

        final String processName = ModelUtils.makeValidNCName(serviceTemplate.getId() + "_" + planInterfaceName + "_"
            + planOperationName + "_" + System.currentTimeMillis());
        final String processNamespace =
            serviceTemplate.getTargetNamespace() + planInterfaceName + "_" + planOperationName;

        // generate abstract plan
        final AbstractTransformationPlan adaptationPlan =
            this.generateTFOG(planType, processName, csarName, definitions, serviceTemplate, sourceNodeTemplates,
                              sourceRelationshipTemplates, csarName, definitions, serviceTemplate, targetNodeTemplates,
                              targetRelationshipTemplates);

        // transform to bpel skeleton
        final BPELPlan transformationBPELPlan =
            this.planHandler.createEmptyBPELPlan(processNamespace, processName, adaptationPlan, planOperationName);

        transformationBPELPlan.setTOSCAInterfaceName(planInterfaceName);
        transformationBPELPlan.setTOSCAOperationname(planOperationName);

        this.planHandler.initializeBPELSkeleton(transformationBPELPlan, csarName);
        // instanceDataAPI handling is done solely trough this extension
        this.planHandler.registerExtension("http://www.apache.org/ode/bpel/extensions/bpel4restlight", true,
                                           transformationBPELPlan);

        // set instance ids for relationships and nodes
        addNodeRelationInstanceVariables(transformationBPELPlan, serviceTemplate, serviceTemplate);

        // generate variables for properties
        LOG.debug("Initializing properties for {} source NodeTemplates and {} source RelationshipTemplates.",
                  sourceNodeTemplates.size(), sourceRelationshipTemplates.size());
        final Property2VariableMapping sourcesProp2VarMap =
            this.propertyInitializer.initializePropertiesAsVariables(transformationBPELPlan, serviceTemplate,
                                                                     sourceNodeTemplates, sourceRelationshipTemplates);

        LOG.debug("Initializing properties for {} target NodeTemplates and {} target RelationshipTemplates.",
                  targetNodeTemplates.size(), targetRelationshipTemplates.size());
        final Property2VariableMapping targetsProp2VarMap =
            this.propertyInitializer.initializePropertiesAsVariables(transformationBPELPlan, serviceTemplate,
                                                                     targetNodeTemplates, targetRelationshipTemplates);

        if (planType.equals(PlanType.BUILD)) {
            // initialize instanceData handling for build plans
            this.serviceInstanceHandler.appendCreateServiceInstanceVarsAndAnitializeWithInstanceDataAPI(transformationBPELPlan);
        } else {
            // add required variables that are initialized by using instance data
            this.serviceInstanceHandler.addServiceInstanceURLVariable(transformationBPELPlan);
            this.serviceInstanceHandler.addServiceTemplateURLVariable(transformationBPELPlan);
            this.serviceInstanceHandler.addServiceInstanceIDVariable(transformationBPELPlan);
        }

        // add correlation id and handling for input and output
        this.correlationHandler.addCorrellationID(transformationBPELPlan);

        // service instance handling
        final String sourceServiceInstancesURL =
            this.serviceInstanceHandler.addInstanceDataAPIURLVariable(transformationBPELPlan);

        final String serviceTemplateURL =
            this.serviceInstanceHandler.findServiceTemplateUrlVariableName(transformationBPELPlan);

        final String serviceInstanceID =
            this.serviceInstanceHandler.findServiceInstanceIdVarName(transformationBPELPlan);

        final String serviceInstanceURL =
            this.serviceInstanceHandler.findServiceInstanceUrlVariableName(transformationBPELPlan);

        // handle source instance information, e.g., load instance url/, template url and properties
        // append reading source service instance from input and setting created variables
        if (!planType.equals(PlanType.BUILD)) {
            final String planInstanceURL =
                this.serviceInstanceHandler.addPlanInstanceURLVariable(transformationBPELPlan);
            this.serviceInstanceHandler.addServiceInstanceHandlingFromInput(transformationBPELPlan,
                                                                            sourceServiceInstancesURL,
                                                                            serviceInstanceURL, serviceTemplateURL,
                                                                            serviceInstanceID, planInstanceURL);
        }

        // load nodeTemplate properties from source service instance
        final Collection<BPELScope> terminationScopes = getTerminationScopes(transformationBPELPlan);
        this.serviceInstanceHandler.appendInitPropertyVariablesFromServiceInstanceData(transformationBPELPlan,
                                                                                       sourcesProp2VarMap,
                                                                                       serviceTemplateURL,
                                                                                       terminationScopes,
                                                                                       serviceTemplate,
                                                                                       "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED");

        // return created service instance
        this.serviceInstanceHandler.appendAssignServiceInstanceIdToOutput(transformationBPELPlan, serviceInstanceID);

        // we need only input for instances that will be created in the target, deleted or migrated node
        // instances should never get data from the input
        this.emptyPropInit.initializeEmptyPropertiesAsInputParam(getProvisioningScopes(transformationBPELPlan),
                                                                 transformationBPELPlan, targetsProp2VarMap,
                                                                 serviceInstanceURL, serviceInstanceID,
                                                                 serviceTemplateURL, serviceTemplate, csarName);

        for (final BPELScope scope : terminationScopes) {
            if (scope.getNodeTemplate() != null) {
                this.nodeRelationInstanceHandler.addNodeInstanceFindLogic(scope, serviceTemplateURL,
                                                                          "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED",
                                                                          serviceTemplate);
                this.nodeRelationInstanceHandler.addPropertyVariableUpdateBasedOnNodeInstanceID(scope,
                                                                                                sourcesProp2VarMap,
                                                                                                serviceTemplate);
            } else {
                this.nodeRelationInstanceHandler.addRelationInstanceFindLogic(scope, serviceTemplateURL,
                                                                              "?state=CREATED&amp;state=INITIAL",
                                                                              serviceTemplate);

                this.nodeRelationInstanceHandler.addPropertyVariableUpdateBasedOnRelationInstanceID(scope,
                                                                                                    sourcesProp2VarMap,
                                                                                                    serviceTemplate);
            }
        }

        for (final BPELScope scope : getMigrationScopes(transformationBPELPlan)) {
            if (scope.getNodeTemplate() != null) {
                this.nodeRelationInstanceHandler.addNodeInstanceFindLogic(scope, serviceTemplateURL,
                                                                          "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED",
                                                                          serviceTemplate);
                this.nodeRelationInstanceHandler.addPropertyVariableUpdateBasedOnNodeInstanceID(scope,
                                                                                                sourcesProp2VarMap,
                                                                                                serviceTemplate);
            } else {
                this.nodeRelationInstanceHandler.addRelationInstanceFindLogic(scope, serviceTemplateURL,
                                                                              "?state=CREATED&amp;state=INITIAL",
                                                                              serviceTemplate);

                this.nodeRelationInstanceHandler.addPropertyVariableUpdateBasedOnRelationInstanceID(scope,
                                                                                                    sourcesProp2VarMap,
                                                                                                    serviceTemplate);
            }
        }

        // handle output for build plans
        if (planType.equals(PlanType.BUILD)) {
            this.propertyOutputInitializer.initializeBuildPlanOutput(definitions, transformationBPELPlan,
                                                                     targetsProp2VarMap, serviceTemplate);
        }

        runPlugins(transformationBPELPlan, sourcesProp2VarMap, targetsProp2VarMap, csarName, serviceTemplate,
                   serviceInstanceURL, serviceInstanceID, serviceTemplateURL, csarName, serviceTemplate,
                   serviceInstanceURL, serviceInstanceID, serviceTemplateURL);


        this.serviceInstanceHandler.appendSetServiceInstanceState(transformationBPELPlan,
                                                                  transformationBPELPlan.getBpelMainFlowElement(),
                                                                  "ADAPTING", serviceInstanceURL);

        if (planType.equals(PlanType.TERMINATE)) {
            this.serviceInstanceHandler.appendSetServiceInstanceState(transformationBPELPlan,
                                                                      transformationBPELPlan.getBpelMainSequenceOutputAssignElement(),
                                                                      ServiceTemplateInstanceState.DELETED.toString(),
                                                                      serviceInstanceURL);
        } else {
            this.serviceInstanceHandler.appendSetServiceInstanceState(transformationBPELPlan,
                                                                      transformationBPELPlan.getBpelMainSequenceOutputAssignElement(),
                                                                      ServiceTemplateInstanceState.CREATED.toString(),
                                                                      serviceInstanceURL);
        }

        this.serviceInstanceHandler.appendSetServiceInstanceStateAsChild(transformationBPELPlan,
                                                                         this.planHandler.getMainCatchAllFaultHandlerSequenceElement(transformationBPELPlan),
                                                                         ServiceTemplateInstanceState.ERROR.toString(),
                                                                         serviceInstanceURL);
        this.serviceInstanceHandler.appendSetServiceInstanceStateAsChild(transformationBPELPlan,
                                                                         this.planHandler.getMainCatchAllFaultHandlerSequenceElement(transformationBPELPlan),
                                                                         ServiceTemplateInstanceState.FAILED.toString(),
                                                                         this.serviceInstanceHandler.findPlanInstanceUrlVariableName(transformationBPELPlan));

        this.finalizer.finalize(transformationBPELPlan);

        // iterate over terminated nodes and create for each loop per instance
        for (final BPELScope scope : terminationScopes) {
            if (scope.getNodeTemplate() != null) {
                final BPELPlanContext context = new BPELPlanContext(transformationBPELPlan, scope, sourcesProp2VarMap,
                    transformationBPELPlan.getServiceTemplate(), serviceInstanceURL, serviceInstanceID,
                    serviceTemplateURL, csarName);
                this.nodeRelationInstanceHandler.appendCountInstancesLogic(context, scope.getNodeTemplate(),
                                                                           "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED");
            } else {
                final BPELPlanContext context = new BPELPlanContext(transformationBPELPlan, scope, sourcesProp2VarMap,
                    transformationBPELPlan.getServiceTemplate(), serviceInstanceURL, serviceInstanceID,
                    serviceTemplateURL, csarName);
                this.nodeRelationInstanceHandler.appendCountInstancesLogic(context, scope.getRelationshipTemplate(),
                                                                           "?state=CREATED&amp;state=INITIAL");
            }
        }

        return transformationBPELPlan;
    }

    @Override
    public BPELPlan buildPlan(final PlanType planType, final String sourceCsarName,
                              final AbstractDefinitions sourceDefinitions, final QName sourceServiceTemplateId,
                              final String targetCsarName, final AbstractDefinitions targetDefinitions,
                              final QName targetServiceTemplateId) {

        AbstractServiceTemplate sourceServiceTemplate = null;
        AbstractServiceTemplate targetServiceTemplate = null;
        sourceServiceTemplate = getServiceTemplate(sourceDefinitions, sourceServiceTemplateId);
        targetServiceTemplate = getServiceTemplate(targetDefinitions, targetServiceTemplateId);

        // generate abstract plan
        final AbstractTransformationPlan transformationPlan =
            this.generateTFOG(planType, sourceCsarName, sourceDefinitions, sourceServiceTemplate, targetCsarName,
                              targetDefinitions, targetServiceTemplate);


        // transform to bpel skeleton
        final String processName = ModelUtils.makeValidNCName(sourceServiceTemplate.getId() + "_transformTo_"
            + targetServiceTemplate.getId() + "_plan");
        final String processNamespace = sourceServiceTemplate.getTargetNamespace() + "_transformPlan";

        final BPELPlan transformationBPELPlan =
            this.planHandler.createEmptyBPELPlan(processNamespace, processName, transformationPlan, "transform");



        transformationBPELPlan.setTOSCAInterfaceName("OpenTOSCA-Transformation-Interface");
        transformationBPELPlan.setTOSCAOperationname("transform");

        this.planHandler.initializeBPELSkeleton(transformationBPELPlan, sourceCsarName);
        // instanceDataAPI handling is done solely trough this extension
        this.planHandler.registerExtension("http://www.apache.org/ode/bpel/extensions/bpel4restlight", true,
                                           transformationBPELPlan);

        // set instance ids for relationships and nodes
        addNodeRelationInstanceVariables(transformationBPELPlan, sourceServiceTemplate, targetServiceTemplate);

        // generate variables for properties
        final Property2VariableMapping sourcePropMap =
            this.propertyInitializer.initializePropertiesAsVariables(transformationBPELPlan, sourceServiceTemplate,
                                                                     transformationPlan.getHandledSourceServiceTemplateNodes(),
                                                                     transformationPlan.getHandledSourceServiceTemplateRelations());
        final Property2VariableMapping targetPropMap =
            this.propertyInitializer.initializePropertiesAsVariables(transformationBPELPlan, targetServiceTemplate,
                                                                     transformationPlan.getHandledTargetServiceTemplateNodes(),
                                                                     transformationPlan.getHandledTargetServiceTemplateRelations());

        // add correlation id and handling for input and output
        this.correlationHandler.addCorrellationID(transformationBPELPlan);

        // service instance handling
        final String sourceServiceInstancesURL =
            this.serviceInstanceHandler.addInstanceDataAPIURLVariable(transformationBPELPlan);
        final String targetServiceInstancesURL =
            this.serviceInstanceHandler.addInstanceDataAPIURLVariable(transformationBPELPlan);
        final String sourceServiceTemplateURL =
            this.serviceInstanceHandler.addServiceTemplateURLVariable(transformationBPELPlan);
        final String targetServiceTemplateURL =
            this.serviceInstanceHandler.addServiceTemplateURLVariable(transformationBPELPlan);
        final String sourceServiceInstanceID =
            this.serviceInstanceHandler.addServiceInstanceIDVariable(transformationBPELPlan);
        final String targetServiceInstanceID =
            this.serviceInstanceHandler.addServiceInstanceIDVariable(transformationBPELPlan);
        final String sourceServiceInstanceURL =
            this.serviceInstanceHandler.addServiceInstanceURLVariable(transformationBPELPlan);
        final String targetServiceInstanceURL =
            this.serviceInstanceHandler.addServiceInstanceURLVariable(transformationBPELPlan);

        final String planInstanceURL = this.serviceInstanceHandler.addPlanInstanceURLVariable(transformationBPELPlan);

        // handle sourceinstance information, e.g., load instance url/, template url and
        // properties
        // append reading source service instance from input and setting created
        // variables
        this.serviceInstanceHandler.addServiceInstanceHandlingFromInput(transformationBPELPlan,
                                                                        sourceServiceInstancesURL,
                                                                        sourceServiceInstanceURL,
                                                                        sourceServiceTemplateURL,
                                                                        sourceServiceInstanceID, planInstanceURL);

        // load nodeTemplate properties from source service instance
        final Collection<BPELScope> terminationScopes = getTerminationScopes(transformationBPELPlan);
        this.serviceInstanceHandler.appendInitPropertyVariablesFromServiceInstanceData(transformationBPELPlan,
                                                                                       sourcePropMap,
                                                                                       sourceServiceTemplateURL,
                                                                                       terminationScopes,
                                                                                       sourceServiceTemplate,
                                                                                       "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED");

        // handle target service instance information
        this.serviceInstanceHandler.initServiceInstancesURLVariableFromAvailableServiceInstanceUrlVar(transformationBPELPlan,
                                                                                                      sourceServiceInstancesURL,
                                                                                                      targetServiceTemplateId,
                                                                                                      targetCsarName,
                                                                                                      targetServiceInstancesURL);
        // create service instance for target
        this.serviceInstanceHandler.appendCreateServiceInstance(transformationBPELPlan, targetServiceInstancesURL,
                                                                targetServiceInstanceURL, targetServiceInstanceID,
                                                                targetServiceTemplateURL, planInstanceURL, true);

        // return created service instance
        this.serviceInstanceHandler.appendAssignServiceInstanceIdToOutput(transformationBPELPlan,
                                                                          targetServiceInstanceID);

        // we need only input for instances that will be created in the target, deleted or migrated node
        // instances should never get data from the input
        this.emptyPropInit.initializeEmptyPropertiesAsInputParam(getProvisioningScopes(transformationBPELPlan),
                                                                 transformationBPELPlan, targetPropMap,
                                                                 targetServiceInstanceURL, targetServiceInstanceID,
                                                                 targetServiceTemplateURL, targetServiceTemplate,
                                                                 targetCsarName);

        for (final BPELScope scope : terminationScopes) {
            if (scope.getNodeTemplate() != null) {
                this.nodeRelationInstanceHandler.addNodeInstanceFindLogic(scope, sourceServiceTemplateURL,
                                                                          "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED",
                                                                          sourceServiceTemplate);
                this.nodeRelationInstanceHandler.addPropertyVariableUpdateBasedOnNodeInstanceID(scope, sourcePropMap,
                                                                                                sourceServiceTemplate);
            } else {
                this.nodeRelationInstanceHandler.addRelationInstanceFindLogic(scope, sourceServiceTemplateURL,
                                                                              "?state=CREATED&amp;state=INITIAL",
                                                                              sourceServiceTemplate);


                this.nodeRelationInstanceHandler.addPropertyVariableUpdateBasedOnRelationInstanceID(scope,
                                                                                                    sourcePropMap,
                                                                                                    sourceServiceTemplate);
            }
        }

        for (final BPELScope scope : getMigrationScopes(transformationBPELPlan)) {
            if (scope.getNodeTemplate() != null) {
                this.nodeRelationInstanceHandler.addNodeInstanceFindLogic(scope, sourceServiceTemplateURL,
                                                                          "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED",
                                                                          sourceServiceTemplate);
                this.nodeRelationInstanceHandler.addPropertyVariableUpdateBasedOnNodeInstanceID(scope, sourcePropMap,
                                                                                                sourceServiceTemplate);
            } else {
                this.nodeRelationInstanceHandler.addRelationInstanceFindLogic(scope, sourceServiceTemplateURL,
                                                                              "?state=CREATED&amp;state=INITIAL",
                                                                              sourceServiceTemplate);

                this.nodeRelationInstanceHandler.addPropertyVariableUpdateBasedOnRelationInstanceID(scope,
                                                                                                    sourcePropMap,
                                                                                                    sourceServiceTemplate);
            }
        }

        runPlugins(transformationBPELPlan, sourcePropMap, targetPropMap, sourceCsarName, sourceServiceTemplate,
                   sourceServiceInstanceURL, sourceServiceInstanceID, sourceServiceTemplateURL, targetCsarName,
                   targetServiceTemplate, targetServiceInstanceURL, targetServiceInstanceID, targetServiceTemplateURL);


        this.serviceInstanceHandler.appendSetServiceInstanceState(transformationBPELPlan,
                                                                  transformationBPELPlan.getBpelMainFlowElement(),
                                                                  ServiceTemplateInstanceState.MIGRATING.toString(),
                                                                  sourceServiceInstanceURL);
        this.serviceInstanceHandler.appendSetServiceInstanceState(transformationBPELPlan,
                                                                  transformationBPELPlan.getBpelMainFlowElement(),
                                                                  ServiceTemplateInstanceState.CREATING.toString(),
                                                                  targetServiceInstanceURL);
        this.serviceInstanceHandler.appendSetServiceInstanceState(transformationBPELPlan,
                                                                  transformationBPELPlan.getBpelMainSequenceOutputAssignElement(),
                                                                  ServiceTemplateInstanceState.MIGRATED.toString(),
                                                                  sourceServiceInstanceURL);
        this.serviceInstanceHandler.appendSetServiceInstanceState(transformationBPELPlan,
                                                                  transformationBPELPlan.getBpelMainSequenceOutputAssignElement(),
                                                                  ServiceTemplateInstanceState.CREATED.toString(),
                                                                  targetServiceInstanceURL);

        this.finalizer.finalize(transformationBPELPlan);

        // iterate over terminated nodes and create for each loop per instance
        for (final BPELScope scope : terminationScopes) {
            if (scope.getNodeTemplate() != null) {
                final BPELPlanContext context = new BPELPlanContext(transformationBPELPlan, scope, sourcePropMap,
                    transformationBPELPlan.getServiceTemplate(), sourceServiceInstanceURL, sourceServiceInstanceID,
                    sourceServiceTemplateURL, sourceCsarName);
                this.nodeRelationInstanceHandler.appendCountInstancesLogic(context, scope.getNodeTemplate(),
                                                                           "?state=STARTED&amp;state=CREATED&amp;state=CONFIGURED");
            } else {
                final BPELPlanContext context = new BPELPlanContext(transformationBPELPlan, scope, sourcePropMap,
                    transformationBPELPlan.getServiceTemplate(), sourceServiceInstanceURL, sourceServiceInstanceID,
                    sourceServiceTemplateURL, sourceCsarName);
                this.nodeRelationInstanceHandler.appendCountInstancesLogic(context, scope.getRelationshipTemplate(),
                                                                           "?state=CREATED&amp;state=INITIAL");
            }
        }

        return transformationBPELPlan;
    }

    private Collection<BPELScope> getMigrationScopes(final BPELPlan plan) {
        return getScopesByType(plan, ActivityType.MIGRATION);
    }

    private Collection<BPELScope> getScopesByType(final BPELPlan plan, final ActivityType type) {
        final Collection<BPELScope> scopes = new HashSet<>();
        for (final AbstractActivity act : plan.getAbstract2BPEL().keySet()) {
            if (act.getType().equals(type)) {
                scopes.add(plan.getAbstract2BPEL().get(act));
            }
        }
        return scopes;
    }

    private Collection<BPELScope> getTerminationScopes(final BPELPlan plan) {
        return getScopesByType(plan, ActivityType.TERMINATION);
    }


    private Collection<BPELScope> getProvisioningScopes(final BPELPlan plan) {
        return getScopesByType(plan, ActivityType.PROVISIONING);
    }

    private AbstractServiceTemplate getServiceTemplate(final AbstractDefinitions defs, final QName serviceTemplateId) {
        for (final AbstractServiceTemplate servTemplate : defs.getServiceTemplates()) {
            if (servTemplate.getQName().equals(serviceTemplateId)) {
                return servTemplate;
            }
        }
        return null;
    }

    private void addNodeRelationInstanceVariables(final BPELPlan plan,
                                                  final AbstractServiceTemplate sourceServiceTemplate,
                                                  final AbstractServiceTemplate targetServiceTemplate) {
        for (final BPELScope scope : getTerminationScopes(plan)) {
            boolean added =
                this.nodeRelationInstanceHandler.addInstanceIDVarToTemplatePlan(scope, sourceServiceTemplate);
            while (!added) {
                added = this.nodeRelationInstanceHandler.addInstanceIDVarToTemplatePlan(scope, sourceServiceTemplate);
            }

            added = this.nodeRelationInstanceHandler.addInstanceURLVarToTemplatePlan(scope, sourceServiceTemplate);
            while (!added) {
                added = this.nodeRelationInstanceHandler.addInstanceURLVarToTemplatePlan(scope, sourceServiceTemplate);
            }
        }

        for (final BPELScope scope : getProvisioningScopes(plan)) {
            boolean added =
                this.nodeRelationInstanceHandler.addInstanceIDVarToTemplatePlan(scope, targetServiceTemplate);
            while (!added) {
                added = this.nodeRelationInstanceHandler.addInstanceIDVarToTemplatePlan(scope, targetServiceTemplate);
            }

            added = this.nodeRelationInstanceHandler.addInstanceURLVarToTemplatePlan(scope, targetServiceTemplate);
            while (!added) {
                added = this.nodeRelationInstanceHandler.addInstanceURLVarToTemplatePlan(scope, targetServiceTemplate);
            }
        }

        for (final BPELScope scope : getMigrationScopes(plan)) {
            boolean added =
                this.nodeRelationInstanceHandler.addInstanceIDVarToTemplatePlan(scope, sourceServiceTemplate);
            while (!added) {
                added = this.nodeRelationInstanceHandler.addInstanceIDVarToTemplatePlan(scope, sourceServiceTemplate);
            }

            added = this.nodeRelationInstanceHandler.addInstanceURLVarToTemplatePlan(scope, sourceServiceTemplate);
            while (!added) {
                added = this.nodeRelationInstanceHandler.addInstanceURLVarToTemplatePlan(scope, sourceServiceTemplate);
            }

            added = this.nodeRelationInstanceHandler.addInstanceIDVarToTemplatePlan(scope, targetServiceTemplate);
            while (!added) {
                added = this.nodeRelationInstanceHandler.addInstanceIDVarToTemplatePlan(scope, targetServiceTemplate);
            }

            added = this.nodeRelationInstanceHandler.addInstanceURLVarToTemplatePlan(scope, targetServiceTemplate);
            while (!added) {
                added = this.nodeRelationInstanceHandler.addInstanceURLVarToTemplatePlan(scope, targetServiceTemplate);
            }
        }
    }

    @Override
    public List<AbstractPlan> buildPlans(final String sourceCsarName, final AbstractDefinitions sourceDefinitions,
                                         final String targetCsarName, final AbstractDefinitions targetDefinitions) {
        // TODO Auto-generated method stub
        return null;
    }

    private void runPlugins(final BPELPlan buildPlan, final Property2VariableMapping sourceServiceTemplateMap,
                            final Property2VariableMapping targetServiceTemplateMap, final String sourceCsarName,
                            final AbstractServiceTemplate sourceServiceTemplate, final String sourceServiceInstanceUrl,
                            final String sourceServiceInstanceId, final String sourceServiceTemplateUrl,
                            final String targetCsarName, final AbstractServiceTemplate targetServiceTemplate,
                            final String targetServiceInstanceUrl, final String targetServiceInstanceId,
                            final String targetServiceTemplateUrl) {

        for (final BPELScope bpelScope : buildPlan.getTemplateBuildPlans()) {

            if (bpelScope.getNodeTemplate() != null) {

                final AbstractActivity activity = bpelScope.getActivity();

                if (activity.getType().equals(ActivityType.PROVISIONING)) {
                    final BPELPlanContext context = new BPELPlanContext(buildPlan, bpelScope, targetServiceTemplateMap,
                        targetServiceTemplate, targetServiceInstanceUrl, targetServiceInstanceId,
                        targetServiceTemplateUrl, targetCsarName);
                    this.bpelPluginHandler.handleActivity(context, bpelScope, bpelScope.getNodeTemplate());
                } else if (activity.getType().equals(ActivityType.TERMINATION)) {
                    final BPELPlanContext context = new BPELPlanContext(buildPlan, bpelScope, sourceServiceTemplateMap,
                        sourceServiceTemplate, sourceServiceInstanceUrl, sourceServiceInstanceId,
                        sourceServiceTemplateUrl, sourceCsarName);
                    this.bpelPluginHandler.handleActivity(context, bpelScope, bpelScope.getNodeTemplate());
                } else if (activity.getType().equals(ActivityType.MIGRATION)) {

                    final AbstractNodeTemplate sourceNodeTemplate = bpelScope.getNodeTemplate();
                    final AbstractNodeTemplate targetNodeTemplate =
                        getCorrespondingNode(bpelScope.getNodeTemplate(),
                                             targetServiceTemplate.getTopologyTemplate().getNodeTemplates());


                    final BPELPlanContext sourceContext = new BPELPlanContext(buildPlan, bpelScope,
                        sourceServiceTemplateMap, sourceServiceTemplate, sourceServiceInstanceUrl,
                        sourceServiceInstanceId, sourceServiceTemplateUrl, sourceCsarName);

                    final BPELPlanContext targetContext = new BPELPlanContext(buildPlan, bpelScope,
                        targetServiceTemplateMap, targetServiceTemplate, targetServiceInstanceUrl,
                        targetServiceInstanceId, targetServiceTemplateUrl, targetCsarName);

                    for (final IPlanBuilderPostPhasePlugin postPhasePlugin : this.pluginRegistry.getPostPlugins()) {
                        if (postPhasePlugin.canHandleUpdate(sourceNodeTemplate, targetNodeTemplate)) {
                            postPhasePlugin.handleUpdate(sourceContext, targetContext, sourceNodeTemplate,
                                                         targetNodeTemplate);
                        }
                    }

                }
                // if this nodeTemplate has the label running (Property: State=Running), skip
                // provisioning and just generate instance data handlin

                // generate code for the activity
            } else if (bpelScope.getRelationshipTemplate() != null) {
                // handling relationshiptemplate

                final AbstractActivity activity = bpelScope.getActivity();
                if (activity.getType().equals(ActivityType.PROVISIONING)) {
                    final BPELPlanContext context = new BPELPlanContext(buildPlan, bpelScope, targetServiceTemplateMap,
                        targetServiceTemplate, targetServiceInstanceUrl, targetServiceInstanceId,
                        targetServiceTemplateUrl, targetCsarName);
                    this.bpelPluginHandler.handleActivity(context, bpelScope, bpelScope.getRelationshipTemplate());
                } else if (activity.getType().equals(ActivityType.TERMINATION)) {
                    final BPELPlanContext context = new BPELPlanContext(buildPlan, bpelScope, sourceServiceTemplateMap,
                        sourceServiceTemplate, sourceServiceInstanceUrl, sourceServiceInstanceId,
                        sourceServiceTemplateUrl, sourceCsarName);
                    this.bpelPluginHandler.handleActivity(context, bpelScope, bpelScope.getRelationshipTemplate());
                } else if (activity.getType().equals(ActivityType.MIGRATION)) {

                    final AbstractRelationshipTemplate sourceRelationshipTemplate = bpelScope.getRelationshipTemplate();
                    final AbstractRelationshipTemplate targetRelationshipTemplate =
                        getCorrespondingEdge(bpelScope.getRelationshipTemplate(),
                                             targetServiceTemplate.getTopologyTemplate().getRelationshipTemplates());

                    final BPELPlanContext sourceContext = new BPELPlanContext(buildPlan, bpelScope,
                        sourceServiceTemplateMap, sourceServiceTemplate, sourceServiceInstanceUrl,
                        sourceServiceInstanceId, sourceServiceTemplateUrl, sourceCsarName);

                    final BPELPlanContext targetContext = new BPELPlanContext(buildPlan, bpelScope,
                        targetServiceTemplateMap, targetServiceTemplate, targetServiceInstanceUrl,
                        targetServiceInstanceId, targetServiceTemplateUrl, targetCsarName);

                    for (final IPlanBuilderPostPhasePlugin postPhasePlugin : this.pluginRegistry.getPostPlugins()) {
                        if (postPhasePlugin.canHandleUpdate(sourceRelationshipTemplate, targetRelationshipTemplate)) {
                            postPhasePlugin.handleUpdate(sourceContext, targetContext, sourceRelationshipTemplate,
                                                         targetRelationshipTemplate);
                        }
                    }
                }
            }
        }
    }
}
