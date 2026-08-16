package org.evomaster.client.java.controller.problem;

import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.sql.DbSpecification;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncApiProblemTest {

    @Test
    public void testDeclaredWithSomewhereToFetchTheDocumentFrom() {

        AsyncApiProblem p = new AsyncApiProblem("http://localhost:8080/asyncapi.yaml");

        assertEquals("http://localhost:8080/asyncapi.yaml", p.getSchemaLocation());
        assertNull(p.getSchemaText());
    }

    @Test
    public void testDeclaredWithTheDocumentItself() {

        /*
            The common case, and the difference from OpenAPI: a service that speaks only Kafka
            has no HTTP endpoint to serve its own contract from, so the document is far more
            likely to be a file shipped beside it than a URL.
         */
        AsyncApiProblem p = AsyncApiProblem.fromSchemaText("asyncapi: 3.0.0");

        assertEquals("asyncapi: 3.0.0", p.getSchemaText());
        assertNull(p.getSchemaLocation());
    }

    @Test
    public void testTheDocumentHasToComeFromExactlyOnePlace() {

        //neither
        assertThrows(IllegalArgumentException.class, () -> new AsyncApiProblem(null));

        //and null is not a document
        assertThrows(NullPointerException.class, () -> AsyncApiProblem.fromSchemaText(null));
    }

    @Test
    public void testServicesToNotMockAreCarriedOver() {

        List<ExternalService> services =
                Arrays.asList(new ExternalService("foo.com", 80), new ExternalService("bar.com", 443));

        AsyncApiProblem p = AsyncApiProblem.fromSchemaText("asyncapi: 3.0.0")
                .withServicesToNotMock(services);

        assertEquals(2, p.getServicesToNotMock().size());
        assertEquals("asyncapi: 3.0.0", p.getSchemaText());
    }

    @Test
    public void testServicesToNotMockCannotBeChangedFromOutside() {

        AsyncApiProblem p = AsyncApiProblem.fromSchemaText("asyncapi: 3.0.0")
                .withServicesToNotMock(Collections.singletonList(new ExternalService("foo.com", 80)));

        assertThrows(
                UnsupportedOperationException.class,
                () -> p.getServicesToNotMock().add(new ExternalService("bar.com", 80))
        );
    }

    @Test
    public void testADriverThatDoesNotPublishSaysSoClearly() {

        /*
            The hook has a default rather than being abstract, so that adding it does not break
            every existing driver. A driver that is pointed at an AsyncAPI service without
            implementing it should fail with something that explains what to do.
         */
        SutController controller = new AsyncApiControllerWithoutPublishing();

        IllegalStateException e = assertThrows(
                IllegalStateException.class,
                () -> controller.executeAsyncApiAction(new AsyncApiActionDto(), new AsyncApiReplyDto())
        );

        assertTrue(e.getMessage().contains("executeAsyncApiAction"), e.getMessage());
    }

    /**
     * A driver that says its SUT is an AsyncAPI service, but never implements the publishing.
     */
    private static class AsyncApiControllerWithoutPublishing extends EmbeddedSutController {

        @Override
        public String startSut() {
            return null;
        }

        @Override
        public boolean isSutRunning() {
            return false;
        }

        @Override
        public void stopSut() {
        }

        @Override
        public String getPackagePrefixesToCover() {
            return null;
        }

        @Override
        public void resetStateOfSUT() {
        }

        @Override
        public List<AuthenticationDto> getInfoForAuthentication() {
            return null;
        }

        @Override
        public List<DbSpecification> getDbSpecifications() {
            return null;
        }

        @Override
        public ProblemInfo getProblemInfo() {
            return AsyncApiProblem.fromSchemaText("asyncapi: 3.0.0");
        }

        @Override
        public SutInfoDto.OutputFormat getPreferredOutputFormat() {
            return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
        }
    }
}
