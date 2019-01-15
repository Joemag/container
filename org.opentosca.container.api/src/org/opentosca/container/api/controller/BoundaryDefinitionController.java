package org.opentosca.container.api.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;

import org.opentosca.container.api.dto.NodeOperationDTO;
import org.opentosca.container.api.dto.ResourceSupport;
import org.opentosca.container.api.dto.boundarydefinitions.InterfaceDTO;
import org.opentosca.container.api.dto.boundarydefinitions.InterfaceListDTO;
import org.opentosca.container.api.dto.boundarydefinitions.BoundaryInterfaceDTO;
import org.opentosca.container.api.dto.boundarydefinitions.BoundaryOperationDTO;
import org.opentosca.container.api.dto.boundarydefinitions.OperationDTO;
import org.opentosca.container.api.dto.boundarydefinitions.PropertiesDTO;
import org.opentosca.container.api.dto.plan.PlanDTO;
import org.opentosca.container.api.service.CsarService;
import org.opentosca.container.api.service.PlanService;
import org.opentosca.container.api.util.UriUtil;
import org.opentosca.container.core.engine.IToscaEngineService;
import org.opentosca.container.core.engine.IToscaReferenceMapper;
import org.opentosca.container.core.model.csar.CSARContent;
import org.opentosca.container.core.model.csar.id.CSARID;
import org.opentosca.container.core.tosca.extension.PlanTypes;
import org.opentosca.container.core.tosca.model.TExportedInterface;
import org.opentosca.container.core.tosca.model.TExportedOperation;
import org.opentosca.container.core.tosca.model.TPlan;
import org.opentosca.container.core.tosca.model.TPropertyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api
@Path("/csars/{csar}/servicetemplates/{servicetemplate}/boundarydefinitions")
public class BoundaryDefinitionController {

	private static Logger logger = LoggerFactory.getLogger(BoundaryDefinitionController.class);

	@Context
	private UriInfo uriInfo;

	@Context
	private Request request;

	private CsarService csarService;

	private IToscaEngineService engineService;

	private IToscaReferenceMapper referenceMapper;
	
    private PlanService planService = new PlanService();
	
	

	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(hidden = true, value = "")
	public Response getBoundaryDefinitions(@PathParam("csar") final String csar,
			@PathParam("servicetemplate") final String servicetemplate) {

		final CSARContent csarContent = this.csarService.findById(csar);
		if (!this.csarService.hasServiceTemplate(csarContent.getCSARID(), servicetemplate)) {
			logger.info("Service template \"" + servicetemplate + "\" could not be found");
			throw new NotFoundException("Service template \"" + servicetemplate + "\" could not be found");
		}

		final ResourceSupport links = new ResourceSupport();
		links.add(UriUtil.generateSubResourceLink(this.uriInfo, "properties", false, "properties"));
		links.add(UriUtil.generateSubResourceLink(this.uriInfo, "interfaces", false, "interfaces"));

		// TODO This resource seems to be unused and not implemented
		// links.add(Link.fromUri(UriUtils.encode(this.uriInfo.getAbsolutePathBuilder().path("propertyconstraints").build())).rel("propertyconstraints").build());
		// TODO This resource seems to be unused and not implemented
		// links.add(Link.fromUri(UriUtils.encode(this.uriInfo.getAbsolutePathBuilder().path("requirements").build())).rel("requirements").build());
		// TODO This resource seems to be unused and not implemented
		// links.add(Link.fromUri(UriUtils.encode(this.uriInfo.getAbsolutePathBuilder().path("capabilities").build())).rel("capabilities").build());
		// TODO: This resource seems to be unused and not implemented
		// links.add(Link.fromUri(UriUtils.encode(this.uriInfo.getAbsolutePathBuilder().path("policies").build())).rel("policies").build());
		links.add(Link.fromUri(UriUtil.encode(this.uriInfo.getAbsolutePath())).rel("self").build());

		return Response.ok(links).build();
	}

	@GET
	@Path("/properties")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Get properties of a service tempate", response = PropertiesDTO.class)
	public Response getBoundaryDefinitionProperties(@ApiParam("ID of CSAR") @PathParam("csar") final String csar,
			@ApiParam("qualified name of the service template") @PathParam("servicetemplate") final String servicetemplate) {

		final CSARContent csarContent = this.csarService.findById(csar);
		if (!this.csarService.hasServiceTemplate(csarContent.getCSARID(), servicetemplate)) {
			logger.info("Service template \"" + servicetemplate + "\" could not be found");
			throw new NotFoundException("Service template \"" + servicetemplate + "\" could not be found");
		}

		final String xmlFragment = this.referenceMapper
				.getServiceTemplateBoundsPropertiesContent(csarContent.getCSARID(), QName.valueOf(servicetemplate));
		final List<TPropertyMapping> propertyMappings = this.referenceMapper
				.getPropertyMappings(csarContent.getCSARID(), QName.valueOf(servicetemplate));

		final PropertiesDTO dto = new PropertiesDTO();
		logger.debug("XML Fragement: {}", xmlFragment);
		dto.setXmlFragment(xmlFragment);

		if (propertyMappings != null) {
			logger.debug("Found <{}> property mappings", propertyMappings.size());
			dto.setPropertyMappings(propertyMappings);
		}
		dto.add(Link.fromUri(UriUtil.encode(this.uriInfo.getAbsolutePath())).rel("self").build());

		return Response.ok(dto).build();
	}

