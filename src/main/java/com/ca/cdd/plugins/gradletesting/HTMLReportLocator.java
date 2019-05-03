package com.ca.cdd.plugins.gradletesting;

import com.ca.rp.plugins.dto.model.ExternalTaskInputs;

public interface HTMLReportLocator {

    /**
     * Used by @{@link com.ca.rp.plugins.dto.model.TestSuiteResult#setReportFilename(ExternalTaskInputs, String, String, String...)}
     * @return
     */
    ExternalTaskInputs getTaskInputs();

    /**
     * Main HTML file
     *
     * Used by @{@link com.ca.rp.plugins.dto.model.TestSuiteResult#setReportFilename(ExternalTaskInputs, String, String, String...)}
     * @return
     */
    String getEntryPoint();

    /**
     * All files are relative to this folder
     *
     * Used by @{@link com.ca.rp.plugins.dto.model.TestSuiteResult#setReportFilename(ExternalTaskInputs, String, String, String...)}
     * @return
     */
    String getRootFolder();

    /**
     * CSS, JS and other files linked from Main HTML file
     * Used by @{@link com.ca.rp.plugins.dto.model.TestSuiteResult#setReportFilename(ExternalTaskInputs, String, String, String...)}
     * @return
     */
    String[] getExtraFiles();

    void onReportReady();
}
