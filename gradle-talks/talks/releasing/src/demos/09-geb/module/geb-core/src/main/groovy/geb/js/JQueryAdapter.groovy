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
package geb.js

import org.openqa.selenium.WebElement
import geb.navigator.Navigator

class JQueryAdapter {

	private final Navigator navigator
	
	JQueryAdapter(Navigator navigator) {
		this.navigator = navigator
	}
	
	private _callJQueryMethod(String name, args) {
		def browser = navigator.browser
		def elements = navigator.allElements()
		
		if (elements) {
			browser.js.exec(*elements, "EOE", *args, """
				var elements = new Array();
				var callArgs = new Array();
				var collectingElements = true;

				for (j = 0; j < arguments.length; ++j) {
					var arg = arguments[j];

					if (collectingElements == true && arg == "EOE") {
						collectingElements = false;
					} else if (collectingElements) {
						elements.push(arg);
					} else {
						callArgs.push(arg);
					}
				}
				
				var o = jQuery(elements);
				var r = o.${name}.apply(o, callArgs);
				return (typeof r == "object") ? r.toArray() : r;
			""")
		} else {
			null
		}
	}
	
	def methodMissing(String name, args) {
		def result = _callJQueryMethod(name, args)
		if (result instanceof WebElement || result instanceof List) {
			Navigator.on(navigator.browser, result)
		} else {
			result
		}
	}

}