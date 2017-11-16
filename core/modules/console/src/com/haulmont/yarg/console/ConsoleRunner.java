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
package com.haulmont.yarg.console;

import com.haulmont.yarg.formatters.factory.DefaultFormatterFactory;
import com.haulmont.yarg.formatters.impl.doc.connector.OfficeIntegration;
import com.haulmont.yarg.loaders.factory.DefaultLoaderFactory;
import com.haulmont.yarg.loaders.impl.GroovyDataLoader;
import com.haulmont.yarg.loaders.impl.JsonDataLoader;
import com.haulmont.yarg.loaders.impl.SqlDataLoader;
import com.haulmont.yarg.reporting.DataExtractorImpl;
import com.haulmont.yarg.reporting.Reporting;
import com.haulmont.yarg.reporting.RunParams;
import com.haulmont.yarg.structure.Report;
import com.haulmont.yarg.structure.ReportOutputType;
import com.haulmont.yarg.structure.ReportParameter;
import com.haulmont.yarg.structure.ReportTemplate;
import com.haulmont.yarg.structure.impl.ReportBuilder;
import com.haulmont.yarg.structure.impl.ReportTemplateBuilder;
import com.haulmont.yarg.structure.xml.XmlReader;
import com.haulmont.yarg.structure.xml.impl.DefaultXmlReader;
import com.haulmont.yarg.util.converter.ObjectToStringConverter;
import com.haulmont.yarg.util.converter.ObjectToStringConverterImpl;
import com.haulmont.yarg.util.groovy.DefaultScriptingImpl;
import com.haulmont.yarg.util.properties.DefaultPropertiesLoader;
import com.haulmont.yarg.util.properties.PropertiesLoader;
import net.openhft.compiler.CompilerUtils;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConsoleRunner {
    public static final String PROPERTIES_PATH = "prop";
    public static final String XML_PATH = "rp";
    public static final String OUTPUT_PATH = "op";
    public static final String TEMPLATE_CODE = "tc";
    public static final String REPORT_PARAMETER = "P";
    public static final String CLASS_PATH = "cl";
    public static final String TEMPLATE_PATH = "tp";
    public static final String JSON_PATH = "json";
    public static final String YARG_PACKAGE_NAME = "com.haulmont.yarg.console.";
    public static volatile boolean doExitWhenFinished = true;

    protected static ObjectToStringConverter converter = new ObjectToStringConverterImpl();


    public static void main(String[] args) {
        Options options = createOptions();

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);


            if ((!cmd.hasOption(XML_PATH) && !cmd.hasOption(CLASS_PATH)) || !cmd.hasOption(OUTPUT_PATH)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("report", options);
                System.exit(-1);
            }

            String templateCode = cmd.getOptionValue(TEMPLATE_CODE, ReportTemplate.DEFAULT_TEMPLATE_CODE);
            PropertiesLoader propertiesLoader = new DefaultPropertiesLoader(
                    cmd.getOptionValue(PROPERTIES_PATH, DefaultPropertiesLoader.DEFAULT_PROPERTIES_PATH));

            Reporting reporting = new ReportEngineCreator().createReportingEngine(propertiesLoader);

            Report report = null;
            AbstractReport abstractReport = null;
            if (cmd.hasOption(CLASS_PATH)) {
                String templatePath = cmd.getOptionValue(TEMPLATE_PATH);
                String templateFileExtension = FilenameUtils.getExtension(templatePath);
                String templateFilename = FilenameUtils.getBaseName(templatePath) + "." + templateFileExtension;
                System.out.println("TEMPLATE: " + templateFilename);
                ReportBuilder reportBuilder = new ReportBuilder();
                ReportTemplateBuilder reportTemplateBuilder = null;
                System.out.println("FILENAME: " + FilenameUtils.getBaseName(templatePath));
                System.out.println("TYPE: " + templateFileExtension);

                ReportOutputType outputType;
                if ("doc".equals(templateFileExtension) || "docx".equals(templateFileExtension)) {
                    outputType = ReportOutputType.docx;
                } else if ("xls".equals(templateFileExtension) || "xlsx".equals(templateFileExtension)) {
                    outputType = ReportOutputType.xls;
                } else {
                    throw new MissingArgumentException("Wrong output file type!");
                }

                try {
                    reportTemplateBuilder = new ReportTemplateBuilder()
                            .documentName(templateFilename)
                            .documentPath(templatePath)
                            .outputNamePattern(cmd.getOptionValue(OUTPUT_PATH))
                            .outputType(outputType)
                            .readFileFromPath();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                reportBuilder.template(reportTemplateBuilder.build());
                String json = parseJsonFile(cmd.getOptionValue(JSON_PATH));
                abstractReport = compileAndGetReport(cmd.getOptionValue(CLASS_PATH));
                report = abstractReport.getReport(reportBuilder, json);
            } else if (cmd.hasOption(XML_PATH)) {
                XmlReader xmlReader = new DefaultXmlReader();
                report = xmlReader.parseXml(FileUtils.readFileToString(new File(cmd.getOptionValue(XML_PATH))));
            }


            Map<String, Object> params = parseReportParams(cmd, report);

            reporting.runReport(new RunParams(report)
                            .templateCode(templateCode)
                            .params(params),
                    new FileOutputStream(cmd.getOptionValue(OUTPUT_PATH)));

            if (abstractReport !=null) {
                System.out.println("\nAdditional data provided by report: " + abstractReport.getAdditionalInfo());
            }
            if (doExitWhenFinished) {
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (doExitWhenFinished) {
                System.exit(-1);
            }
        }
    }

    private static String parseJsonFile(String jsonFilePath) {
        if (jsonFilePath == null) {
            return null;
        }
        String fileAsString = null;
        try {
            File jsonFile = new File(jsonFilePath);
            fileAsString = FileUtils.readFileToString(jsonFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileAsString;
    }

    private static Map<String, Object> parseReportParams(CommandLine cmd, Report report) {
        if (cmd.hasOption(REPORT_PARAMETER)) {
            Map<String, Object> params = new HashMap<String, Object>();
            Properties optionProperties = cmd.getOptionProperties(REPORT_PARAMETER);
            for (ReportParameter reportParameter : report.getReportParameters()) {
                String paramValueStr = optionProperties.getProperty(reportParameter.getAlias());
                if (paramValueStr != null) {
                    params.put(reportParameter.getAlias(),
                            converter.convertFromString(reportParameter.getParameterClass(), paramValueStr));
                }
            }

            return params;
        } else {
            return Collections.emptyMap();
        }
    }

    private static Reporting createReportingEngine(PropertiesLoader propertiesLoader) throws IOException {
        DefaultFormatterFactory formatterFactory = new DefaultFormatterFactory();

        Reporting reporting = new Reporting();
        Properties properties = propertiesLoader.load();
        String openOfficePath = properties.getProperty(PropertiesLoader.CUBA_REPORTING_OPENOFFICE_PATH);
        String openOfficePorts = properties.getProperty(PropertiesLoader.CUBA_REPORTING_OPENOFFICE_PORTS);
        if (StringUtils.isNotBlank(openOfficePath) && StringUtils.isNotBlank(openOfficePorts)) {
            String[] portsStr = openOfficePorts.split("[,|]");
            Integer[] ports = new Integer[portsStr.length];
            for (int i = 0, portsStrLength = portsStr.length; i < portsStrLength; i++) {
                String str = portsStr[i];
                ports[i] = Integer.valueOf(str);
            }

            OfficeIntegration officeIntegration = new OfficeIntegration(openOfficePath, ports);
            formatterFactory.setOfficeIntegration(officeIntegration);

            String openOfficeTimeout = properties.getProperty(PropertiesLoader.CUBA_REPORTING_OPENOFFICE_TIMEOUT);
            if (StringUtils.isNotBlank(openOfficeTimeout)) {
                officeIntegration.setTimeoutInSeconds(Integer.valueOf(openOfficeTimeout));
            }

            String displayDeviceAvailable = properties.getProperty(PropertiesLoader.CUBA_REPORTING_OPENOFFICE_DISPLAY_DEVICE_AVAILABLE);
            if (StringUtils.isNotBlank(displayDeviceAvailable)) {
                officeIntegration.setDisplayDeviceAvailable(Boolean.valueOf(displayDeviceAvailable));
            }
        }

        reporting.setFormatterFactory(formatterFactory);
        SqlDataLoader sqlDataLoader = new PropertiesSqlLoaderFactory(propertiesLoader).create();
        GroovyDataLoader groovyDataLoader = new GroovyDataLoader(new DefaultScriptingImpl());
        JsonDataLoader jsonDataLoader = new JsonDataLoader();

        DefaultLoaderFactory loaderFactory = new DefaultLoaderFactory()
                .setSqlDataLoader(sqlDataLoader)
                .setGroovyDataLoader(groovyDataLoader)
                .setJsonDataLoader(jsonDataLoader);
        reporting.setLoaderFactory(loaderFactory);

        String putEmptyRowIfNoDataSelected = properties.getProperty(PropertiesLoader.CUBA_REPORTING_PUT_EMPTY_ROW_IF_NO_DATA_SELECTED);
        DataExtractorImpl dataExtractor = new DataExtractorImpl(loaderFactory);
        dataExtractor.setPutEmptyRowIfNoDataSelected(Boolean.parseBoolean(putEmptyRowIfNoDataSelected));
        reporting.setDataExtractor(dataExtractor);

        if (sqlDataLoader != null) {
            DatasourceHolder.dataSource = sqlDataLoader.getDataSource();
        }
        return reporting;
    }

    private static AbstractReport compileAndGetReport(String reportPath) {
        AbstractReport report = null;
        try {
            File reportFile = new File(reportPath);
            String fileAsString = FileUtils.readFileToString(reportFile);
            Class reportClass = CompilerUtils.CACHED_COMPILER.loadFromJava(YARG_PACKAGE_NAME + FilenameUtils.getBaseName(reportPath), fileAsString);
            Constructor dataSourceConstructor = reportClass.getConstructor(DataSource.class);
            report = (AbstractReport) dataSourceConstructor.newInstance(DatasourceHolder.dataSource);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return report;
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption(PROPERTIES_PATH, true, "reporting properties path");
        options.addOption(XML_PATH, true, "target xml report path");
        options.addOption(CLASS_PATH, true, "compiled report class path");
        options.addOption(OUTPUT_PATH, true, "output document path");
        options.addOption(TEMPLATE_CODE, true, "template code");
        options.addOption(TEMPLATE_PATH, true, "xls template path");
        options.addOption(JSON_PATH, true, "JSON options file path");
        OptionBuilder
                .withArgName("parameter=value")
                .hasOptionalArgs()
                .withValueSeparator()
                .withDescription("report parameter");
        Option reportParam = OptionBuilder.create(REPORT_PARAMETER);
        options.addOption(reportParam);
        return options;
    }
}