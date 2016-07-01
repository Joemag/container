package org.opentosca.planbuilder.provphase.plugin.ansibleoperation.handler;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RunnableScheduledFuture;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.FileLocator;
import org.opentosca.planbuilder.TemplatePlanBuilder;
import org.opentosca.planbuilder.TemplatePlanBuilder.ProvisioningChain;
import org.opentosca.planbuilder.model.plan.BuildPlan;
import org.opentosca.planbuilder.model.tosca.AbstractArtifactReference;
import org.opentosca.planbuilder.model.tosca.AbstractDeploymentArtifact;
import org.opentosca.planbuilder.model.tosca.AbstractImplementationArtifact;
import org.opentosca.planbuilder.model.tosca.AbstractNodeTemplate;
import org.opentosca.planbuilder.model.tosca.AbstractOperation;
import org.opentosca.planbuilder.model.tosca.AbstractParameter;
import org.opentosca.planbuilder.model.tosca.AbstractProperties;
import org.opentosca.planbuilder.model.tosca.AbstractRelationshipTemplate;
import org.opentosca.model.tosca.conventions.Interfaces;
import org.opentosca.model.tosca.conventions.Properties;
import org.opentosca.planbuilder.plugins.context.TemplatePlanContext;
import org.opentosca.planbuilder.plugins.context.TemplatePlanContext.Variable;
import org.opentosca.planbuilder.provphase.plugin.invoker.Plugin;
import org.opentosca.planbuilder.utils.Utils;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <p>
 * This class is contains the logic to add BPEL Fragments, which executes
 * Ansible Playbooks on remote machine. The class assumes that the playbook that must be
 * called are already uploaded to the appropriate path. For example by the
 * ScriptIAOnLinux Plugin
 * </p>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 *
 * @author Kalman Kepes - kalman.kepes@iaas.uni-stuttgart.de
 * @author Michael Zimmermann - michael.zimmermann@iaas.uni-stuttgart.de
 *
 */
public class Handler {

	private Plugin invokerPlugin = new Plugin();

	private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(Handler.class);

	private DocumentBuilderFactory docFactory;
	private DocumentBuilder docBuilder;

