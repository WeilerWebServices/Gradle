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
import spock.lang.*
import geb.waiting.Wait

class WaitingConfigurationSpec extends Specification {
	
	@Delegate Configuration config
	
	void setUserConf(String script) {
		config = new Configuration(new ConfigSlurper().parse(script))
	}
	
	def "defaults"() {
		when:
		userConf = ""
		
		then:
		defaultWait == new Wait()
		getWaitPreset("anything") == defaultWait
		getWait(10) == new Wait(10)
	}
	
	def "specified default wait values"() {
		when:
		userConf = """
			waiting {
				timeout = 20
				retryInterval = 40
			}
		"""
		
		then:
		defaultWait == new Wait(20, 40)
		getWaitPreset("anything") == new Wait(20, 40)
		getWait(10) == new Wait(10, 40)
	}
	
	def "presets"() {
		when:
		userConf = """
			waiting {
				timeout = 3
				presets {
					quick {
						timeout = 1
						retryInterval = 0.1
					}
					slow {
						timeout = 30
						retryInterval = 1
					}
					partial {
						retryInterval = 5
					}
				}
			}
		"""
		
		then:
		getWaitPreset("quick") == new Wait(1, 0.1)
		getWaitPreset("slow") == new Wait(30, 1)
		getWaitPreset("partial") == new Wait(3, 5)
	}
}