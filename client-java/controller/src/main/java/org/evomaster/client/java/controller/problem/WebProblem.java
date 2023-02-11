package org.evomaster.client.java.controller.problem;

import java.util.List;

public class WebProblem extends ProblemInfo{

    private final String urlPathOfStartingPage;

    public WebProblem(String urlPathOfStartingPage) {
        this.urlPathOfStartingPage = urlPathOfStartingPage;
    }

    public String getUrlPathOfStartingPage() {
        return urlPathOfStartingPage;
    }

    @Override
    public ProblemInfo withServicesToNotMock(List<ExternalService> servicesToNotMock) {
        WebProblem p = new WebProblem(this.urlPathOfStartingPage);
        p.servicesToNotMock.addAll(servicesToNotMock);
        return p;
    }
}
