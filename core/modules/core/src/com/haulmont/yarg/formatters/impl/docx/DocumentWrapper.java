package com.haulmont.yarg.formatters.impl.docx;

import com.haulmont.yarg.formatters.impl.DocxFormatterDelegate;
import org.docx4j.TraversalUtil;
import org.docx4j.model.structure.HeaderFooterPolicy;
import org.docx4j.model.structure.SectionWrapper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

import java.util.List;
import java.util.Set;

public class DocumentWrapper {
    protected DocxFormatterDelegate docxFormatter;
    protected WordprocessingMLPackage wordprocessingMLPackage;
    protected MainDocumentPart mainDocumentPart;
    protected Set<TableManager> tables;
    protected Set<TextWrapper> texts;

    public DocumentWrapper(DocxFormatterDelegate docxFormatter, WordprocessingMLPackage wordprocessingMLPackage) {
        this.docxFormatter = docxFormatter;
        this.wordprocessingMLPackage = wordprocessingMLPackage;
        this.mainDocumentPart = wordprocessingMLPackage.getMainDocumentPart();
        collectData();
    }

    protected void collectDataFromObjects(Object... objects) {
        for (Object object : objects) {
            if (object != null) {
                TextVisitor collectAliasesCallback = new TextVisitor(docxFormatter);
                new TraversalUtil(object, collectAliasesCallback);
                texts.addAll(collectAliasesCallback.textWrappers);
            }
        }
    }

    protected void collectData() {
        collectTables();
        collectTexts();
        collectHeadersAndFooters();
    }

    protected void collectHeadersAndFooters() {//collect data from headers
        List<SectionWrapper> sectionWrappers = wordprocessingMLPackage.getDocumentModel().getSections();
        for (SectionWrapper sw : sectionWrappers) {
            HeaderFooterPolicy hfp = sw.getHeaderFooterPolicy();
            collectDataFromObjects(hfp.getFirstHeader(), hfp.getDefaultHeader(), hfp.getEvenHeader(), hfp.getFirstFooter(), hfp.getDefaultFooter(), hfp.getEvenFooter());
        }
    }

    protected void collectTexts() {
        TextVisitor collectAliasesCallback = new TextVisitor(docxFormatter);
        new TraversalUtil(mainDocumentPart, collectAliasesCallback);
        texts = collectAliasesCallback.textWrappers;
    }

    protected void collectTables() {
        TableCollector collectTablesCallback = new TableCollector(docxFormatter);
        new TraversalUtil(mainDocumentPart, collectTablesCallback);
        tables = collectTablesCallback.tableManagers;
    }

    public Set<TableManager> getTables() {
        return tables;
    }

    public Set<TextWrapper> getTexts() {
        return texts;
    }
}
