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
package geb

/**
 * Mechanism for 3rd parties to receive notification when a browser page instance changes
 */
interface PageChangeListener {
	
	/**
	 * Called first when the listener is registered, with {@code oldPage} as {@code null}
	 * and {@code newPage} as the current page at time of registration, then each time
	 * the browser's page instance changes.
	 * 
	 * Note that this is not when the browser navigates to a new page, but when it's page <i>object</i>
	 * changes.
	 */
	void pageWillChange(Browser browser, Page oldPage, Page newPage)
	
}