	@GET
	@Path("/interfaces")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Get interfaces of a service tempate", response = InterfaceListDTO.class)
	public Response getBoundaryDefinitionInterfaces(@ApiParam("ID of CSAR") @PathParam("csar") final String csar,
			@ApiParam("qualified name of the service template") @PathParam("servicetemplate") final String servicetemplate) {

		final CSARContent csarContent = this.csarService.findById(csar);
		if (!this.csarService.hasServiceTemplate(csarContent.getCSARID(), servicetemplate)) {
			logger.info("Service template \"" + servicetemplate + "\" could not be found");
			throw new NotFoundException("Service template \"" + servicetemplate + "\" could not be found");
		}

		final List<String> interfaces = this.referenceMapper
				.getBoundaryInterfacesOfServiceTemplate(csarContent.getCSARID(), QName.valueOf(servicetemplate));
		logger.debug("Found <{}> interface(s) in Service Template \"{}\" of CSAR \"{}\" ", interfaces.size(),
				servicetemplate, csar);

		final InterfaceListDTO list = new InterfaceListDTO();
		list.add(interfaces.stream().map(name -> {
			final InterfaceDTO dto = new InterfaceDTO();
			dto.setName(name);
			dto.add(UriUtil.generateSubResourceLink(this.uriInfo, name, false, "self"));
			return dto;
		}).collect(Collectors.toList()).toArray(new InterfaceDTO[] {}));
		list.add(UriUtil.generateSelfLink(this.uriInfo));

		return Response.ok(list).build();
	}

	@POST
	@Path("/interfaces/{ifacename}/{opname}")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML , MediaType.TEXT_PLAIN})
	public Response invokeBoundaryOperation(@PathParam("ifacename") final String ifaceName,
			@PathParam("opname") String opName, BoundaryOperationDTO invokeOperation,
			@ApiParam("ID of CSAR") @PathParam("csar") final String csar,
			@ApiParam("qualified name of the service template") @PathParam("servicetemplate") final String servicetemplate) {

		// check which kind of operation we have to invoke (node, relation or plan)

		final CSARContent csarContent = this.csarService.findById(csar);

		List<TExportedOperation> operations = getExportedOperations(csarContent.getCSARID(),
				QName.valueOf(servicetemplate), ifaceName);

		for (TExportedOperation op : operations) {
			if (op.getName().equals(opName)) {
				if (op.getPlan() != null) {
				
				}
				// TODO we only invoke plans here, bind here against the Bus for enabling
				// calling Nodes and Relations
			}
		}

		return Response.ok().build();
	}

	@GET
	@Path("/interfaces/{name}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Get an interface of a service template", response = InterfaceDTO.class)
	public Response getBoundaryDefinitionInterface(@ApiParam("interface name") @PathParam("name") final String name,
			@ApiParam("ID of CSAR") @PathParam("csar") final String csar,
			@ApiParam("qualified name of the service template") @PathParam("servicetemplate") final String servicetemplate) {

		final CSARContent csarContent = this.csarService.findById(csar);
		if (!this.csarService.hasServiceTemplate(csarContent.getCSARID(), servicetemplate)) {
			logger.info("Service template \"" + servicetemplate + "\" could not be found");
			throw new NotFoundException("Service template \"" + servicetemplate + "\" could not be found");
		}

		final List<TExportedOperation> operations = getExportedOperations(csarContent.getCSARID(),
				QName.valueOf(servicetemplate), name);

		logger.debug("Found <{}> operation(s) for Interface \"{}\" in Service Template \"{}\" of CSAR \"{}\" ",
				operations.size(), name, servicetemplate, csar);

		final Map<String, BoundaryOperationDTO> ops = operations.stream().map(o -> {

			final BoundaryOperationDTO dto = new BoundaryOperationDTO();

			dto.setId(o.getName());
			
			
			
			if (o.getPlan() != null) {
				final PlanDTO plan = new PlanDTO((TPlan) o.getPlan().getPlanRef());
				dto.setInputParameters(plan.getInputParameters());
				dto.setType(plan.getPlanType());
				// Compute the according URL for the Build or Management Plan				
				
				final URI opUrl;
				opUrl = this.uriInfo.getBaseUriBuilder().path("/csars/{csar}/servicetemplates/{servicetemplate}/boundarydefinitions/interfaces/{ifacename}/{opname}").build(csar,servicetemplate,name,dto.getId());

				dto.add(Link.fromUri(UriUtil.encode(opUrl)).rel("self").build());
				dto.add(Link.fromUri(UriUtil.encode(opUrl)).rel("op").build());
			}
			
			// TODO Node and Relation Operations

			return dto;
		}).collect(Collectors.toMap(BoundaryOperationDTO::getId, t -> t));

		final BoundaryInterfaceDTO dto = new BoundaryInterfaceDTO();
		dto.setName(name);		
		dto.setOperations(ops);
		dto.add(UriUtil.generateSelfLink(this.uriInfo));

		return Response.ok(dto).build();
	}

	private List<TExportedOperation> getExportedOperations(final CSARID csarId, final QName serviceTemplate,
			final String interfaceName) {
		final Map<QName, List<TExportedInterface>> exportedInterfacesOfCsar = this.referenceMapper
				.getExportedInterfacesOfCSAR(csarId);
		if (exportedInterfacesOfCsar.containsKey(serviceTemplate)) {
			final List<TExportedInterface> exportedInterfaces = exportedInterfacesOfCsar.get(serviceTemplate);
			for (final TExportedInterface exportedInterface : exportedInterfaces) {
				if (exportedInterface.getName().equalsIgnoreCase(interfaceName)) {
					return exportedInterface.getOperation();
				}
			}
		}
		return null;
	}

	public void setCsarService(final CsarService csarService) {
		this.csarService = csarService;
	}

	public void setEngineService(final IToscaEngineService engineService) {
		this.engineService = engineService;
		// We cannot inject an instance of {@link IToscaReferenceMapper} since
		// it is manually created in our default implementation of {@link
		// IToscaEngineService}
		this.referenceMapper = this.engineService.getToscaReferenceMapper();
	}
}
