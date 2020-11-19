/*
 * Copyright 2011 the original author or authors.
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
package geb.navigator

import spock.lang.*

import geb.test.*
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Select

class SelectFactorySpec extends GebSpecWithServer {

	def setupSpec() {
		server.get = { req, res ->
			res.outputStream << """
			<html>
			<body>
				<select name="s"/>
			</body>
			</html>"""
		}
	}

	def setup() {
		go()
	}
	
	def factory
	
	def getSelect() {
		factory.createSelectFor(s().firstElement())
	}
	
	def "will load successfully when select is available"() {
		when:
		factory = new SelectFactory()
		
		then:
		select instanceof Select
	}
	
	def "will give nice error message when select is not available"() {
		given:
		factory = new SelectFactory() {
			protected ClassLoader getClassLoaderToUse() {
				new ClassLoader() {
					protected Class loadClass(String name, boolean resolve) {
						if (name == SelectFactory.SELECT_CLASS_NAME) {
							throw new ClassNotFoundException()
						} else {
							super.loadClass(name, resolve)
						}
					}
				}
			}
		}
		
		when:
		getSelect()
		
		then:
		def e = thrown(ClassNotFoundException)
		e.message.contains "This class is part of the selenium-support jar" 
	}

}