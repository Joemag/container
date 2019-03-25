package org.opentosca.container.api.controller;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.TExportedInterface;
import org.eclipse.winery.model.tosca.TExportedOperation;
import org.eclipse.winery.model.tosca.TPlan;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.opentosca.container.api.dto.NodeOperationDTO;
import org.opentosca.container.api.dto.ResourceDecorator;
import org.opentosca.container.api.dto.ServiceTemplateInstanceDTO;
import org.opentosca.container.api.dto.ServiceTemplateInstanceListDTO;
import org.opentosca.container.api.dto.boundarydefinitions.InterfaceDTO;
import org.opentosca.container.api.dto.boundarydefinitions.InterfaceListDTO;
import org.opentosca.container.api.dto.boundarydefinitions.OperationDTO;
import org.opentosca.container.api.dto.plan.PlanDTO;
import org.opentosca.container.api.dto.request.CreateServiceTemplateInstanceRequest;
import org.opentosca.container.api.service.InstanceService;
import org.opentosca.container.api.service.PlanService;
import org.opentosca.container.core.common.uri.UriUtil;
import org.opentosca.container.core.model.csar.Csar;
import org.opentosca.container.core.next.model.DeploymentTest;
import org.opentosca.container.core.next.model.PlanInstance;
import org.opentosca.container.core.next.model.PlanType;
import org.opentosca.container.core.next.model.ServiceTemplateInstance;
import org.opentosca.container.core.next.model.ServiceTemplateInstanceState;
import org.opentosca.container.core.next.repository.DeploymentTestRepository;
import org.opentosca.container.core.next.repository.ServiceTemplateInstanceRepository;
import org.opentosca.container.core.tosca.extension.PlanTypes;
import org.opentosca.deployment.checks.DeploymentTestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api
public class ServiceTemplateInstanceController {

  private static final Logger logger = LoggerFactory.getLogger(ServiceTemplateInstanceController.class);

  private final Csar csar;
  private final TServiceTemplate serviceTemplate;

  @Context
  private UriInfo uriInfo;

  private final InstanceService instanceService;

  private final PlanService planService;

  private final DeploymentTestService deploymentTestService;

  public ServiceTemplateInstanceController(final Csar csar, final TServiceTemplate serviceTemplate,
                                           final InstanceService instanceService, final PlanService planService,
                                           final DeploymentTestService deploymentTestService) {
    this.csar = csar;
    this.serviceTemplate = serviceTemplate;
    this.instanceService = instanceService;
    this.planService = planService;
    this.deploymentTestService = deploymentTestService;
  }

  @GET
  @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ApiOperation(value = "Get all instances of a service template", response = ServiceTemplateInstanceListDTO.class)
  public Response getServiceTemplateInstances() {
    final Collection<ServiceTemplateInstance> serviceInstances =
      this.instanceService.getServiceTemplateInstances(serviceTemplate.getId());
    logger.debug("Found <{}> instances of ServiceTemplate \"{}\" ", serviceInstances.size(), serviceTemplate.getId());

    final ServiceTemplateInstanceListDTO list = new ServiceTemplateInstanceListDTO();

    for (final ServiceTemplateInstance i : serviceInstances) {
      final ServiceTemplateInstanceDTO dto = ServiceTemplateInstanceDTO.Converter.convert(i);
      dto.add(UriUtil.generateSubResourceLink(this.uriInfo, dto.getId().toString(), false, "self"));

      list.add(dto);
    }

    list.add(UriUtil.generateSelfLink(this.uriInfo));

    return Response.ok(list).build();
  }

