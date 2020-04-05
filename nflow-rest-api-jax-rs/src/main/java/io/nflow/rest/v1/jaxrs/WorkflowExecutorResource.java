package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_EXECUTOR_PATH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;
import static java.util.stream.Collectors.toList;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.WorkflowExecutorService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.converter.ListWorkflowExecutorConverter;
import io.nflow.rest.v1.msg.ListWorkflowExecutorResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;

@Path(NFLOW_WORKFLOW_EXECUTOR_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@OpenAPIDefinition(info = @Info(
        title = "nFlow workflow executor management"
))
@Component
@NflowCors
public class WorkflowExecutorResource {

  private final WorkflowExecutorService workflowExecutors;
  private final ListWorkflowExecutorConverter converter;

  @Inject
  public WorkflowExecutorResource(WorkflowExecutorService workflowExecutors, ListWorkflowExecutorConverter converter) {
    this.workflowExecutors = workflowExecutors;
    this.converter = converter;
  }

  @GET
  @Operation(summary = "List workflow executors")
  public Collection<ListWorkflowExecutorResponse> listWorkflowExecutors() {
    return workflowExecutors.getWorkflowExecutors().stream().map(executor -> converter.convert(executor)).collect(toList());
  }
}
