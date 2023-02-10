package org.evomaster.client.java.controller.problem;

import java.util.List;

public class WebProblem extends ProblemInfo{

    private final String urlOfStartingPage;

    public WebProblem(String urlOfStartingPage) {
        this.urlOfStartingPage = urlOfStartingPage;
    }

    public String getUrlOfStartingPage() {
        return urlOfStartingPage;
    }

    @Override
    public ProblemInfo withServicesToNotMock(List<ExternalService> servicesToNotMock) {
        WebProblem p = new WebProblem(this.urlOfStartingPage);
        p.servicesToNotMock.addAll(servicesToNotMock);
        return p;
    }
}
