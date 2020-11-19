/* Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package geb.report

import geb.Browser

/**
 * Writes the source content of the browser's current page as a html file.
 */
class PageSourceReporter extends ReporterSupport {

	static public final NO_PAGE_SOURCE_SUBSTITUTE = "-- no page source --"
		
	void writeReport(Browser browser, String label, File outputDir) {
		writePageSource(getReportFile(browser, label, outputDir), browser)
	}
	
	protected getReportFile(Browser browser, String label, File outputDir) {
		getFile(outputDir, label, getPageSourceFileExtension(browser))
	}
	
	protected writePageSource(File file, Browser browser) {
		file.write(getPageSource(browser))
	}
	
	protected getPageSource(Browser browser) {
		browser.driver.pageSource ?: NO_PAGE_SOURCE_SUBSTITUTE
	}
	
	protected getPageSourceFileExtension(Browser browser) {
		"html"
	}

}