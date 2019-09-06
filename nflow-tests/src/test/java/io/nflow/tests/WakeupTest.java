package io.nflow.tests;

import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.tests.demo.workflow.ForeverWaitingWorkflow;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.ClientErrorException;
import java.io.IOException;

import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

@ExtendWith(NflowServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WakeupTest extends AbstractNflowTest {

    public static NflowServerConfig server = new NflowServerConfig.Builder().springContextClass(DemoWorkflowTest.DemoConfiguration.class)
            .prop("nflow.executor.timeout.seconds", 1)
            .prop("nflow.executor.keepalive.seconds", 5)
            .prop("nflow.dispatcher.await.termination.seconds", 1)
            .prop("nflow.db.h2.url", "jdbc:h2:mem:statisticstest;TRACE_LEVEL_FILE=4;DB_CLOSE_DELAY=-1")
            .build();

    private static CreateWorkflowInstanceResponse createdWorkflow;

    public WakeupTest() {
        super(server);
    }

    @Test
    @Order(1)
    public void createForeverWaitingWorkflow() throws IOException {
        CreateWorkflowInstanceRequest createRequest = new CreateWorkflowInstanceRequest();
        createRequest.type = ForeverWaitingWorkflow.FOREVER_WAITING_WORKFLOW_TYPE;
        createdWorkflow = assertTimeoutPreemptively(ofSeconds(5),
                () -> createWorkflowInstance(createRequest));
        assertThat(createdWorkflow.id, notNullValue());
    }

    @Test
    @Order(2)
    public void waitUntilInWaitingState() throws InterruptedException {
        getWorkflowInstance(createdWorkflow.id, "waiting");
        ListWorkflowInstanceResponse instance = getWorkflowInstance(createdWorkflow.id);
        assertEquals("waiting", instance.state);
        assertEquals(1, getWorkflowInstance(createdWorkflow.id).actions.size());
    }

    @Test
    @Order(3)
    public void wakeupWorkflowWithWrongExpectedStateReturns409() {
        ClientErrorException thrown = assertThrows(ClientErrorException.class,
                () -> wakeup(createdWorkflow.id, asList("xxx")));
        assertEquals(409, thrown.getResponse().getStatus());

        assertEquals(1, getWorkflowInstance(createdWorkflow.id).actions.size());
    }

    @Test
    @Order(4)
    public void wakeupWorkflowWithRightExpectedStateReturns204() throws InterruptedException {
        wakeup(createdWorkflow.id, asList("waiting", "xxx"));
        waitUntilActionCount(createdWorkflow.id, 2, 10 * 1000);
    }

    @Test
    @Order(5)
    public void wakeupAgain() throws InterruptedException {
        wakeup(createdWorkflow.id, asList("waiting"));
        waitUntilActionCount(createdWorkflow.id, 3, 10 * 1000);
    }

    private void waitUntilActionCount(int workflowId, int expectedActionCount, long maxWaitTime) throws InterruptedException {
        long start = currentTimeMillis();
        while((currentTimeMillis() - start) < maxWaitTime ) {
            Thread.sleep(500);
            if (getWorkflowInstance(workflowId).actions.size() >= expectedActionCount) {
                return;
            }
        }
    }

}
