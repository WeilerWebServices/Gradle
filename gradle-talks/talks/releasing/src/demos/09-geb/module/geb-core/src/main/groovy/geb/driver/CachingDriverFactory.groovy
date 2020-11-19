/* Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package geb.driver

import org.openqa.selenium.WebDriver

class CachingDriverFactory implements DriverFactory {

	private static interface Cache<T> {
		T get(Closure factory)
		T clear()
	}

	static private class SimpleCache<T> implements Cache<T> {
		private T cached
		synchronized T get(Closure factory) {
			if (cached == null) {
				cached = factory()
			}
			cached
		}
		synchronized T clear() {
			def prev = cached
			cached = null
			prev
		}
	}

	static private class ThreadLocalCache<T> implements Cache<T> {
		private ThreadLocal<T> threadLocal = new ThreadLocal()
		synchronized T get(Closure factory) {
			def cached = threadLocal.get()
			if (cached == null) {
				cached = factory()
				threadLocal.set(cached)
			}
			cached
		}
		synchronized T clear() {
			def prev = threadLocal.get()
			threadLocal.set(null)
			prev
		}
	}

	static private CACHE = new SimpleCache<Cache<DriverFactory>>()

	private final Cache<DriverFactory> cache
	private final DriverFactory innerFactory
	private final boolean quitOnShutdown

	private CachingDriverFactory(Cache<DriverFactory> cache, DriverFactory innerFactory, boolean quitOnShutdown) {
		this.cache = cache
		this.innerFactory = innerFactory
		this.quitOnShutdown = quitOnShutdown
	}

	static CachingDriverFactory global(DriverFactory innerFactory, boolean quitOnShutdown) {
		new CachingDriverFactory(CACHE.get {  new SimpleCache() }, innerFactory, quitOnShutdown)
	}

	static CachingDriverFactory perThread(DriverFactory innerFactory, boolean quitOnShutdown) {
		new CachingDriverFactory(CACHE.get { new ThreadLocalCache() }, innerFactory, quitOnShutdown)
	}

	WebDriver getDriver() {
		cache.get {
			def driver = innerFactory.driver
			if (quitOnShutdown) {
				addShutdownHook {
					try { driver.quit() } catch (Throwable e) {}
				}
			}
			driver
		}
	}

	static WebDriver clearCache() {
		CACHE.get { null }?.clear()
	}

	static WebDriver clearCacheAndQuitDriver() {
		def driver = clearCache()
		driver?.quit()
		driver
	}
	
	static clearCacheCache() {
		CACHE.clear()
	}

}