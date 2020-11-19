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
 * A reporter can take a snapshot of the browser state, using a given name as the
 * base for whatever it produces. A common usage of a reporter is to take snapshots
 * before and after test methods.
 * 
 * @see geb.report.ReporterSupport
 * @see geb.report.ScreenshotAndPageSourceReporter
 */
interface Reporter {
	
	/**
	 * Takes a snapshot of the given browser's state, using the given name
	 * as the base name for anything (e.g. file) that is produced.
	 */
	void writeReport(Browser browser, String label, File outputDir)
	
} 