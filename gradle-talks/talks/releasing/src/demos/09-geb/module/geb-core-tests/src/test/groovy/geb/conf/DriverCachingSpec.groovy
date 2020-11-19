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
package geb.conf

import geb.*
import geb.driver.CachingDriverFactory
import org.openqa.selenium.htmlunit.HtmlUnitDriver

import spock.lang.*
import spock.util.concurrent.BlockingVariable

class DriverCachingSpec extends Specification {

	def num = 0
	
	private static class NullBuildAdapter implements BuildAdapter {
		String getBaseUrl() { null }
		File getReportsDir() { null }
	}

	def conf(cacheDriverPerThread = false) {
		def conf = new Configuration(new ConfigObject(), new Properties(), new NullBuildAdapter())
		conf.cacheDriverPerThread = cacheDriverPerThread
		conf.driverConf = { new HtmlUnitDriver() }
		
		assert conf.cacheDriver

		conf
	}

	def setupSpec() {
		CachingDriverFactory.clearCacheCache()
	}
	
	def "per thread caching yields a new driver on a different thread"() {
		given:
		def conf1 = conf(true)
		def conf2 = conf(true)

		and:
		def holder = new BlockingVariable()

		when:
		Thread.start { holder.set(conf2.driver) }
		
		and:
		def driver1 = conf1.driver
		def driver2 = holder.get()
		
		then:
		!driver1.is(driver2)
		
		and:
		driver1.is conf(true).driver
		
		when:
		CachingDriverFactory.clearCacheAndQuitDriver()
		
		then:
		! driver1.is (conf(true).driver)
		
		cleanup:
		driver1.quit()
		driver2.quit()
	}

	def "global caching yields the same driver on different threads"() {
		given:
		def conf1 = conf()
		def conf2 = conf()

		and:
		def holder = new BlockingVariable()

		when:
		Thread.start { holder.set(conf2.driver) }
		
		and:
		def driver1 = conf1.driver
		def driver2 = holder.get()
		
		then:
		driver1.is(driver2)
		
		and:
		driver1.is conf().driver
		
		when:
		CachingDriverFactory.clearCacheAndQuitDriver()
		
		then:
		! driver1.is (conf(true).driver)
		
		cleanup:
		driver1.quit()
		driver2.quit()
	}
	
	def cleanup() {
		CachingDriverFactory.clearCacheCache()
	}
	
}