	public Handler() {
		try {
			this.docFactory = DocumentBuilderFactory.newInstance();
			this.docFactory.setNamespaceAware(true);
			this.docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds logic to the BuildPlan to call a Ansible Playbook on a remote machine
	 *
	 * @param context
	 *            the TemplatePlanContext where the logical provisioning
	 *            operation is called
	 * @param operation
	 *            the operation to call
	 * @param ia
	 *            the ia that implements the operation
	 * @return true iff adding BPEL Fragment was successful
	 */
	public boolean handle(TemplatePlanContext templateContext, AbstractOperation operation,
			AbstractImplementationArtifact ia) {
		
		LOG.debug("Handling Ansible Playbook IA operation: " +operation.getName());
		AbstractArtifactReference ansibleRef = this.fetchAnsiblePlaybookRefFromIA(ia);
		if (ansibleRef == null) {
			return false;
		}
		LOG.debug("Ref: " + ansibleRef.getReference());

		// calculate relevant nodeTemplates for this operation call (the node
		// itself and infraNodes)
		List<AbstractNodeTemplate> nodes = templateContext.getInfrastructureNodes();

		// add the template itself
		nodes.add(templateContext.getNodeTemplate());

		// find the ubuntu node and its nodeTemplateId
		AbstractNodeTemplate infrastructureNodeTemplate = this.findInfrastructureNode(nodes);

		if (infrastructureNodeTemplate == null) {
			Handler.LOG.warn("Couldn't determine NodeTemplateId of Ubuntu Node");
			return false;
		}

		/*
		 * fetch relevant variables/properties
		 */
		if (templateContext.getNodeTemplate() == null) {
			Handler.LOG.warn("Appending logic to relationshipTemplate plan is not possible by this plugin");
			return false;
		}

		// fetch server ip of the vm this apache http php module will be
		// installed on
		Variable serverIpPropWrapper = null;
		for (String serverIp : org.opentosca.model.tosca.conventions.Utils
				.getSupportedVirtualMachineIPPropertyNames()) {
			serverIpPropWrapper = templateContext.getPropertyVariable(infrastructureNodeTemplate, serverIp);
			if (serverIpPropWrapper != null) {
				break;
			} 
		}

		if (serverIpPropWrapper == null) {
			Handler.LOG.warn("No Infrastructure Node available with ServerIp property");
			return false;
		}

		// find sshUser and sshKey
		Variable sshUserVariable = null;
		for (String vmUserName : org.opentosca.model.tosca.conventions.Utils
				.getSupportedVirtualMachineLoginUserNamePropertyNames()) {
			sshUserVariable = templateContext.getPropertyVariable(infrastructureNodeTemplate, vmUserName);
			if (sshUserVariable != null) {
				break;
			} 
		}

		// if the variable is null now -> the property isn't set properly
		if (sshUserVariable == null) {
			return false;
		} else {
			if (Utils.isVariableValueEmpty(sshUserVariable, templateContext)) {
				// the property isn't set in the topology template -> we set it
				// null here so it will be handled as an external parameter
				sshUserVariable = null;
			}
		}
		Variable sshKeyVariable = null;
		for (String vmUserPassword : org.opentosca.model.tosca.conventions.Utils
				.getSupportedVirtualMachineLoginPasswordPropertyNames()) {
			sshKeyVariable = templateContext.getPropertyVariable(infrastructureNodeTemplate, vmUserPassword);
			if (sshKeyVariable != null) {
				break;
			} 
		}

		// if variable null now -> the property isn't set according to schema
		if (sshKeyVariable == null) {
			return false;
		} else {
			if (Utils.isVariableValueEmpty(sshKeyVariable, templateContext)) {
				// see sshUserVariable..
				sshKeyVariable = null;
			}
		}
		// add sshUser and sshKey to the input message of the build plan, if
		// needed
		if (sshUserVariable == null) {
			// dirty check if we use old style properties
			String cleanPropName = serverIpPropWrapper.getName()
					.substring(serverIpPropWrapper.getName().lastIndexOf("_") + 1);
			switch (cleanPropName) {
			case Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_SERVERIP:
				LOG.debug("Adding sshUser field to plan input");
				templateContext.addStringValueToPlanRequest("sshUser");
				break;
			case Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_VMIP:
				LOG.debug("Adding sshUser field to plan input");
				templateContext.addStringValueToPlanRequest(Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_VMLOGINNAME);
				break;
			case Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_RASPBIANIP:
				LOG.debug("Adding User fiel to plan input");
				templateContext.addStringValueToPlanRequest(Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_RASPBIANUSER);
				break;
			default:
				return false;

			}
		}

		if (sshKeyVariable == null) {
			// dirty check if we use old style properties
			String cleanPropName = serverIpPropWrapper.getName()
					.substring(serverIpPropWrapper.getName().lastIndexOf("_") + 1);
			switch (cleanPropName) {
			case Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_SERVERIP:
				LOG.debug("Adding sshUser field to plan input");
				templateContext.addStringValueToPlanRequest("sshKey");
				break;
			case Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_VMIP:
				LOG.debug("Adding sshUser field to plan input");
				templateContext
						.addStringValueToPlanRequest(Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_VMLOGINPASSWORD);
				break;
			case Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_RASPBIANIP:
				LOG.debug("Adding User fiel to plan input");
				templateContext
						.addStringValueToPlanRequest(Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_RASPBIANPASSWD);
				break;
			default:
				return false;

			}
		}

		// adds field into plan input message to give the plan it's own address
		// for the invoker PortType (callback etc.). This is needed as WSO2 BPS
		// 2.x can't give that at runtime (bug)
		LOG.debug("Adding plan callback address field to plan input");
		templateContext.addStringValueToPlanRequest("planCallbackAddress_invoker");

		// add csarEntryPoint to plan input message
		LOG.debug("Adding csarEntryPoint field to plan input");
		templateContext.addStringValueToPlanRequest("csarEntrypoint");

		Variable runShScriptStringVar = this.appendBPELAssignOperationShScript(templateContext, operation, ansibleRef,
				ia);

		return this.appendExecuteScript(templateContext, infrastructureNodeTemplate.getId(), runShScriptStringVar,
				sshUserVariable, sshKeyVariable, serverIpPropWrapper);
	}

	public boolean handle(TemplatePlanContext templateContext, AbstractOperation operation,
			AbstractImplementationArtifact ia, Map<AbstractParameter, Variable> param2propertyMapping) {
		
		if(operation.getInputParameters().size() != param2propertyMapping.size()){
			return false;
		}
		
		AbstractNodeTemplate infrastructureNodeTemplate = this
				.findInfrastructureNode(templateContext.getInfrastructureNodes());
		if (infrastructureNodeTemplate == null) {
			return false;
		}

		Variable runShScriptStringVar = null;
		AbstractArtifactReference scriptRef = this.fetchAnsiblePlaybookRefFromIA(ia);
		if (scriptRef == null) {
			return false;
		}
		runShScriptStringVar = this.appendBPELAssignOperationShScript(templateContext, operation, scriptRef, ia,
				param2propertyMapping);

		Variable ipStringVariable = null;
		for (String serverIp : org.opentosca.model.tosca.conventions.Utils
				.getSupportedVirtualMachineIPPropertyNames()) {
			ipStringVariable = templateContext.getPropertyVariable(infrastructureNodeTemplate, serverIp);
			if (ipStringVariable != null) {
				break;
			}
		}

		Variable userStringVariable = null;
		for (String vmUserName : org.opentosca.model.tosca.conventions.Utils
				.getSupportedVirtualMachineLoginUserNamePropertyNames()) {
			userStringVariable = templateContext.getPropertyVariable(infrastructureNodeTemplate, vmUserName);
			if (userStringVariable != null) {
				break;
			}
		}

		Variable passwdStringVariable = null;
		for (String vmUserPassword : org.opentosca.model.tosca.conventions.Utils
				.getSupportedVirtualMachineLoginPasswordPropertyNames()) {
			passwdStringVariable = templateContext.getPropertyVariable(infrastructureNodeTemplate, vmUserPassword);
			if (passwdStringVariable != null) {
				break;
			}
		}

		if (this.isNull(runShScriptStringVar, ipStringVariable, userStringVariable, passwdStringVariable)) {
			// if either of the variables is null -> abort
			return false;
		}

		return this.appendExecuteScript(templateContext, infrastructureNodeTemplate.getId(), runShScriptStringVar,
				userStringVariable, passwdStringVariable, ipStringVariable);
	}

	private boolean isNull(Variable... vars) {
		for (Variable var : vars) {
			if (var == null) {
				return true;
			}
		}
		return false;
	}

	private AbstractNodeTemplate findInfrastructureNode(List<AbstractNodeTemplate> nodes) {
		for (AbstractNodeTemplate nodeTemplate : nodes) {
			if (org.opentosca.model.tosca.conventions.Utils
					.isSupportedInfrastructureNodeType(nodeTemplate.getType().getId())) {
				return nodeTemplate;
			}
		}
		return null;
	}

	/**
	 * Append logic for executing a script on a remote machine with the invoker
	 * plugin
	 * 
	 * @param templateContext
	 *            the context with a bpel templateBuildPlan
	 * @param templateId
	 *            the id of the template inside the context
	 * @param runShScriptStringVar
	 *            the bpel variable containing the script call
	 * @param sshUserVariable
	 *            the user name for the remote machine as a bpel variable
	 * @param sshKeyVariable
	 *            the pass for the remote machine as a bpel variable
	 * @param serverIpPropWrapper
	 *            the ip of the remote machine as a bpel variable
	 * @return true if appending the bpel logic was successful else false
	 */
	private boolean appendExecuteScript(TemplatePlanContext templateContext, String templateId,
			Variable runShScriptStringVar, Variable sshUserVariable, Variable sshKeyVariable,
			Variable serverIpPropWrapper) {
		
		Map<String,Variable> runScriptRequestInputParams = new HashMap<String,Variable>();
		// dirty check if we use old style properties
		String cleanPropName = serverIpPropWrapper.getName()
				.substring(serverIpPropWrapper.getName().lastIndexOf("_") + 1);
		switch (cleanPropName) {
		case Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_SERVERIP:
			runScriptRequestInputParams.put("hostname", serverIpPropWrapper);
			runScriptRequestInputParams.put("sshKey", sshKeyVariable);
			runScriptRequestInputParams.put("sshUser", sshUserVariable);
			runScriptRequestInputParams.put("script", runShScriptStringVar);
			this.invokerPlugin.handle(templateContext, templateId, true, "runScript", "InterfaceUbuntu",
					"planCallbackAddress_invoker", runScriptRequestInputParams, new HashMap<String, Variable>(), false);

			break;
		case Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_VMIP:
		case Properties.OPENTOSCA_DECLARATIVE_PROPERTYNAME_RASPBIANIP:
			runScriptRequestInputParams.put("VMIP", serverIpPropWrapper);
			runScriptRequestInputParams.put("VMPrivateKey", sshKeyVariable);
			runScriptRequestInputParams.put("VMUserName", sshUserVariable);
			runScriptRequestInputParams.put("Script", runShScriptStringVar);
			this.invokerPlugin.handle(templateContext, templateId, true, "runScript",
					Interfaces.OPENTOSCA_DECLARATIVE_INTERFACE_OPERATINGSYSTEM, "planCallbackAddress_invoker",
					runScriptRequestInputParams, new HashMap<String, Variable>(), false);
			break;
		default:
			return false;

		}
		return true;
	}

	private String createDANamePathMapEnvVar(TemplatePlanContext templateContext, AbstractImplementationArtifact ia) {
		AbstractNodeTemplate nodeTemplate = templateContext.getNodeTemplate();

		// find selected implementation
		ProvisioningChain chain = TemplatePlanBuilder.createProvisioningChain(nodeTemplate);

		List<AbstractDeploymentArtifact> das = chain.getDAsOfCandidate(0);

		String daEnvMap = "";
		if (nodeTemplate != null && !das.isEmpty()) {
			daEnvMap += "DAs=\"";
			for (AbstractDeploymentArtifact da : das) {
				String daName = da.getName();
				// FIXME we assume single artifact references at this point
				String daRef = da.getArtifactRef().getArtifactReferences().get(0).getReference();

				// FIXME / is a brutal assumption
				if (!daRef.startsWith("/")) {
					daRef = "/" + daRef;
				}

				daEnvMap += daName + "," + daRef + ";";
			}
			daEnvMap += "\" ";
		}
		return daEnvMap;
	}

	private Variable appendBPELAssignOperationShScript(TemplatePlanContext templateContext, AbstractOperation operation,
			AbstractArtifactReference reference, AbstractImplementationArtifact ia) {
		
		
		System.out.println("******** Magic 1 *******");
		
		
		
		/*
		 * First we initialize a bash script of this form: sudo sh
		 * $InputParamName=ValPlaceHolder* referenceShFileName.sh
		 * 
		 * After that we try to generate a xpath 2.0 query of this form:
		 * ..replace
		 * (replace($runShScriptStringVar,"ValPlaceHolder",$PropertyVariableName
		 * ),"ValPlaceHolder",$planInputVar.partName/inputFieldLocalName)..
		 * 
		 * With both we have a string with runtime property values or input
		 * params
		 */
		Map<String, Variable> inputMappings = new HashMap<String, Variable>();
		String runShScriptString = "mkdir -p ~/" + templateContext.getCSARFileName() + "/logs/plans/ && chmod +x ~/"
				+ templateContext.getCSARFileName() + "/" + reference.getReference() + " && sudo -E "
				+ this.createDANamePathMapEnvVar(templateContext, ia);

		String runShScriptStringVarName = "runShFile" + templateContext.getIdForNames();
		String xpathQueryPrefix = "";
		String xpathQuerySuffix = "";

		for (AbstractParameter parameter : operation.getInputParameters()) {
			// First compute mappings from operation parameters to
			// property/inputfield
			Variable var = templateContext.getPropertyVariable(parameter.getName());
			if (var == null) {
				var = templateContext.getPropertyVariable(parameter.getName(), true);
				if (var == null) {
					var = templateContext.getPropertyVariable(parameter.getName(), false);
				}
			}
			inputMappings.put(parameter.getName(), var);

			// Initialize bash script string variable with placeholders
			runShScriptString += parameter.getName() + "=$" + parameter.getName() + "$ ";

			// put together the xpath query
			xpathQueryPrefix += "replace(";
			// set the placeholder to replace
			xpathQuerySuffix += ",'\\$" + parameter.getName() + "\\$',";
			if (var == null) {
				// param is external, query value form input message e.g.
				// $input.payload//*[local-name()='csarEntrypoint']/text()

				xpathQuerySuffix += "$" + templateContext.getPlanRequestMessageName() + ".payload//*[local-name()='"
						+ parameter.getName() + "']/text())";
			} else {
				// param is internal, so just query the bpelvar e.g. $Varname
				xpathQuerySuffix += "$" + var.getName() + ")";
			}
		}
		
		
		//HERE COMMAND
		
		
		// add path to script
		runShScriptString += "~/" + templateContext.getCSARFileName() + "/" + reference.getReference();

		// construct log file path
		String logFilePath = "~/" + templateContext.getCSARFileName() + "/logs/plans/"
				+ templateContext.getTemplateBuildPlanName() + "$(date +\"%m_%d_%Y\").log";
		// append command to log the operation call on the machine
		runShScriptString += " > " + logFilePath;
		// and echo the operation call log
		runShScriptString += " && echo " + logFilePath;

		// generate string var with script
		Variable runShScriptStringVar = templateContext.createGlobalStringVariable(runShScriptStringVarName,
				runShScriptString);

		// Reassign string var with runtime values and replace their
		// placeholders
		try {
			// create xpath query
			String xpathQuery = xpathQueryPrefix + "$" + runShScriptStringVar.getName() + xpathQuerySuffix;
			// create assign and append
			Node assignNode = this.loadAssignXpathQueryToStringVarFragmentAsNode("assignShCallScriptVar", xpathQuery,
					runShScriptStringVar.getName());
			assignNode = templateContext.importNode(assignNode);
			templateContext.getProvisioningPhaseElement().appendChild(assignNode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOG.error("Couldn't load fragment from file", e);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			LOG.error("Couldn't parse fragment to DOM", e);
		}
		return runShScriptStringVar;
	}
	
	private Variable appendBPELAssignOperationShScript(TemplatePlanContext templateContext, AbstractOperation operation,
			AbstractArtifactReference reference, AbstractImplementationArtifact ia, Map<AbstractParameter, Variable> inputMappings) {
		
		
		System.out.println("******** Magic 2 *******");
		
		
		/*
		 * First we initialize a bash script of this form: sudo sh
		 * $InputParamName=ValPlaceHolder* referenceShFileName.sh
		 * 
		 * After that we try to generate a xpath 2.0 query of this form:
		 * ..replace
		 * (replace($runShScriptStringVar,"ValPlaceHolder",$PropertyVariableName
		 * ),"ValPlaceHolder",$planInputVar.partName/inputFieldLocalName)..
		 * 
		 * With both we have a string with runtime property values or input
		 * params
		 */
		String runShScriptString = "mkdir -p ~/" + templateContext.getCSARFileName() + "/logs/plans/ && chmod +x ~/"
				+ templateContext.getCSARFileName() + "/" + reference.getReference() + " && sudo -E "
				+ this.createDANamePathMapEnvVar(templateContext, ia);

		String runShScriptStringVarName = "runShFile" + templateContext.getIdForNames();
		String xpathQueryPrefix = "";
		String xpathQuerySuffix = "";

		for (AbstractParameter parameter : operation.getInputParameters()) {
			// First compute mappings from operation parameters to
			// property/inputfield
			Variable var = inputMappings.get(parameter);

			// Initialize bash script string variable with placeholders
			runShScriptString += parameter.getName() + "=$" + parameter.getName() + "$ ";

			// put together the xpath query
			xpathQueryPrefix += "replace(";
			// set the placeholder to replace
			xpathQuerySuffix += ",'\\$" + parameter.getName() + "\\$',";
			if (var == null) {
				// param is external, query value form input message e.g.
				// $input.payload//*[local-name()='csarEntrypoint']/text()

				xpathQuerySuffix += "$" + templateContext.getPlanRequestMessageName() + ".payload//*[local-name()='"
						+ parameter.getName() + "']/text())";
			} else {
				// param is internal, so just query the bpelvar e.g. $Varname
				xpathQuerySuffix += "$" + var.getName() + ")";
			}
		}
		// add path to script
		runShScriptString += "~/" + templateContext.getCSARFileName() + "/" + reference.getReference();

		// construct log file path
		String logFilePath = "~/" + templateContext.getCSARFileName() + "/logs/plans/"
				+ templateContext.getTemplateBuildPlanName() + "$(date +\"%m_%d_%Y\").log";
		// append command to log the operation call on the machine
		runShScriptString += " > " + logFilePath;
		// and echo the operation call log
		runShScriptString += " && echo " + logFilePath;

		// generate string var with script
		Variable runShScriptStringVar = templateContext.createGlobalStringVariable(runShScriptStringVarName,
				runShScriptString);

		// Reassign string var with runtime values and replace their
		// placeholders
		try {
			// create xpath query
			String xpathQuery = xpathQueryPrefix + "$" + runShScriptStringVar.getName() + xpathQuerySuffix;
			// create assign and append
			Node assignNode = this.loadAssignXpathQueryToStringVarFragmentAsNode("assignShCallScriptVar", xpathQuery,
					runShScriptStringVar.getName());
			assignNode = templateContext.importNode(assignNode);
			templateContext.getProvisioningPhaseElement().appendChild(assignNode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOG.error("Couldn't load fragment from file", e);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			LOG.error("Couldn't parse fragment to DOM", e);
		}
		return runShScriptStringVar;
	}

	/**
	 * Returns the first occurrence of *.zip file, inside the given
	 * ImplementationArtifact
	 *
	 * @param ia
	 *            an AbstractImplementationArtifact
	 * @return a String containing a relative file path to a *.zip file, if no
	 *         *.zip file inside the given IA is found null
	 */
	private AbstractArtifactReference fetchAnsiblePlaybookRefFromIA(AbstractImplementationArtifact ia) {
		List<AbstractArtifactReference> refs = ia.getArtifactRef().getArtifactReferences();
		for (AbstractArtifactReference ref : refs) {
			if (ref.getReference().endsWith(".zip")) {
				return ref;
			}
		}
		return null;
	}

	/**
	 * Loads a BPEL Assign fragment which queries the csarEntrypath from the
	 * input message into String variable.
	 *
	 * @param assignName
	 *            the name of the BPEL assign
	 * @param xpath2Query
	 *            the csarEntryPoint XPath query
	 * @param stringVarName
	 *            the variable to load the queries results into
	 * @return a String containing a BPEL Assign element
	 * @throws IOException
	 *             is thrown when reading the BPEL fragment form the resources
	 *             fails
	 */
	public String loadAssignXpathQueryToStringVarFragmentAsString(String assignName, String xpath2Query,
			String stringVarName) throws IOException {
		// <!-- {AssignName},{xpath2query}, {stringVarName} -->
		URL url = FrameworkUtil.getBundle(this.getClass()).getBundleContext().getBundle()
				.getResource("assignStringVarWithXpath2Query.xml");
		File bpelFragmentFile = new File(FileLocator.toFileURL(url).getPath());
		String template = FileUtils.readFileToString(bpelFragmentFile);
		template = template.replace("{AssignName}", assignName);
		template = template.replace("{xpath2query}", xpath2Query);
		template = template.replace("{stringVarName}", stringVarName);
		return template;
	}

	/**
	 * Loads a BPEL Assign fragment which queries the csarEntrypath from the
	 * input message into String variable.
	 *
	 * @param assignName
	 *            the name of the BPEL assign
	 * @param csarEntryXpathQuery
	 *            the csarEntryPoint XPath query
	 * @param stringVarName
	 *            the variable to load the queries results into
	 * @return a DOM Node representing a BPEL assign element
	 * @throws IOException
	 *             is thrown when loading internal bpel fragments fails
	 * @throws SAXException
	 *             is thrown when parsing internal format into DOM fails
	 */
	public Node loadAssignXpathQueryToStringVarFragmentAsNode(String assignName, String xpath2Query,
			String stringVarName) throws IOException, SAXException {
		String templateString = this.loadAssignXpathQueryToStringVarFragmentAsString(assignName, xpath2Query,
				stringVarName);
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(templateString));
		Document doc = this.docBuilder.parse(is);
		return doc.getFirstChild();
	}

}