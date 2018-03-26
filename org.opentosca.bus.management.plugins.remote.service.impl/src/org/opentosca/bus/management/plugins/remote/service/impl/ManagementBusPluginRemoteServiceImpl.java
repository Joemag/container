package org.opentosca.bus.management.plugins.remote.service.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.FilenameUtils;
import org.opentosca.bus.management.header.MBHeader;
import org.opentosca.bus.management.plugins.remote.service.impl.servicehandler.ServiceHandler;
import org.opentosca.bus.management.plugins.remote.service.impl.typeshandler.ArtifactTypesHandler;
import org.opentosca.bus.management.plugins.service.IManagementBusPluginService;
import org.opentosca.bus.management.utils.MBUtils;
import org.opentosca.container.core.common.Settings;
import org.opentosca.container.core.engine.ResolvedArtifacts;
import org.opentosca.container.core.engine.ResolvedArtifacts.ResolvedDeploymentArtifact;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.tosca.convention.Interfaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Management Bus-Plug-in for remoteIAs.<br>
 * <br>
 *
 *
 *
 * The Plugin gets needed information from the ManagementBus and is responsible to handle "remote IAs".
 * Remote IAs are IAs such as scripts that needs to be executed on the host machine. Therefore this
 * plugin also is responsible for the uploading of the files and the installation of required packages
 * on the target machine (if specified).
 *
 *
 *
 * @author Michael Zimmermann - michael.zimmermann@iaas.uni-stuttgart.de
 *
 *
 */
public class ManagementBusPluginRemoteServiceImpl implements IManagementBusPluginService {

    final private static String PLACEHOLDER_TARGET_FILE_PATH = "{TARGET_FILE_PATH}";
    final private static String PLACEHOLDER_TARGET_FILE_FOLDER_PATH = "{TARGET_FILE_FOLDER_PATH}";
    final private static String PLACEHOLDER_TARGET_FILE_NAME_WITH_EXTENSION = "{TARGET_FILE_NAME_WITH_E}";
    final private static String PLACEHOLDER_TARGET_FILE_NAME_WITHOUT_EXTENSION = "{TARGET_FILE_NAME_WITHOUT_E}";
    final private static String PLACEHOLDER_DA_NAME_PATH_MAP = "{DA_NAME_PATH_MAP}";
    final private static String PLACEHOLDER_DA_INPUT_PARAMETER = "{INPUT_PARAMETER}";

    final private static String RUN_SCRIPT_OUTPUT_PARAMETER_NAME = "ScriptResult";

    final private static Logger LOG = LoggerFactory.getLogger(ManagementBusPluginRemoteServiceImpl.class);


