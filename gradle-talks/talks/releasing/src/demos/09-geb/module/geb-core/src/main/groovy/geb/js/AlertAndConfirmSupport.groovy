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

import geb.Configuration
import geb.waiting.Wait
import geb.waiting.WaitTimeoutException

class AlertAndConfirmSupport {

	private final static UNKNOWN = -1
	private final Closure javascriptInterfaceFactory
	private final Configuration config
	
	AlertAndConfirmSupport(Closure javascriptInterfaceFactory, Configuration config) {
		this.javascriptInterfaceFactory = javascriptInterfaceFactory
		this.config = config
	}
	
	private JavascriptInterface getJavascriptInterface() {
		def js = javascriptInterfaceFactory()
		if (js == null) {
			throw new IllegalStateException("javascriptInterfaceFactory returned null")
		} else if (!(js instanceof JavascriptInterface)) {
			throw new IllegalStateException("javascriptInterfaceFactory did not return a JavascriptInterface")
		}
		
		js
	}

	private getInstallGebStorageScript() {
		"""
			if (!window.geb) {
				window.geb = {};
			} 
		"""
	}
	
	private getInstallDialogStorageScript() {
		"""
			$installGebStorageScript
			
			if (!window.geb.dialogFunctions) {
				window.geb.dialogFunctions = new Array();
			}
			if (!window.geb.dialogMessages) {
				window.geb.dialogMessages = new Array();
			}
		"""
	}
	
	private popLastDialogMessage(JavascriptInterface js) {
		js.exec """
			if (window.geb) {
				return window.geb.dialogMessages.pop();
			} else {
				return $UNKNOWN;
			}
		"""
	}

	private popLastDialogFunctionOnto(JavascriptInterface js, String onto) {
		js.exec """
			if (window.geb) {
				window.$onto = window.geb.dialogFunctions.pop();
			}
		"""
	}
	
	private installAlert(JavascriptInterface js) {
		js.exec """
			$installDialogStorageScript
			
			window.geb.dialogFunctions.push(window.alert);
			window.geb.dialogMessages.push(null);
			
			window.alert = function(msg) {
				window.geb.dialogMessages.pop();
				window.geb.dialogMessages.push(msg);
				return true;
			};
		"""
	}
	
	private installConfirm(boolean ok, JavascriptInterface js) {
		js.exec """
			$installDialogStorageScript
			
			window.geb.dialogFunctions.push(window.confirm);
			window.geb.dialogMessages.push(null);
			
			window.confirm = function(msg) {
				window.geb.dialogMessages.pop();
				window.geb.dialogMessages.push(msg);
				return $ok;
			};
		"""
	}

	private captureDialog(Closure installer, String function, Closure actions, Wait wait = null) {
		def js = getJavascriptInterface()
		
		installer(js)
		
		def actionsError = null
		try {
			actions()
		} catch (Throwable e) {
			actionsError = e
		}
		def message
		try {
			message = wait ? wait.waitFor { popLastDialogMessage(js) } : popLastDialogMessage(js)
		} finally {
			// Need to do this even if actions raised exception
			popLastDialogFunctionOnto(js, function)
		}

		if (actionsError) {
			throw actionsError
		} else {
			message
		}
	}
	
	private captureAlert(Closure actions, waitParam = null) {
		captureDialog(this.&installAlert, 'alert', actions, config.getWaitForParam(waitParam))
	}

	private captureConfirm(boolean ok, Closure actions, waitParam = null) {
		captureDialog(this.&installConfirm.curry(ok), 'confirm', actions, config.getWaitForParam(waitParam))
	}

	def withAlert(Closure actions) {
		withAlert([:], actions)
	}

	def withAlert(Map params, Closure actions) {
		def message = captureAlert(actions, params.wait)
		if (message == null) {
			throw new AssertionError("no browser alert() was raised")
		} else if (message == UNKNOWN) {
			true
		} else {
			message.toString()
		}
	}

	void withNoAlert(Closure actions) {
		def message = captureAlert(actions)
		if (message != null && message != UNKNOWN) {
			throw new AssertionError("an unexpected browser alert() was raised (message: $message)")
		}
	}

	def withConfirm(Map params, Closure actions) {
		withConfirm(params, true, actions)
	}

	def withConfirm(Closure actions) {
		withConfirm([:], actions)
	}

	def withConfirm(boolean ok, Closure actions) {
		withConfirm([:], ok, actions)
	}

	def withConfirm(Map params, boolean ok, Closure actions) {
		def message = captureConfirm(ok, actions, params.wait)
		if (message == null) {
			throw new AssertionError("no browser confirm() was raised")
		} else if (message == UNKNOWN) {
			true
		} else {
			message.toString()
		}
	}

	void withNoConfirm(Closure actions) {
		def message = captureConfirm(false, actions)
		if (message != null && message != UNKNOWN) {
			throw new AssertionError("an unexpected browser confirm() was raised (message: $message)")
		}
	}
}