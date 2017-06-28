package com.haulmont.yarg.console;

import com.haulmont.yarg.structure.Report;
import com.haulmont.yarg.structure.ReportOutputType;
import com.haulmont.yarg.structure.impl.ReportBuilder;

abstract public class AbstractReport {

    abstract public Report getReport(ReportBuilder reportBuilder, String jsonString);

    abstract public ReportOutputType getOutputType();
}
