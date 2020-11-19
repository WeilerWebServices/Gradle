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

import spock.lang.*
import geb.test.*
import geb.error.InvalidPageContent

class ModulesSpec extends GebSpecWithServer {

	def setupSpec() {
		responseHtml {
			body {
				div('class': 'a') { p('a') }
				div('class': 'b') { p('a') }
				div('class': 'c') {
					div('class': 'd') { p('d') }
				}
			}
		}
	}

	def "modules"() {
		when:
		to ModulesSpecPage
		then:
		divNoBase("a").p.text() == "a"
		divWithBase("a").p.text() == "a"
		divWithBaseAndSpecificBaseAndParam.p.text() == "d"
		divWithBaseAndSpecificBaseAndParam.p.text() == "d"
		divA.p.text() == "a"
		divC.innerDiv.p.text() == "d"
		divCWithRelativeInner.innerDiv.p.text() == "d"
	}

	def "call in mixed in method from TextMatchingSupport"() {
		when:
		to ModulesSpecPage
		then:
		divA.contains("b").matches("abc")
	}

	def "module base must return a navigator"() {
		when:
		to ModulesSpecPage
		badBase
		then:
		thrown(InvalidPageContent)
	}

	@Issue("http://jira.codehaus.org/browse/GEB-2")
	def "can access module instance methods from content"() {
		when:
		to ModulesSpecPage
		then:
		instanceMethod.val == 3
	}

	@Issue("http://jira.codehaus.org/browse/GEB-38")
	def "can coerce module to boolean"() {
		when:
		to ModulesSpecPage
		then:
		optional("a")
		!optional("e")
		!optional("e").p
	}

	def 'content created with moduleList contains a list of expected Module instances and of expected size'() {
		when:
		to ModulesSpecPage

		then:
		repeating.size() == 4
		repeating.every { it.class == ModulesSpecDivModuleNoLocator }
		repeatingWithParam.size() == 4
		repeatingWithParam.every { it.class == ModulesSpecDivModuleWithLocator }
	}

	@Unroll
	def 'content created with moduleList contains consecutive modules'() {
		when:
		to ModulesSpecPage

		then:
		repeating[index].p.text() == repeatingText
		repeatingWithParam[index] as Boolean == repeatingWithParamAvailable

		where:
		index | repeatingText | repeatingWithParamAvailable
		0     | 'a'           | false
		1     | 'a'           | false
		2     | 'd'           | true
		3     | 'd'           | false
	}

	def 'moduleList also supports ranges'() {
		when:
		to ModulesSpecPage

		then:
		repeating(1..2)*.p*.text() == ['a', 'd']
	}

	@Unroll
	def 'indexed moduleList content definition works as expected'() {
		when:
		to ModulesSpecPage

		then:
		repeating(index).p.text() == repeatingText
		repeatingWithParam(index) as Boolean == repeatingWithParamAvailable

		where:
		index | repeatingText | repeatingWithParamAvailable
		0     | 'a'           | false
		1     | 'a'           | false
		2     | 'd'           | true
		3     | 'd'           | false
	}
}

class ModulesSpecPage extends Page {
	static content = {
		// A module that doesn't define a locator, given one at construction
		divNoBase { module ModulesSpecDivModuleNoLocator, $("div.$it") }

		// A module that defines a locator, given a param at construction
		divWithBase { module ModulesSpecDivModuleWithLocator, className: it }

		// A module that defines a location, and uses a param given at construction in the locator
		divWithBaseAndSpecificBaseAndParam { module ModulesSpecDivModuleWithLocator, $("div.c"), className: "d" }

		// A module that defines a location, and is contructed with no base or params
		divA { module ModulesSpecSpecificDivModule }

		// A module that itself has a module
		divC { module ModulesSpecDivModuleWithNestedDiv }

		// A module whose inner module is defined by the owner module's base
		divCWithRelativeInner { module ModulesSpecDivModuleWithNestedDivRelativeToModuleBase }

		badBase { module ModulesSpecBadBase }

		instanceMethod { module InstanceMethodModule }

		optional(required: false) { module OptionalModule, $("div.$it") }

		// A list of modules, with the base of each module being set to the nth given navigator
		repeating { index -> moduleList ModulesSpecDivModuleNoLocator, $('div'), index }
		repeatingWithParam(required: false) { index ->
			moduleList ModulesSpecDivModuleWithLocator, $('div'), index, className: 'd'
		}
	}
}

class ModulesSpecDivModuleNoLocator extends Module {
	static content = {
		p { $("p") }
	}
}

class ModulesSpecDivModuleWithLocator extends Module {
	def className
	static base = { $("div.$className") }
	static content = {
		p { $("p") }
	}
}

class ModulesSpecSpecificDivModule extends Module {
	static base = { $("div.a") }
	static content = {
		p { $("p") }
	}
}

class ModulesSpecDivModuleWithNestedDiv extends Module {
	static base = { $("div.c") }
	static content = {
		innerDiv { module ModulesSpecDivModuleWithLocator, className: "d" }
	}
}

class ModulesSpecDivModuleWithNestedDivRelativeToModuleBase extends Module {
	static base = { $("div.c") }
	static content = {
		innerDiv { module ModulesSpecDivModuleWithLocator, $(), className: "d" }
	}
}

class ModulesSpecBadBase extends Module {
	static base = { 1 }
}

class InstanceMethodModule extends Module {
	static content = {
		val { getValue() }
	}

	def getValue() { 3 }
}

class OptionalModule extends Module {
	static content = {
		p(required: false) { $("p") }
	}
}