    @Override
    public Exchange invoke(final Exchange exchange) {

        final Message message = exchange.getIn();

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Management Bus Remote Plugin getting information...");

        final CSARID csarID = message.getHeader(MBHeader.CSARID.toString(), CSARID.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("CsarID: {}", csarID);
        final QName artifactTemplateID = message.getHeader(MBHeader.ARTIFACTTEMPLATEID_QNAME.toString(), QName.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("ArtifactTemplateID: {}", artifactTemplateID);
        String nodeTemplateID = message.getHeader(MBHeader.NODETEMPLATEID_STRING.toString(), String.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("NodeTemplateID: {}", nodeTemplateID);
        final String relationshipTemplateID =
            message.getHeader(MBHeader.RELATIONSHIPTEMPLATEID_STRING.toString(), String.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("RelationshipTemplateID: {}", relationshipTemplateID);
        final QName serviceTemplateID = message.getHeader(MBHeader.SERVICETEMPLATEID_QNAME.toString(), QName.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("ServiceTemplateID: {}", serviceTemplateID);
        final QName nodeTypeID = message.getHeader(MBHeader.NODETYPEID_QNAME.toString(), QName.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("NodeTypeID: {}", nodeTypeID);
        final QName relationshipTypeID = message.getHeader(MBHeader.RELATIONSHIPTYPEID_QNAME.toString(), QName.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("RelationshipTypeID: {}", relationshipTypeID);
        final String interfaceName = message.getHeader(MBHeader.INTERFACENAME_STRING.toString(), String.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("InterfaceName: {}", interfaceName);
        final String operationName = message.getHeader(MBHeader.OPERATIONNAME_STRING.toString(), String.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("OperationName: {}", operationName);
        final URI serviceInstanceID = message.getHeader(MBHeader.SERVICEINSTANCEID_URI.toString(), URI.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("ServiceInstanceID: {}", serviceInstanceID);
        final String nodeInstanceID = message.getHeader(MBHeader.NODEINSTANCEID_STRING.toString(), String.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("NodeInstanceID: {}", nodeInstanceID);
        final String publicIP = message.getHeader(MBHeader.OPENTOSCA_PUBLIC_IP.toString(), String.class);
        ManagementBusPluginRemoteServiceImpl.LOG.debug("OPENTOSCA_PUBLIC_IP: {}", publicIP);

        if (nodeTemplateID == null && relationshipTemplateID != null) {

            final boolean isBoundToSourceNode =
                ServiceHandler.toscaEngineService.isOperationOfRelationshipBoundToSourceNode(csarID, relationshipTypeID,
                                                                                             interfaceName, operationName);

            if (isBoundToSourceNode) {
                nodeTemplateID =
                    ServiceHandler.toscaEngineService.getSourceNodeTemplateIDOfRelationshipTemplate(csarID,
                                                                                                    serviceTemplateID,
                                                                                                    relationshipTemplateID);
            } else {
                nodeTemplateID =
                    ServiceHandler.toscaEngineService.getTargetNodeTemplateIDOfRelationshipTemplate(csarID,
                                                                                                    serviceTemplateID,
                                                                                                    relationshipTemplateID);
            }
        }


        // Determine output parameters of the current operation
        final List<String> outputParameters = new LinkedList<>();
        final boolean hasOutputParams =
            ServiceHandler.toscaEngineService.hasOperationOfANodeTypeSpecifiedOutputParams(csarID, nodeTypeID,
                                                                                           interfaceName, operationName);
        if (hasOutputParams) {
            final Node outputParametersNode =
                ServiceHandler.toscaEngineService.getOutputParametersOfANodeTypeOperation(csarID, nodeTypeID,
                                                                                          interfaceName, operationName);
            if (outputParametersNode != null) {
                final NodeList children = outputParametersNode.getChildNodes();

                for (int i = 0; i < children.getLength(); i++) {
                    final Node child = children.item(i);

                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        final String name = ((Element) child).getAttribute("name");
                        outputParameters.add(name);
                    }
                }
            }
        }
        for (final String param : outputParameters) {
            ManagementBusPluginRemoteServiceImpl.LOG.debug("Output parameter: {}", param);
        }

        final QName artifactType =
            ServiceHandler.toscaEngineService.getArtifactTypeOfArtifactTemplate(csarID, artifactTemplateID);

        ManagementBusPluginRemoteServiceImpl.LOG.debug("ArtifactType of ArtifactTemplate {} : {}", artifactTemplateID,
                                                       artifactType);

        if (artifactType != null && nodeTemplateID != null) {

            // search operating system ia to upload files and run scripts on
            // target
            // machine
            final String osNodeTemplateID =
                MBUtils.getOperatingSystemNodeTemplateID(csarID, serviceTemplateID, nodeTemplateID);

            if (osNodeTemplateID != null) {
                final QName osNodeTypeID =
                    ServiceHandler.toscaEngineService.getNodeTypeOfNodeTemplate(csarID, serviceTemplateID,
                                                                                osNodeTemplateID);

                if (osNodeTypeID != null) {
                    ManagementBusPluginRemoteServiceImpl.LOG.debug("OperatingSystem-NodeType found: {}", osNodeTypeID);
                    final String osIAName = MBUtils.getOperatingSystemIA(csarID, serviceTemplateID, osNodeTemplateID);

                    if (osIAName != null) {

                        final Object params = message.getBody();

                        // create headers
                        final HashMap<String, Object> headers = new HashMap<>();

                        headers.put(MBHeader.CSARID.toString(), csarID);
                        headers.put(MBHeader.SERVICETEMPLATEID_QNAME.toString(), serviceTemplateID);
                        headers.put(MBHeader.NODETEMPLATEID_STRING.toString(), osNodeTemplateID);
                        headers.put(MBHeader.INTERFACENAME_STRING.toString(),
                                    MBUtils.getInterfaceForOperatingSystemNodeType(csarID, osNodeTypeID));
                        headers.put(MBHeader.SERVICEINSTANCEID_URI.toString(), serviceInstanceID);
                        headers.put(MBHeader.OPENTOSCA_PUBLIC_IP.toString(), publicIP);

                        // install packages
                        ManagementBusPluginRemoteServiceImpl.LOG.debug("Installing packages...");

                        installPackages(artifactType, headers);

                        ManagementBusPluginRemoteServiceImpl.LOG.debug("Packages installed.");

                        // upload files
                        ManagementBusPluginRemoteServiceImpl.LOG.debug("Uploading files...");

                        final List<String> artifactReferences =
                            ServiceHandler.toscaEngineService.getArtifactReferenceWithinArtifactTemplate(csarID,
                                                                                                         artifactTemplateID);

                        String fileSource;
                        String targetFilePath = null;
                        String targetFileFolderPath = null;

                        for (final String artifactRef : artifactReferences) {

                            fileSource =
                                Settings.CONTAINER_API + "/csars/" + csarID.getFileName() + "/content/" + artifactRef;


                            targetFilePath = "~/" + csarID.getFileName() + "/" + artifactRef;

                            targetFileFolderPath = FilenameUtils.getFullPathNoEndSeparator(targetFilePath);

                            final String createDirCommand = "sleep 1 && mkdir -p " + targetFileFolderPath;

                            // create directory before uploading file
                            runScript(createDirCommand, headers);

                            // upload file
                            transferFile(csarID, artifactTemplateID, fileSource, targetFilePath, headers);
                        }

                        ManagementBusPluginRemoteServiceImpl.LOG.debug("Files uploaded.");

                        // run script
                        ManagementBusPluginRemoteServiceImpl.LOG.debug("Running scripts...");

                        final String fileNameWithE = FilenameUtils.getName(targetFilePath);
                        final String fileNameWithoutE = FilenameUtils.getBaseName(targetFilePath);

                        String artifactTypeSpecificCommand =
                            createArtifcatTypeSpecificCommandString(csarID, artifactType, artifactTemplateID, params);

                        ManagementBusPluginRemoteServiceImpl.LOG.debug("Replacing further generic placeholder...");

                        // replace placeholders
                        artifactTypeSpecificCommand =
                            artifactTypeSpecificCommand.replace(ManagementBusPluginRemoteServiceImpl.PLACEHOLDER_TARGET_FILE_PATH,
                                                                targetFilePath);
                        artifactTypeSpecificCommand =
                            artifactTypeSpecificCommand.replace(ManagementBusPluginRemoteServiceImpl.PLACEHOLDER_TARGET_FILE_FOLDER_PATH,
                                                                targetFileFolderPath);
                        artifactTypeSpecificCommand =
                            artifactTypeSpecificCommand.replace(ManagementBusPluginRemoteServiceImpl.PLACEHOLDER_TARGET_FILE_NAME_WITH_EXTENSION,
                                                                fileNameWithE);
                        artifactTypeSpecificCommand =
                            artifactTypeSpecificCommand.replace(ManagementBusPluginRemoteServiceImpl.PLACEHOLDER_TARGET_FILE_NAME_WITHOUT_EXTENSION,
                                                                fileNameWithoutE);
                        artifactTypeSpecificCommand =
                            artifactTypeSpecificCommand.replace(ManagementBusPluginRemoteServiceImpl.PLACEHOLDER_DA_NAME_PATH_MAP,
                                                                "sudo -E " + createDANamePathMapEnvVar(csarID,
                                                                                                       serviceTemplateID, nodeTypeID, nodeTemplateID));
                        artifactTypeSpecificCommand =
                            artifactTypeSpecificCommand.replace(ManagementBusPluginRemoteServiceImpl.PLACEHOLDER_DA_INPUT_PARAMETER,
                                                                createParamsString(params, publicIP));

                        ManagementBusPluginRemoteServiceImpl.LOG.debug("Final command for ArtifactType {} : {}",
                                                                       artifactType, artifactTypeSpecificCommand);

                        final Object result = runScript(artifactTypeSpecificCommand, headers);

                        ManagementBusPluginRemoteServiceImpl.LOG.debug("Script execution result: {}", result);

                        ManagementBusPluginRemoteServiceImpl.LOG.debug("Scripts finished.");

                        // create response
                        final Map<String, String> resultMap = new HashMap<>();
                        addOutputParametersToResultMap(resultMap, result, outputParameters);
                        if (resultMap.isEmpty()) {
                            // create dummy response in case there are no output parameters
                            resultMap.put("invocation", "finished");
                        }

                        exchange.getIn().setBody(resultMap);

                    } else {
                        ManagementBusPluginRemoteServiceImpl.LOG.warn("No OperatingSystem-IA found!");
                    }
                } else {
                    ManagementBusPluginRemoteServiceImpl.LOG.warn("No OperatingSystem-NodeType found!");
                }
            } else {
                ManagementBusPluginRemoteServiceImpl.LOG.warn("No OperatingSystem-NodeTemplate found!");
            }
        } else {
            ManagementBusPluginRemoteServiceImpl.LOG.warn("Could not determine ArtifactType of ArtifactTemplate: {}!",
                                                          artifactTemplateID);
        }
        return exchange;
    }

    /**
     * Check if the output parameters for this remote service operation are returned in the script
     * result and add them to the result map.
     *
     * @param resultMap The result map which is returned for the invocation of the remote service
     *        operation
     * @param result The returned result of the run script operation
     * @param outputParameters The output parameters that are expected for the operation
     */
    private void addOutputParametersToResultMap(final Map<String, String> resultMap, final Object result,
                                                final List<String> outputParameters) {

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Adding output parameters to the response message.");

        // process result as HashMap
        if (result instanceof HashMap<?, ?>) {
            final HashMap<?, ?> resultHashMap = (HashMap<?, ?>) result;

            // get ScriptResult part of the response which contains the parameters
            if (resultHashMap.containsKey(ManagementBusPluginRemoteServiceImpl.RUN_SCRIPT_OUTPUT_PARAMETER_NAME)) {
                final Object scriptResult =
                    resultHashMap.get(ManagementBusPluginRemoteServiceImpl.RUN_SCRIPT_OUTPUT_PARAMETER_NAME);

                if (scriptResult != null) {
                    final String scriptResultString = scriptResult.toString();

                    ManagementBusPluginRemoteServiceImpl.LOG.debug("{}: {}",
                                                                   ManagementBusPluginRemoteServiceImpl.RUN_SCRIPT_OUTPUT_PARAMETER_NAME,
                                                                   scriptResultString);

                    // split result on line breaks as every parameter is returned in a separate "echo"
                    // command
                    final String[] resultParameters = scriptResultString.split("[\\r\\n]+");

                    // add each parameter that is defined in the operation and passed back
                    for (final String resultParameter : resultParameters) {
                        for (final String outputParameter : outputParameters) {
                            if (resultParameter.startsWith(outputParameter)) {
                                final String value = resultParameter.substring(resultParameter.indexOf("=") + 1);

                                ManagementBusPluginRemoteServiceImpl.LOG.debug("Adding parameter {} with value: {}",
                                                                               outputParameter, value);
                                resultMap.put(outputParameter, value);
                            }
                        }
                    }
                }

            } else {
                ManagementBusPluginRemoteServiceImpl.LOG.warn("Result contains no result entry '{}'",
                                                              ManagementBusPluginRemoteServiceImpl.RUN_SCRIPT_OUTPUT_PARAMETER_NAME);
            }

        } else {
            ManagementBusPluginRemoteServiceImpl.LOG.warn("Result of type {} not supported. The bus should return a HashMap as result class when it is used as input.",
                                                          result.getClass());
        }
    }

    /**
     * @param csarID
     * @param serviceTemplateID
     * @param nodeTypeID
     * @param nodeTemplateID
     *
     * @return mapping with DeploymentArtifact names and their paths.
     */
    private String createDANamePathMapEnvVar(final CSARID csarID, final QName serviceTemplateID, final QName nodeTypeID,
                                             final String nodeTemplateID) {

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Checking if NodeTemplate {} has DAs...", nodeTemplateID);

        final HashMap<String, List<String>> daNameReferenceMapping = new HashMap<>();

        final QName nodeTemplateQName = new QName(serviceTemplateID.getNamespaceURI(), nodeTemplateID);

        final ResolvedArtifacts resolvedArtifacts =
            ServiceHandler.toscaEngineService.getResolvedArtifactsOfNodeTemplate(csarID, nodeTemplateQName);

        final List<ResolvedDeploymentArtifact> resolvedDAs = resolvedArtifacts.getDeploymentArtifacts();

        List<String> daArtifactReferences;

        for (final ResolvedDeploymentArtifact resolvedDA : resolvedDAs) {

            daArtifactReferences = resolvedDA.getReferences();

            for (final String daArtifactReference : daArtifactReferences) {

                ManagementBusPluginRemoteServiceImpl.LOG.debug("Artifact reference for DA: {} found: {} .",
                                                               resolvedDA.getName(), daArtifactReference);

                List<String> currentValue = daNameReferenceMapping.get(resolvedDA.getName());
                if (currentValue == null) {
                    currentValue = new ArrayList<>();
                    daNameReferenceMapping.put(resolvedDA.getName(), currentValue);
                }
                currentValue.add(daArtifactReference);
            }
        }

        final List<QName> nodeTypeImpls =
            ServiceHandler.toscaEngineService.getNodeTypeImplementationsOfNodeType(csarID, nodeTypeID);

        for (final QName nodeTypeImpl : nodeTypeImpls) {
            final List<String> daNames =
                ServiceHandler.toscaEngineService.getDeploymentArtifactNamesOfNodeTypeImplementation(csarID,
                                                                                                     nodeTypeImpl);

            for (final String daName : daNames) {
                final QName daArtifactTemplate =
                    ServiceHandler.toscaEngineService.getArtifactTemplateOfADeploymentArtifactOfANodeTypeImplementation(csarID,
                                                                                                                        nodeTypeImpl, daName);

                daArtifactReferences =
                    ServiceHandler.toscaEngineService.getArtifactReferenceWithinArtifactTemplate(csarID,
                                                                                                 daArtifactTemplate);

                for (final String daArtifactReference : daArtifactReferences) {

                    ManagementBusPluginRemoteServiceImpl.LOG.debug("Artifact reference for DA: {} found: {} .", daName,
                                                                   daArtifactReference);

                    List<String> currentValue = daNameReferenceMapping.get(daName);
                    if (currentValue == null) {
                        currentValue = new ArrayList<>();
                        daNameReferenceMapping.put(daName, currentValue);
                    }
                    currentValue.add(daArtifactReference);
                }
            }
        }

        String daEnvMap = "";
        if (!daNameReferenceMapping.isEmpty()) {

            ManagementBusPluginRemoteServiceImpl.LOG.debug("NodeTemplate {} has {} DAs.", nodeTemplateID,
                                                           daNameReferenceMapping.size());

            daEnvMap += "DAs=\"";
            for (final Entry<String, List<String>> da : daNameReferenceMapping.entrySet()) {

                final String daName = da.getKey();
                final List<String> daRefs = da.getValue();

                for (String daRef : daRefs) {

                    // FIXME / is a brutal assumption
                    if (!daRef.startsWith("/")) {
                        daRef = "/" + daRef;
                    }

                    daEnvMap += daName + "," + daRef + ";";
                }
            }
            daEnvMap += "\" ";

            ManagementBusPluginRemoteServiceImpl.LOG.debug("Created DA-DANamePathMapEnvVar for NodeTemplate {} : {}",
                                                           nodeTemplateID, daEnvMap);
        }

        return daEnvMap;
    }

    /**
     *
     * Installs required and specified packages of the specified ArtifactType. Required packages are in
     * defined the corresponding *.xml file.
     *
     * @param artifactType
     * @param headers
     */
    private void installPackages(final QName artifactType, final HashMap<String, Object> headers) {

        final List<String> requiredPackages = ArtifactTypesHandler.getRequiredPackages(artifactType);

        String requiredPackagesString = "";

        if (!requiredPackages.isEmpty()) {

            final HashMap<String, String> inputParamsMap = new HashMap<>();

            for (final String requiredPackage : requiredPackages) {
                requiredPackagesString += requiredPackage;
                requiredPackagesString += " ";
            }
            inputParamsMap.put(Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_OPERATINGSYSTEM_PARAMETER_PACKAGENAMES,
                               requiredPackagesString);

            ManagementBusPluginRemoteServiceImpl.LOG.debug("Installing packages: {} for ArtifactType: {} ",
                                                           requiredPackages, artifactType);

            headers.put(MBHeader.OPERATIONNAME_STRING.toString(),
                        Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_OPERATINGSYSTEM_INSTALLPACKAGE);

            invokeManagementBusEngine(inputParamsMap, headers);
        } else {
            ManagementBusPluginRemoteServiceImpl.LOG.debug("ArtifactType: {} needs no packages to install.",
                                                           requiredPackages, artifactType);
        }
    }

    /**
     *
     * For transferring files to the target machine.
     *
     * @param csarID
     * @param artifactTemplate
     * @param source
     * @param target
     * @param headers
     */
    private void transferFile(final CSARID csarID, final QName artifactTemplate, final String source,
                              final String target, final HashMap<String, Object> headers) {

        final HashMap<String, String> inputParamsMap = new HashMap<>();

        inputParamsMap.put(Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_OPERATINGSYSTEM_PARAMETER_TARGETABSOLUTPATH,
                           target);
        inputParamsMap.put(Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_OPERATINGSYSTEM_PARAMETER_SOURCEURLORLOCALPATH,
                           source);

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Uploading file. Source: {} Target: {} ", source, target);

        headers.put(MBHeader.OPERATIONNAME_STRING.toString(),
                    Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_OPERATINGSYSTEM_TRANSFERFILE);

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Invoking ManagementBus for transferFile with the following headers:");

        for (final String key : headers.keySet()) {
            if (headers.get(key) != null && headers.get(key) instanceof String) {
                ManagementBusPluginRemoteServiceImpl.LOG.debug("Header: " + key + " Value: " + headers.get(key));
            }
        }

        invokeManagementBusEngine(inputParamsMap, headers);

    }

    /**
     *
     * For running scripts on the target machine. Commands to be executed are defined in the
     * corresponding *.xml file.
     *
     * @param commandsString
     * @param headers
     */
    private Object runScript(final String commandsString, final HashMap<String, Object> headers) {

        final HashMap<String, String> inputParamsMap = new HashMap<>();

        inputParamsMap.put(Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_OPERATINGSYSTEM_PARAMETER_SCRIPT, commandsString);

        ManagementBusPluginRemoteServiceImpl.LOG.debug("RunScript: {} ", commandsString);

        headers.put(MBHeader.OPERATIONNAME_STRING.toString(),
                    Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_OPERATINGSYSTEM_RUNSCRIPT);

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Invoking ManagementBus for runScript with the following headers:");

        for (final String key : headers.keySet()) {
            if (headers.get(key) != null && headers.get(key) instanceof String) {
                ManagementBusPluginRemoteServiceImpl.LOG.debug("Header: " + key + " Value: " + headers.get(key));
            }
        }

        return invokeManagementBusEngine(inputParamsMap, headers);
    }

    /**
     *
     * Creates ArtifactType specific commands that should be executed on the target machine. Commands
     * to be executed are defined in the corresponding *.xml file.
     *
     * @param csarID
     * @param artifactType
     * @param artifactTemplateID
     * @param params
     *
     * @return the created command
     */
    @SuppressWarnings("unchecked")
    private String createArtifcatTypeSpecificCommandString(final CSARID csarID, final QName artifactType,
                                                           final QName artifactTemplateID, final Object params) {

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Creating ArtifcatType specific command for artifactType {}:...",
                                                       artifactType);

        String commandsString = "";

        final List<String> commands = ArtifactTypesHandler.getCommands(artifactType);

        for (final String command : commands) {
            commandsString += command;
            commandsString += " && ";
        }

        if (commandsString.endsWith(" && ")) {
            commandsString = commandsString.substring(0, commandsString.length() - 4);
        }

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Defined generic command for ArtifactType {} : {} ",
                                                       artifactType, commandsString);

        // replace placeholder with data from inputParams and/or instance data

        if (commandsString.contains("{{") && commandsString.contains("}}")) {

            ManagementBusPluginRemoteServiceImpl.LOG.debug("Replacing the placeholder of the generic command with properties data and/or provided input parameter...");

            HashMap<String, String> paramsMap = new HashMap<>();

            if (params instanceof HashMap) {
                paramsMap = (HashMap<String, String>) params;
            } else if (params instanceof Document) {
                final Document paramsDoc = (Document) params;
                paramsMap = MBUtils.docToMap(paramsDoc, true);
            }

            final Document propDoc =
                ServiceHandler.toscaEngineService.getPropertiesOfAArtifactTemplate(csarID, artifactTemplateID);

            if (propDoc != null) {
                paramsMap.putAll(MBUtils.docToMap(propDoc, true));
            }

            for (final Entry<String, String> prop : paramsMap.entrySet()) {
                commandsString = commandsString.replace("{{" + prop.getKey() + "}}", prop.getValue());
            }

            // delete not replaced placeholder
            commandsString = commandsString.replaceAll("\\{\\{.*?\\}\\}", "");

            ManagementBusPluginRemoteServiceImpl.LOG.debug("Generic command with replaced placeholder: {}",
                                                           commandsString);
        }

        return commandsString;
    }

    /**
     * @param params
     * @return whitespace separated String with parameter keys and values
     */
    @SuppressWarnings("unchecked")
    private String createParamsString(final Object params, final String publicIP) {
        HashMap<String, String> paramsMap = new HashMap<>();

        if (params instanceof HashMap) {
            paramsMap = (HashMap<String, String>) params;
        } else if (params instanceof Document) {
            final Document paramsDoc = (Document) params;
            paramsMap = MBUtils.docToMap(paramsDoc, true);
        }

        String paramsString = "";
        for (final Entry<String, String> param : paramsMap.entrySet()) {
            // Replace http://container:1337 by the real IP address of the OpenTOSCA container. The
            // container placeholder occurs in the docker setup but can´t be used when the remote
            // service is invoked on a device outside of the docker daemon where the container resides.
            if (param.getValue().startsWith("http://container:1337")) {
                final String replacedValue = param.getValue().replaceFirst("container", publicIP);
                paramsString += param.getKey() + "='" + replacedValue + "' ";
            } else {
                // Also replace localhost in case the OpenTOSCA container was started without IP.
                if (param.getValue().startsWith("http://localhost:1337")) {
                    final String replacedValue = param.getValue().replaceFirst("localhost", publicIP);
                    paramsString += param.getKey() + "='" + replacedValue + "' ";
                } else {

                    paramsString += param.getKey() + "='" + param.getValue() + "' ";
                }
            }
        }

        return paramsString;
    }

    /**
     *
     * Invokes the Management Bus.
     *
     * @param paramsMap
     * @param headers
     */
    private Object invokeManagementBusEngine(final HashMap<String, String> paramsMap,
                                             final HashMap<String, Object> headers) {

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Invoking the Management Bus...");

        final ProducerTemplate template = Activator.camelContext.createProducerTemplate();

        final Object response =
            template.requestBodyAndHeaders("bean:org.opentosca.bus.management.service.IManagementBusService?method=invokeIA",
                                           paramsMap, headers);

        ManagementBusPluginRemoteServiceImpl.LOG.debug("Invocation finished: {}", response);

        return response;

    }

    @Override
    public List<String> getSupportedTypes() {

        final List<String> supportedTypes = new ArrayList<>();

        final List<QName> supportedTypesQName = ArtifactTypesHandler.getSupportedTypes();

        for (final QName supportedTypeQName : supportedTypesQName) {
            supportedTypes.add(supportedTypeQName.toString());
        }

        return supportedTypes;
    }

}
