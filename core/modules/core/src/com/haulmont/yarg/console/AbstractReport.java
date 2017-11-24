package com.haulmont.yarg.console;

import com.haulmont.yarg.structure.Report;
import com.haulmont.yarg.structure.ReportOutputType;
import com.haulmont.yarg.structure.impl.ReportBuilder;

import javax.sql.DataSource;

abstract public class AbstractReport {

    protected DataSource dataSource;

    public AbstractReport(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    abstract public Report getReport(ReportBuilder reportBuilder, String jsonString);

    abstract public ReportOutputType getOutputType();

    public String getAdditionalInfo() {
        return "Additional Info method was not overloaded!";
    }

    public void postprocessFile(String fileName) { return; }
}
