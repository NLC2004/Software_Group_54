package com.bupt.tarecruit.model;

/** Backup/export history task record for admin pages. */
/**
 * Represents the export task component of the TA recruitment system.
 */
public class ExportTask {
    /**
     * Stores the id value.
     */
    public String id;
    /**
     * Stores the data subject value.
     */
    public String dataSubject = "";
    /**
     * Stores the date range value.
     */
    public String dateRange = "";
    /**
     * Stores the format value.
     */
    public String format = "CSV";
    /**
     * Stores the task type value.
     */
    public String taskType = "EXPORT"; // EXPORT | BACKUP
    /**
     * Stores the status value.
     */
    public String status = "COMPLETED"; // COMPLETED | PROCESSING | FAILED
    /**
     * Stores the generator id value.
     */
    public String generatorId = "";
    /**
     * Stores the generator name value.
     */
    public String generatorName = "";
    /**
     * Stores the file name value.
     */
    public String fileName = "";
    /**
     * Stores the error message value.
     */
    public String errorMessage = "";
    /**
     * Stores the created at value.
     */
    public long createdAt;
    /**
     * Stores the updated at value.
     */
    public long updatedAt;

    /**
     * Creates a new export task instance.
     */
    public ExportTask() {}
}