  @POST
  @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Consumes( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ApiOperation(hidden = true, value = "")
  public Response createServiceTemplateInstance(final CreateServiceTemplateInstanceRequest request) {

    if (request == null || request.getCorrelationId() == null || request.getCorrelationId().trim().length() == 0) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    try {
      final ServiceTemplateInstance createdInstance =
        this.instanceService.createServiceTemplateInstance(csar.id().csarName(), serviceTemplate.getId(), request.getCorrelationId().trim());

      final URI uri = UriUtil.generateSubResourceURI(this.uriInfo, createdInstance.getId().toString(), false);

      return Response.ok(uri).build();
    } catch (final IllegalArgumentException e) {
      return Response.status(Status.BAD_REQUEST).build();
    } catch (InstantiationException | IllegalAccessException e) {
      logger.debug("Internal error occurred: {}", e.getMessage());

      return Response.serverError().build();
    } catch (final NotFoundException e) {
      logger.debug("Didn't find PlanInstances with given correlationId: {}", e.getMessage());
      return Response.status(Status.BAD_REQUEST).entity("Didn't find PlanInstances with given correlationId")
        .build();
    }
  }

  @GET
  @Path("/{id}")
  @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ApiOperation(value = "Get a service template instance", response = ServiceTemplateInstanceDTO.class)
  public Response getServiceTemplateInstance(@ApiParam("ID of service template instance") @PathParam("id") final Long id) {

    final ServiceTemplateInstance instance = resolveInstance(id, serviceTemplate.getId());

    final ServiceTemplateInstanceDTO dto = ServiceTemplateInstanceDTO.Converter.convert(instance);

    // Build plan: Determine plan instance that created this service
    // template instance
    final PlanInstance pi =
      instance.getPlanInstances().stream().filter(p -> p.getType().equals(PlanType.BUILD)).findFirst().get();
    // Add a link
    final String path = "/csars/{csar}/servicetemplates/{servicetemplate}/buildplans/{plan}/instances/{instance}";
    final URI uri =
      this.uriInfo.getBaseUriBuilder().path(path).build(csar.id().csarName(), serviceTemplate.getId(),
        pi.getTemplateId().getLocalPart(), pi.getCorrelationId());
    dto.add(Link.fromUri(UriUtil.encode(uri)).rel("build_plan_instance").build());
    dto.add(UriUtil.generateSubResourceLink(this.uriInfo, "managementplans", false, "managementplans"));
    dto.add(UriUtil.generateSubResourceLink(this.uriInfo, "state", false, "state"));
    dto.add(UriUtil.generateSubResourceLink(this.uriInfo, "properties", false, "properties"));
    dto.add(UriUtil.generateSubResourceLink(this.uriInfo, "deploymenttests", false, "deploymenttests"));
    dto.add(UriUtil.generateSubResourceLink(this.uriInfo, "boundarydefinitions/interfaces", false,
      "boundarydefinitions/interfaces"));
    dto.add(UriUtil.generateSelfLink(this.uriInfo));

    return Response.ok(dto).build();
  }

  @DELETE
  @Path("/{id}")
  @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ApiOperation(hidden = true, value = "")
  public Response deleteServiceTemplateInstance(@PathParam("id") final Long id) {
    this.instanceService.deleteServiceTemplateInstance(id);
    return Response.noContent().build();
  }

  @Path("/{id}/managementplans")
  public ManagementPlanController getManagementPlans(@ApiParam("ID of service template instance") @PathParam("id") final Long id) {
    final ServiceTemplateInstance instance = resolveInstance(id, serviceTemplate.getId());
    return new ManagementPlanController(csar, serviceTemplate, id, this.planService, PlanTypes.TERMINATION, PlanTypes.OTHERMANAGEMENT);
  }

  @GET
  @Path("/{id}/state")
  @Produces( {MediaType.TEXT_PLAIN})
  @ApiOperation(value = "Get state of a service template instance", response = String.class)
  public Response getServiceTemplateInstanceState(@ApiParam("ID of service template instance") @PathParam("id") final Long id) {
    final ServiceTemplateInstanceState state = this.instanceService.getServiceTemplateInstanceState(id);
    return Response.ok(state.toString()).build();
  }

  @PUT
  @Path("/{id}/state")
  @Consumes( {MediaType.TEXT_PLAIN})
  @ApiOperation(hidden = true, value = "")
  public Response updateServiceTemplateInstanceState(@PathParam("id") final Long id, final String request) {
    try {
      this.instanceService.setServiceTemplateInstanceState(id, request);
    } catch (final IllegalArgumentException e) { // this handles a null request too
      return Response.status(Status.BAD_REQUEST).build();
    }
    return Response.ok().build();
  }

  @GET
  @Path("/{id}/properties")
  @Produces( {MediaType.APPLICATION_XML})
  @ApiOperation(hidden = true, value = "")
  public Response getServiceTemplateInstanceProperties(@PathParam("id") final Long id) {
    final Document properties = this.instanceService.getServiceTemplateInstanceProperties(id);
    if (properties == null) {
      return Response.noContent().build();
    } else {
      return Response.ok(properties).build();
    }
  }

  @PUT
  @Path("/{id}/properties")
  @Consumes( {MediaType.APPLICATION_XML})
  @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  @ApiOperation(hidden = true, value = "")
  public Response updateServiceTemplateInstanceProperties(@PathParam("id") final Long id, final Document request) {

    try {
      this.instanceService.setServiceTemplateInstanceProperties(id, request);
    } catch (final IllegalArgumentException e) { // this handles a null request too
      return Response.status(Status.BAD_REQUEST).build();
    } catch (final ReflectiveOperationException e) {
      return Response.serverError().build();
    }
    return Response.ok(UriUtil.generateSelfURI(this.uriInfo)).build();
  }

  /**
   * Gets a reference to the service template instance. Ensures that the instance actually belongs to
   * the service template.
   *
   * @param instanceId
   * @param templateId
   * @return
   * @throws NotFoundException if the instance does not belong to the service template
   */
  private ServiceTemplateInstance resolveInstance(final Long instanceId,
                                                  final String templateId) throws NotFoundException {
    // We only need to check that the instance belongs to the template, the rest is
    // guaranteed while this is a sub-resource
    final ServiceTemplateInstance instance = this.instanceService.getServiceTemplateInstance(instanceId);

    if (!instance.getTemplateId().equals(QName.valueOf(templateId))) {
      logger.info("Service template instance <{}> could not be found", instanceId);
      throw new NotFoundException(String.format("Service template instance <%s> could not be found", instanceId));
    }

    return instance;
  }

  @GET
  @Path("/{id}/boundarydefinitions/interfaces")
  @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @ApiOperation(value = "Get interfaces of a service tempate", response = InterfaceListDTO.class)
  public Response getInterfaces(@PathParam("id") final Long id) {

    List<TExportedInterface> boundaryInterfaces = serviceTemplate.getBoundaryDefinitions().getInterfaces().getInterface();
    logger.debug("Found <{}> interface(s) in Service Template \"{}\" of CSAR \"{}\" ", boundaryInterfaces.size(),
      serviceTemplate.getId(), csar.id().csarName());

    final InterfaceListDTO list = new InterfaceListDTO();
    list.add(boundaryInterfaces.stream().map(exportedInterface -> {

      final List<TExportedOperation> operations = exportedInterface.getOperation();
      logger.debug("Found <{}> operation(s) for Interface \"{}\" in Service Template \"{}\" of CSAR \"{}\" ",
        operations.size(), exportedInterface.getName(), serviceTemplate.getId(), csar.id().csarName());

      final Map<String, OperationDTO> ops = operations.stream().filter(o -> {
        final PlanDTO plan = new PlanDTO((TPlan) o.getPlan().getPlanRef());
        return !PlanTypes.BUILD.toString().equals(plan.getPlanType());
      }).map(o -> {
        final OperationDTO dto = new OperationDTO();
        dto.setName(o.getName());
        dto.setNodeOperation(NodeOperationDTO.Converter.convert(o.getNodeOperation()));
        dto.setRelationshipOperation(o.getRelationshipOperation());
        if (o.getPlan() != null) {
          final PlanDTO plan = new PlanDTO((TPlan) o.getPlan().getPlanRef());
          dto.setPlan(plan);
          // Compute the according URL for the Build or Management Plan
          final URI planUrl;
          if (PlanTypes.BUILD.toString().equals(plan.getPlanType())) {
            // If it's a build plan
            planUrl =
              this.uriInfo.getBaseUriBuilder()
                .path("/csars/{csar}/servicetemplates/{servicetemplate}/buildplans/{buildplan}")
                .build(csar.id().csarName(), serviceTemplate.getId(), plan.getId());
          } else {
            // ... else we assume it's a management plan
            planUrl =
              this.uriInfo.getBaseUriBuilder()
                .path("/csars/{csar}/servicetemplates/{servicetemplate}/instances/{id}/managementplans/{managementplan}")
                .build(csar.id().csarName(), serviceTemplate.getId(), id, plan.getId());
          }
          plan.add(Link.fromUri(UriUtil.encode(planUrl)).rel("self").build());
          dto.add(Link.fromUri(UriUtil.encode(planUrl)).rel("plan").build());
        }
        return dto;
      }).collect(Collectors.toMap(OperationDTO::getName, t -> t));

      final InterfaceDTO dto = new InterfaceDTO();
      String name = exportedInterface.getName();
      dto.setName(name);
      dto.setOperations(ops);

      final URI selfLink =
        this.uriInfo.getBaseUriBuilder()
          .path("/csars/{csar}/servicetemplates/{servicetemplate}/boundarydefinitions/interfaces/{name}")
          .build(csar.id().csarName(), serviceTemplate.getId(), name);
      dto.add(Link.fromUri(UriUtil.encode(selfLink)).rel("self").build());

      return dto;
    }).collect(Collectors.toList()).toArray(new InterfaceDTO[] {}));
    list.add(UriUtil.generateSelfLink(this.uriInfo));

    return Response.ok(list).build();
  }

  @GET
  @Path("/{id}/deploymenttests")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(hidden = true, value = "")
  public Response getDeploymentTests(@PathParam("id") final Integer id) {
    // TODO: Check if instance belongs to CSAR and Service Template
    final ServiceTemplateInstance sti = new ServiceTemplateInstanceRepository().find(Long.valueOf(id)).orElse(null);
    if (sti == null) {
      logger.info("Service template instance \"" + id + "\" of template \"" + serviceTemplate.getId()
        + "\" could not be found");
      throw new NotFoundException("Service template instance \"" + id + "\" of template \""
        + serviceTemplate.getId() + "\" could not be found");
    }

    final List<ResourceDecorator> items = sti.getDeploymentTests().stream().map(v -> {
      final ResourceDecorator decorator = new ResourceDecorator();
      decorator.setObject(v);
      decorator.add(Link.fromUri(UriUtil.encode(this.uriInfo.getAbsolutePathBuilder()
        .path(String.valueOf(v.getId())).build()))
        .rel("self").build());
      return decorator;
    }).collect(Collectors.toList());

    final ResourceDecorator response = new ResourceDecorator();
    response.setObject(items);
    response.add(Link.fromUri(UriUtil.encode(this.uriInfo.getAbsolutePath())).rel("self").build());

    return Response.ok(response).build();
  }

  @GET
  @Path("/{id}/deploymenttests/{deploymenttest}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(hidden = true, value = "")
  public Response getDeploymentTest(@PathParam("id") final Integer id,
                                    @PathParam("deploymenttest") final Integer deploymenttest) {
    // TODO: Check if instance belongs to CSAR and Service Template
    final ServiceTemplateInstance sti = new ServiceTemplateInstanceRepository().find(Long.valueOf(id)).orElse(null);
    if (sti == null) {
      logger.info("Service template instance \"" + id + "\" of template \"" + serviceTemplate.getId()
        + "\" could not be found");
      throw new NotFoundException("Service template instance \"" + id + "\" of template \""
        + serviceTemplate.getId() + "\" could not be found");
    }

    // TODO: Check if deployment test belongs the current instance
    final DeploymentTest object = new DeploymentTestRepository().find(Long.valueOf(deploymenttest)).orElse(null);
    if (object == null) {
      throw new NotFoundException();
    }

    final ResourceDecorator response = new ResourceDecorator();
    response.setObject(object);
    response.add(Link.fromUri(UriUtil.encode(this.uriInfo.getAbsolutePath())).rel("self").build());

    return Response.ok(response).build();
  }

  @POST
  @Path("/{id}/deploymenttests")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(hidden = true, value = "")
  public Response createDeploymentTest(@PathParam("id") final Integer id) {
    // TODO: Check if instance belongs to CSAR and Service Template
    final ServiceTemplateInstance sti = new ServiceTemplateInstanceRepository().find(Long.valueOf(id)).orElse(null);
    if (sti == null) {
      logger.info("Service template instance \"" + id + "\" of template \"" + serviceTemplate.getId()
        + "\" could not be found");
      throw new NotFoundException("Service template instance \"" + id + "\" of template \""
        + serviceTemplate.getId() + "\" could not be found");
    }

    final DeploymentTest result = this.deploymentTestService.run(csar.id().toOldCsarId(), sti);
    final URI location = this.uriInfo.getAbsolutePathBuilder().path(String.valueOf(result.getId())).build();
    return Response.created(UriUtil.encode(location)).build();
  }
}