package org.evomaster.client.java.controller.problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Depending on which kind of SUT we are dealing with (eg, REST, GraphQL or SPA frontend),
 * there is different info that must be provided.
 * However, some time of info might be the same for all these kinds of problems.
 *
 * Created by arcuri82 on 05-Nov-18.
 */
public abstract class ProblemInfo {

    /**
     * In some cases, we might want to replace and mock away some interactions with external services
     * (eg, REST APIs).
     * Here, we can specify which ones should NOT be mocked, and rather use the actual services.
     */
    protected final List<ExternalService> servicesToNotMock;

    public ProblemInfo(List<ExternalService> servicesToNotMock) {
        this.servicesToNotMock = servicesToNotMock;
    }

    public ProblemInfo() {
        this(new ArrayList<>());
    }

    public List<ExternalService> getServicesToNotMock() {
        return Collections.unmodifiableList(servicesToNotMock);
    }

    public abstract ProblemInfo withServicesToNotMock(List<ExternalService> servicesToNotMock);


}
