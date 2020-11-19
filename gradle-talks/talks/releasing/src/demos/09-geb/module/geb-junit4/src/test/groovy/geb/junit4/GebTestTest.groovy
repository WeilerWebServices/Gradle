/* Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geb.junit4

import geb.test.CallbackHttpServer
import geb.Page
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4)
class GebTestTest extends GebTest {

	def server = new CallbackHttpServer()
	
	@Before
	void setUp() {
		server.start()
		server.get = { req, res ->
			res.outputStream << """
			<html>
			<body>
				<div class="d1" id="d1">d1</div>
			</body>
			</html>"""
		}
		browser.baseUrl = server.baseUrl
	}
	
	@Test
	void missingMethodsAreInvokedOnTheDriverInstance() {
		// This also verifies that the driver instance is instantiated correctly
		go("/")
	}

	@Test
	void missingPropertyAccessesAreRequestedOnTheDriverInstance() {
		page SomePage
		assert prop == 1
	}

	@Test
	void missingPropertyAssignmentsAreForwardedToTheDriverInstance() {
		page SomePage
		prop = 2
		assert prop == 2
	}

	@After
	void tearDown() {
		server.stop()
		super.tearDown()
	}
}

class SomePage extends Page {
	def prop = 1
}