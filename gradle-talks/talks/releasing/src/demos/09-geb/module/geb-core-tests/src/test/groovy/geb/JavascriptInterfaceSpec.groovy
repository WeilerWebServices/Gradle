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

import geb.test.*
import spock.lang.*

class JavascriptInterfaceSpec extends GebSpecWithServer {

	def setupSpec() {
		server.get = { req, res ->
			res.outputStream << """
			<html>
				<title>geb</title>
				<script type="text/javascript" charset="utf-8">
					var v1 = 1;
					var v2 = 2;
					function changeVars(newV1, newV2) {
						v1 = newV1;
						v2 = newV2;
						
						return "coming back";
					}
				</script>
			<body>
			</body>
			</html>"""
		}
	}
	
	def setup() {
		go() // make sure we have a fresh page
	}
	
	def "scripting style"() {
		expect: "we can read the vars"
		js.v1 == 1
		js.v2 == 2
		when: "we call a javascript method"
		def r = js.changeVars(3,4)
		then: "the method result is returned and it changed the vars"
		r == "coming back"
		js.v1 == 3
		js.v2 == 4
		when: "we use the javascript call syntax"
		r = js.exec(5, 6, "return changeVars(arguments[0], arguments[1]);")
		then: "the call result is returned and it changed the vars"
		r == "coming back"
		js.v1 == 5
		js.v2 == 6
	}

	def "page objects style"() {
		given:
		page JavascriptInterfaceSpecPage
		expect: "we can read the vars"
		v1 == 1
		v2 == 2
		when: "we call a javascript method"
		def r = changeVars(3,4)
		then: "the method result is returned and it changed the vars"
		r == "coming back"
		v1 == 3
		v2 == 4
		when: "we use the javascript call syntax"
		r = changeVarsViaExec(5, 6)
		then: "the call result is returned and it changed the vars"
		r == "coming back"
		v1 == 5
		v2 == 6
	}

	def "via a module"() {
		given:
		page JavascriptInterfaceSpecPage
		expect: "we can read the vars"
		mod.v1 == 1
		mod.v2 == 2
		when: "we call a javascript method"
		def r = mod.changeVars(3,4)
		then: "the method result is returned and it changed the vars"
		r == "coming back"
		mod.v1 == 3
		mod.v2 == 4
		when: "we use the javascript call syntax"
		r = mod.changeVarsViaExec(5, 6)
		then: "the call result is returned and it changed the vars"
		r == "coming back"
		mod.v1 == 5
		mod.v2 == 6
	}

	/**
	 * The JavascriptExecutor interface says NOTHING about script errors
	 * so there is not much we can do here unfortunately.
	 */
	def "missing property"() {
		when:
		js.asdfasdfasd
		then:
		thrown(Exception)
	}
	
	def "nested property"() {
		expect:
		js."document.title" == "geb"
	}
}

class JavascriptInterfaceSpecPage extends Page {

	static content = {
		v1 { js.v1 }
		v2 { js.v2 }
		mod { module JavascriptInterfaceSpecModule }
	}
	
	def changeVars(a, b) {
		js.changeVars(a, b)
	}

	def changeVarsViaExec(a, b) {
		js.exec(a, b, "return changeVars(arguments[0], arguments[1]);")
	}
}

class JavascriptInterfaceSpecModule extends Module {

	static content = {
		v1 { js.v1 }
		v2 { js.v2 }
	}
	
	def changeVars(a, b) {
		js.changeVars(a, b)
	}

	def changeVarsViaExec(a, b) {
		js.exec(a, b, "return changeVars(arguments[0], arguments[1]);")
	}
}