package org.opentosca.container.api.controller;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
import org.opentosca.container.api.dto.boundarydefinitions.OperationDTO;
import org.opentosca.container.api.dto.boundarydefinitions.PropertiesDTO;
import org.opentosca.container.api.dto.plan.PlanDTO;
import org.opentosca.container.api.util.UriUtil;
import org.opentosca.container.core.engine.ToscaEngine;
import org.opentosca.container.core.model.csar.Csar;
import org.opentosca.container.core.model.csar.CsarId;
import org.opentosca.container.core.service.CsarStorageService;
import org.opentosca.container.core.tosca.extension.PlanTypes;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions;
import org.eclipse.winery.model.tosca.TExportedInterface;
import org.eclipse.winery.model.tosca.TExportedOperation;
import org.eclipse.winery.model.tosca.TPlan;
import org.eclipse.winery.model.tosca.TPropertyMapping;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api
@Path("/csars/{csar}/servicetemplates/{servicetemplate}/boundarydefinitions")
public class BoundaryDefinitionController {

    private final Logger logger = LoggerFactory.getLogger(BoundaryDefinitionController.class);

    @Context
    private UriInfo uriInfo;

    @Context
    private Request request;

    private CsarStorageService storage;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(hidden = true, value = "")
    public Response getBoundaryDefinitions(@PathParam("csar") final String csarId,
                                           @PathParam("servicetemplate") final String servicetemplate) {

        final Csar csar = this.storage.findById(new CsarId(csarId));

        final TServiceTemplate serviceTemplate;
        try {
            serviceTemplate = ToscaEngine.findServiceTemplate(csar, QName.valueOf(servicetemplate));
        }
        catch (org.opentosca.container.core.common.NotFoundException e) {
            throw new NotFoundException(e);
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
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(value = "Get properties of a service tempate", response = PropertiesDTO.class)
    public Response getBoundaryDefinitionProperties(@ApiParam("ID of CSAR") @PathParam("csar") final String csarId,
                                                    @ApiParam("qualified name of the service template") @PathParam("servicetemplate") final String servicetemplate) {
        final Csar csar = this.storage.findById(new CsarId(csarId));
        final TServiceTemplate serviceTemplate;
        try {
            serviceTemplate = ToscaEngine.findServiceTemplate(csar, QName.valueOf(servicetemplate));
        }
        catch (org.opentosca.container.core.common.NotFoundException e) {
            throw new NotFoundException(e);
        }
        // using optional to condense nullchecks
        @SuppressWarnings("null")
        List<TPropertyMapping> propertyMappings =
            Optional.ofNullable(serviceTemplate).map(TServiceTemplate::getBoundaryDefinitions)
                    .map(TBoundaryDefinitions::getProperties).map(TBoundaryDefinitions.Properties::getPropertyMappings)
                    .map(TBoundaryDefinitions.Properties.PropertyMappings::getPropertyMapping)
                    .orElse(Collections.emptyList());
        final PropertiesDTO dto = new PropertiesDTO();
        dto.setXmlFragment(""); // we're not really exposing these in the winery-model
        if (propertyMappings != null) {
            this.logger.debug("Found <{}> property mappings", propertyMappings.size());
            dto.setPropertyMappings(propertyMappings);
        }
        dto.add(Link.fromUri(UriUtil.encode(this.uriInfo.getAbsolutePath())).rel("self").build());
        return Response.ok(dto).build();
    }

    @GET
    @Path("/interfaces")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(value = "Get interfaces of a service tempate", response = InterfaceListDTO.class)
    public Response getBoundaryDefinitionInterfaces(@ApiParam("ID of CSAR") @PathParam("csar") final String csarId,
                                                    @ApiParam("qualified name of the service template") @PathParam("servicetemplate") final String servicetemplate) {

        final Csar csar = this.storage.findById(new CsarId(csarId));
        final TServiceTemplate serviceTemplate;
        try {
            serviceTemplate = ToscaEngine.findServiceTemplate(csar, QName.valueOf(servicetemplate));
        }
        catch (org.opentosca.container.core.common.NotFoundException e) {
            throw new NotFoundException(e);
        }

        // we're hacking ourselves an elvis operator here, allowing us to condense nullchecks
        @SuppressWarnings("null")
        final List<TExportedInterface> interfaces =
            Optional.ofNullable(serviceTemplate).map(TServiceTemplate::getBoundaryDefinitions)
                    .map(TBoundaryDefinitions::getInterfaces).map(TBoundaryDefinitions.Interfaces::getInterface)
                    .orElse(Collections.emptyList());
        this.logger.debug("Found <{}> interface(s) in Service Template \"{}\" of CSAR \"{}\" ", interfaces.size(),
                          servicetemplate, csar);

        final InterfaceListDTO list = new InterfaceListDTO();
        list.add(interfaces.stream().map(iface -> {
            final InterfaceDTO dto = new InterfaceDTO();
            dto.setName(iface.getName());
            dto.add(UriUtil.generateSubResourceLink(this.uriInfo, iface.getName(), false, "self"));
            return dto;
        }).collect(Collectors.toList()).toArray(new InterfaceDTO[] {}));
        list.add(UriUtil.generateSelfLink(this.uriInfo));

        return Response.ok(list).build();
    }

    @GET
    @Path("/interfaces/{name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(value = "Get an interface of a service template", response = InterfaceDTO.class)
    public Response getBoundaryDefinitionInterface(@ApiParam("interface name") @PathParam("name") final String name,
                                                   @ApiParam("ID of CSAR") @PathParam("csar") final String csarId,
                                                   @ApiParam("qualified name of the service template") @PathParam("servicetemplate") final String servicetemplate) {

        final Csar csar = this.storage.findById(new CsarId(csarId));
        final TServiceTemplate serviceTemplate;
        try {
            serviceTemplate = ToscaEngine.findServiceTemplate(csar, QName.valueOf(servicetemplate));
        }
        catch (org.opentosca.container.core.common.NotFoundException e) {
            throw new NotFoundException(e);
        }

        @SuppressWarnings("null")
        final List<TExportedOperation> operations =
            Optional.ofNullable(serviceTemplate).map(TServiceTemplate::getBoundaryDefinitions)
                    .map(TBoundaryDefinitions::getInterfaces).map(TBoundaryDefinitions.Interfaces::getInterface)
                    .map(List::stream).orElse(Collections.<TExportedInterface>emptyList().stream())
                    .filter(iface -> iface.getIdFromIdOrNameField().equals(name)).findFirst()
                    .map(iface -> iface.getOperation()).orElse(Collections.emptyList());

        this.logger.debug("Found <{}> operation(s) for Interface \"{}\" in Service Template \"{}\" of CSAR \"{}\" ",
                          operations.size(), name, servicetemplate, csar);

        final Map<String, OperationDTO> ops = operations.stream().map(o -> {
            final OperationDTO op = new OperationDTO();

            op.setName(o.getName());
            op.setNodeOperation(NodeOperationDTO.Converter.convert(o.getNodeOperation()));
            op.setRelationshipOperation(o.getRelationshipOperation());

            if (o.getPlan() != null) {
                final PlanDTO plan = new PlanDTO((TPlan) o.getPlan().getPlanRef());
                op.setPlan(plan);

                // Compute the according URL for the Build or Management Plan
                final URI planUrl;
                if (PlanTypes.BUILD.toString().equals(plan.getPlanType())) {
                    // If it's a build plan
                    planUrl =
                        this.uriInfo.getBaseUriBuilder()
                                    .path("/csars/{csar}/servicetemplates/{servicetemplate}/buildplans/{buildplan}")
                                    .build(csar, servicetemplate, plan.getId());
                } else {
                    // ... else we assume it's a management plan
                    planUrl =
                        this.uriInfo.getBaseUriBuilder()
                                    .path("/csars/{csar}/servicetemplates/{servicetemplate}/instances/:id/managementplans/{managementplan}")
                                    .build(csar, servicetemplate, plan.getId());
                }
                plan.add(Link.fromUri(UriUtil.encode(planUrl)).rel("self").build());
                op.add(Link.fromUri(UriUtil.encode(planUrl)).rel("plan").build());
            }
            return op;
        }).collect(Collectors.toMap(OperationDTO::getName, t -> t));

        final InterfaceDTO dto = new InterfaceDTO();
        dto.setName(name);
        dto.setOperations(ops);
        dto.add(UriUtil.generateSelfLink(this.uriInfo));

        return Response.ok(dto).build();
    }

    public void setCsarStorageService(final CsarStorageService storageService) {
        logger.debug("Binding CsarStorageService");
        this.storage = storageService;
    }
}
