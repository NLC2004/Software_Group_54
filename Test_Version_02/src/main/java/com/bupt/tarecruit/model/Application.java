package com.bupt.tarecruit.model;

/**
 * Represents the application component of the TA recruitment system.
 */
public class Application {
    /**
     * Stores the id value.
     */
    public String id;
    /**
     * Stores the job id value.
     */
    public String jobId;
    /**
     * Stores the applicant id value.
     */
    public String applicantId;
    /**
     * Stores the cv file name value.
     */
    public String cvFileName;
    /**
     * Stores the cover letter value.
     */
    public String coverLetter;
    /**
     * Stores the status value.
     */
    public String status = "PENDING"; // PENDING, APPROVED, REJECTED, WITHDRAWN
    /**
     * Stores the priority value.
     */
    public int priority;
    /**
     * Stores the created at value.
     */
    public long createdAt;
    /**
     * Stores the updated at value.
     */
    public long updatedAt;
    /**
     * Stores the ai match json value.
     */
    public String aiMatchJson;
    /**
     * Stores the ai match model value.
     */
    public String aiMatchModel;
    /**
     * Stores the ai match updated at value.
     */
    public Long aiMatchUpdatedAt;

    /**
     * Creates a new application instance.
     */
    public Application() {}
}
