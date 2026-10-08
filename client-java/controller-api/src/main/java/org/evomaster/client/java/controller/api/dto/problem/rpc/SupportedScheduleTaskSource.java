package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * Supported sources for identifying schedule task methods.
 */
public enum SupportedScheduleTaskSource {

    SPRING_SCHEDULED("org.springframework.scheduling.annotation.Scheduled"),

    EJB_SCHEDULE("javax.ejb.Schedule"),

    CUSTOMIZED(null);

    private final String name;

    SupportedScheduleTaskSource(String name) {
        this.name = name;
    }

    /**
     * @return the fully qualified annotation name for built-in task types.
     * For {@link #CUSTOMIZED}, the concrete name depends on the user-defined
     * annotation and is carried by the schedule task DTO.
     */
    public String getName() {
        return name;
    }
}
