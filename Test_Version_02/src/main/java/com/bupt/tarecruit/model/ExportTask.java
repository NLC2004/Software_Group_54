package com.bupt.tarecruit.model;

/** Backup/export history task record for admin pages. */
public class ExportTask {
    public String id;
    public String dataSubject = "";
    public String dateRange = "";
    public String format = "CSV";
    public String taskType = "EXPORT"; // EXPORT | BACKUP
    public String status = "COMPLETED"; // COMPLETED | PROCESSING | FAILED
    public String generatorId = "";
    public String generatorName = "";
    public String fileName = "";
    public String errorMessage = "";
    public long createdAt;
    public long updatedAt;

    public ExportTask() {}
}
