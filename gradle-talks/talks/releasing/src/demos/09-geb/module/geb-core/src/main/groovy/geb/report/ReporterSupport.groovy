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

/**
 * Common support for reporter implemenations.
 */
abstract class ReporterSupport implements Reporter {
	
	/**
	 * Gets a file reference for the object with the given name and extension within the dir.
	 */
	protected getFile(File dir, String name, String extension) {
		new File(dir, "${escapeFileName(name)}.${escapeFileName(extension)}")
	}
	
	/**
	 * Replaces all non word chars with underscores to avoid using reserved characters in file paths
	 */
	protected escapeFileName(String name) {
		name.replaceAll("[^\\w\\s-]", "_")
	}
	
	static String toTestReportLabel(int testCounter, int reportCounter, String methodName, String label) {
		def numberFormat = "%03d"
		"${String.format(numberFormat, testCounter)}-${String.format(numberFormat, reportCounter)}-$methodName-$label"
	}
}