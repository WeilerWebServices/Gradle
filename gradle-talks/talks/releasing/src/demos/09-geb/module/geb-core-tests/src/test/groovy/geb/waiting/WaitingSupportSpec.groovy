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
package geb.waiting

import geb.*
import geb.test.*
import spock.lang.*
import org.codehaus.groovy.transform.powerassert.PowerAssertionError

class WaitingSupportSpec extends GebSpecWithServer {

	def setupSpec() {
		server.get = { req, res ->
			res.outputStream << """
				<html>
				<head>
				  <script type="text/javascript" charset="utf-8">
				    function showIn(i) {
				      setTimeout(function() {
				        document.body.innerHTML = "<div>a</div>";
				      }, i * 1000);
				    }
				  </script>
				</head>
				<body>
				</body>
				</html>
			"""
		}
	}
		
	def setup() {
		go()
	}
	
	def "basic waiting"() {
		when:
		js.showIn(2)
		then:
		$("div").empty
		waitFor(3) { !$("div").empty }
	}

	def "basic waiting throwing exception"() {
		when:
		js.showIn(2)
		then:
		$("div").empty
		waitFor(3) { assert !$("div").empty; true }
	}
	
	def "failed waiting"() {
		when:
		js.showIn(3)
		waitFor(1) { !$("div").empty }
		then:
		WaitTimeoutException exception = thrown()
		exception.cause in PowerAssertionError
		exception.cause.message.contains('!$("div").empty')
	}

	def "failed waiting throwing exception"() {
		when:
		waitFor(2) { throw new IllegalArgumentException("1") }
		then:
		WaitTimeoutException e = thrown()
		e.cause instanceof IllegalArgumentException
	}
	
	def "larger interval than timeout"() {
		when:
		js.showIn(4)
		then:
		waitFor(1, 10) { $("div").empty }
	}

	def "message argument is appended to the exception message"() {
		when:
		waitFor(1, message: 'Some custom message') { false }

		then:
		WaitTimeoutException e = thrown()
		e.message =~ 'Some custom message'
	}

	def "larger interval than timeout throwing exception"() {
		when:
		js.showIn(4)
		then:
		waitFor(1, 10) { assert $("div").empty; true }
	}
	
	def "default variant"() {
		when:
		js.showIn(2)
		then:
		waitFor{ $("div").empty }
	}
	
	def "available on page"() {
		given:
		page WaitingSupportSpecPage
		when:
		js.showIn(3)
		then:
		$("div").empty
		waitForDiv()
	}
	
	def "available on module"() {
		given:
		page WaitingSupportSpecPage
		when:
		js.showIn(3)
		then:
		$("div").empty
		mod.waitForDiv()
	}
	
}

class WaitingSupportSpecPage extends Page {
	static content = {
		mod { module WaitingSupportSpecModule }
	}
	def waitForDiv() {
		waitFor { !$("div").empty }
	}
}

class WaitingSupportSpecModule extends Module {
	def waitForDiv() {
		waitFor { !$("div").empty }
	}
}