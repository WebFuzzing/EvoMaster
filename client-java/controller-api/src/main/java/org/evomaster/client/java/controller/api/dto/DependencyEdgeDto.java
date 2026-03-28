package org.evomaster.client.java.controller.api.dto;

import java.io.Serializable;

/**
 * Directed edge describing that parentObjectiveId must be covered before childObjectiveId.
 */
public class DependencyEdgeDto implements Serializable {

    public int parentObjectiveId;
    public int childObjectiveId;
}

