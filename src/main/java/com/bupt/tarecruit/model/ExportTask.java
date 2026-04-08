package com.bupt.tarecruit.model;

/** Export history task record for admin statistics page. */
public class ExportTask {
    public String id;
    public String dataSubject = "";
    public String dateRange = "";
    public String format = "CSV";
    public String status = "COMPLETED"; // COMPLETED | PROCESSING | FAILED
    public String generatorId = "";
    public String generatorName = "";
    public String fileName = "";
    public String errorMessage = "";
    public long createdAt;
    public long updatedAt;

    public ExportTask() {}
}
