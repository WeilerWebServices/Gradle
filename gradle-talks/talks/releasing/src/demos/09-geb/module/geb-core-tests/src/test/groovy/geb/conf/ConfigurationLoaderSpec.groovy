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

package geb.conf

import geb.*
import spock.lang.*

import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ConfigurationLoaderSpec extends Specification {
	
	def env
	def config
	
	@Rule TemporaryFolder tmp = new TemporaryFolder()

	protected getLoader() {
		new ConfigurationLoader(env)
	}
	
	protected load(URL location) {
		config = loader.getConf(location, new GroovyClassLoader(getClass().classLoader))
	}
	
	protected getGoodScript() {
		getClass().getResource("good-conf.groovy")
	}
	
	def "load file from classpath with no env"() {
		when:
		load goodScript
		
		then:
		config.rawConfig.a == 1
	}
	
	def "load file from classpath with env"() {
		given:
		env = theEnv
		
		when:
		load goodScript
		
		then:
		config.rawConfig.a == value
		
		where:
		theEnv | value
		"e1"   | 2
		"e2"   | 3
	}

	def "load non-existent bad url"() {
		when:
		load new URL("file:///idontexist")
		
		then:
		thrown ConfigurationLoader.UnableToLoadException
	}

	def "verify default config class name"() {
		expect:
		loader.defaultConfigClassName == 'GebConfig'
	}

	def "ensure various test configuration scripts and classes are available"() {
		given:
		def loader = new GroovyClassLoader()

		and:
		tmp.newFile("GebConfigBothScriptAndClass.groovy") << "testValue = 'from script'"
		loader.addURL(tmp.root.toURL())

		expect:
		loader.getResource('GebConfigBothScriptAndClass.groovy')
		loader.loadClass('GebConfigBothScriptAndClass', false, true, true)
		!loader.getResource('GebConfigClassOnly.groovy')
		loader.loadClass('GebConfigClassOnly', false, true, true)
	}

	def "script config has precedence over class config if both available"() {
		given:
		def loader = new ConfigurationLoaderWithOverriddenConfigNames('GebConfigBothScriptAndClass')

		and:
		tmp.newFile("GebConfigBothScriptAndClass.groovy") << "testValue = 'from script'"
		loader.specialClassLoader.addURL(tmp.root.toURL())

		expect:
		loader.getConf().rawConfig.testValue == 'from script'
	}

	def "class config is used when there is no script config"() {
		given:
		def loader = new ConfigurationLoaderWithOverriddenConfigNames('GebConfigClassOnly')

		expect:
		loader.getConf().rawConfig.testValue == 'test value'
	}
}

class ConfigurationLoaderWithOverriddenConfigNames extends ConfigurationLoader {
	private final String name

	ConfigurationLoaderWithOverriddenConfigNames(String name) {
		this.name = name
	}

	String getDefaultConfigScriptResourcePath() {
		"${name}.groovy"
	}

	String getDefaultConfigClassName() {
		name
	}
}