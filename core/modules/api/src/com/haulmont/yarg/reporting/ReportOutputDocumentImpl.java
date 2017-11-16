/*
 * Copyright 2013 Haulmont
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.haulmont.yarg.reporting;

import com.haulmont.yarg.structure.Report;
import com.haulmont.yarg.structure.ReportOutputType;

public class ReportOutputDocumentImpl implements ReportOutputDocument {
    protected Report report;
    protected byte[] content;
    protected String documentName;
    protected ReportOutputType reportOutputType;

    public ReportOutputDocumentImpl(Report report, byte[] content, String documentName, ReportOutputType reportOutputType) {
        this.report = report;
        this.content = content;
        this.documentName = documentName;
        this.reportOutputType = reportOutputType;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public void setReportOutputType(ReportOutputType reportOutputType) {
        this.reportOutputType = reportOutputType;
    }

    @Override
    public Report getReport() {
        return report;
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public String getDocumentName() {
        return documentName;
    }

    @Override
    public ReportOutputType getReportOutputType() {
        return reportOutputType;
    }
}