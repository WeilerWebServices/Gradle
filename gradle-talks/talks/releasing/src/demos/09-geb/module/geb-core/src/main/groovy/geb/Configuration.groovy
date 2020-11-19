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

import geb.buildadapter.SystemPropertiesBuildAdapter
import geb.report.Reporter
import geb.report.ScreenshotAndPageSourceReporter
import geb.waiting.Wait
import org.openqa.selenium.WebDriver
import geb.driver.*

/**
 * Represents a particular configuration of Geb.
 */
class Configuration {
	
	static private final DEFAULT_WAIT_RETRY_SECS = 0.1
	
	final ClassLoader classLoader
	final ConfigObject rawConfig
	final Properties properties
	final BuildAdapter buildAdapter
	
	private final Map<String, Wait> waits = null
	
	private WebDriver driver
	
	Configuration(Map rawConfig) {
		this(toConfigObject(rawConfig), null, null, null)
	}
		
	Configuration(ConfigObject rawConfig = null, Properties properties = null, BuildAdapter buildAdapter = null, ClassLoader classLoader = null) {
		this.classLoader = classLoader ?: new GroovyClassLoader()
		this.properties = properties == null ? System.properties : properties
		this.buildAdapter = buildAdapter ?: new SystemPropertiesBuildAdapter()
		this.rawConfig = rawConfig ?: new ConfigObject()
	}

	private static toConfigObject(Map rawConfig) {
		def configObject = new ConfigObject()
		configObject.putAll(rawConfig)
		configObject
	}
	
	Wait getWaitPreset(String name) {
		def preset = rawConfig.waiting.presets[name]
		def timeout = readValue(preset, 'timeout', getDefaultWaitTimeout())
		def retryInterval = readValue(preset, 'retryInterval', getDefaultWaitRetryInterval())
		
		new Wait(timeout, retryInterval)
	}
	
	Wait getDefaultWait() {
		new Wait(getDefaultWaitTimeout(), getDefaultWaitRetryInterval())
	}
	
	Wait getWait(Double timeout) {
		new Wait(timeout, getDefaultWaitRetryInterval())
	}

	Wait getWaitForParam(waitingParam) {
		if (waitingParam == true) {
			defaultWait
		} else if (waitingParam instanceof CharSequence) {
			getWaitPreset(waitingParam.toString())
		} else if (waitingParam instanceof Number && waitingParam > 0) {
			getWait(waitingParam.doubleValue())
		} else if (waitingParam instanceof Collection) {
			if (waitingParam.size() == 2) {
				def timeout = waitingParam[0]
				def retryInterval = waitingParam[1]

				if (timeout instanceof Number && retryInterval instanceof Number) {
					new Wait(timeout.doubleValue(), retryInterval.doubleValue())
				} else {
					throw new IllegalArgumentException("'wait' param has illegal value '$waitingParam' (collection elements must be numbers)")
				}
			} else {
				throw new IllegalArgumentException("'wait' param for content template ${this} has illegal value '$waitingParam' (collection must have 2 elements)")
			}
		} else {
			null
		}
	}

	/**
	 * The default {@code timeout} value to use for waiting (i.e. if unspecified).
	 * <p>
	 * Either the value at config path {@code waiting.timeout} or {@link geb.waiting.Wait#DEFAULT_TIMEOUT 5}.
	 */
	Double getDefaultWaitTimeout() {
		readValue(rawConfig.waiting, 'timeout', Wait.DEFAULT_TIMEOUT)
	}
	
	/**
	 * The default {@code retryInterval} value to use for waiting (i.e. if unspecified).
	 * <p>
	 * Either the value at config path {@code waiting.retryInterval} or {@link geb.waiting.Wait#DEFAULT_RETRY_INTERVAL 0.1}.
	 */
	Double getDefaultWaitRetryInterval() {
		readValue(rawConfig.waiting, 'retryInterval', Wait.DEFAULT_RETRY_INTERVAL)
	}
	
	/**
	 * Should the created driver be cached if there is no existing cached driver, of if there
	 * is a cached driver should it be used instead of creating a new one.
	 * <p>
	 * The value is the config entry {@code cacheDriver}, which defaults to {@code true}.
	 */
	boolean isCacheDriver() {
		readValue('cacheDriver', true)
	}

	/**
	 * Updates the {@code cacheDriver} config entry.
	 * 
	 * @see #isCacheDriver()
	 */
	void setCacheDriver(boolean flag) {
		rawConfig.cacheDriver = flag
	}

	/**
	 * The driver is to be cached, this setting controls whether or not the driver is cached per thread,
	 * or per
	 * <p>
	 * The value is the config entry {@code cacheDriverPerThread}, which defaults to {@code true}.
	 */
	boolean isCacheDriverPerThread() {
		readValue('cacheDriverPerThread', false)
	}

	/**
	 * Updates the {@code cacheDriverPerThread} config entry.
	 * 
	 * @see #isCacheDriverPerThread()
	 */
	void setCacheDriverPerThread(boolean flag) {
		rawConfig.cacheDriverPerThread = flag
	}
	
	/**
	 * If a cached driver is being used, should it be automatically quit when the JVM exits.
	 * <p>
	 * The value is the config entry {@code quitCachedDriverOnShutdown}, which defaults to {@code true}.
	 */
	boolean isQuitCachedDriverOnShutdown() {
		readValue('quitCachedDriverOnShutdown', true)
	}
	
	/**
	 * Sets whether or not the cached driver should be quit when the JVM shuts down.
	 */
	void setQuitCacheDriverOnShutdown(boolean flag) {
		rawConfig.quitCachedDriverOnShutdown = flag
	}
	
	/**
	 * Sets the driver configuration value.
	 * <p>
	 * This may be the class name of a driver implementation, a driver short name or a closure 
	 * that when invoked with no arguments returns a driver implementation.
	 * 
	 * @see #getDriver()
	 */
	void setDriverConf(value) {
		rawConfig.driver = value
	}
	
	/**
	 * Returns the configuration value for the driver.
	 * <p>
	 * This may be the class name of a driver implementation, a short name, or a closure
	 * that when invoked returns an actual driver.
	 * 
	 * @see #getDriver()
	 */
	def getDriverConf() {
		def value = properties.getProperty("geb.driver") ?: readValue("driver", null)
		if (value instanceof WebDriver) {
			throw new IllegalStateException(
				"The 'driver' config value is an instance of WebDriver. " +
				"You need to wrap the driver instance in a closure."
			)
		}
		value
	}

	/**
	 * Returns the config value {@code baseUrl}, or {@link geb.BuildAdapter#getBaseUrl()}.
	 */
	String getBaseUrl() {
		readValue("baseUrl", buildAdapter.baseUrl)
	}
	
	void setBaseUrl(baseUrl) {
		rawConfig.baseUrl = baseUrl == null ? null : baseUrl.toString()
	}
	
	/**
	 * Returns the config value {@code reportsDir}, or {@link geb.BuildAdapter#getReportsDir()}.
	 */
	File getReportsDir() {
		def reportsDir = readValue("reportsDir", buildAdapter.reportsDir)
		if (reportsDir == null) {
			null
		} else if (reportsDir instanceof File) {
			reportsDir
		} else {
			new File(reportsDir.toString())
		}
	}

	def setReportOnTestFailureOnly(boolean value) {
		rawConfig.reportOnTestFailureOnly = value
	}

	boolean isReportOnTestFailureOnly() {
		readValue("reportOnTestFailureOnly", false)
	}
	
	void setReportsDir(File reportsDir) {
		rawConfig.reportsDir = reportsDir
	}
	
	/**
	 * Returns the reporter implementation to use for taking snapshots of the browser's state.
	 * <p>
	 * Returns the config value {@code reporter}, or an instance of {@link geb.report.ScreenshotAndPageSourceReporter} if not explicitly set.
	 */
	Reporter getReporter() {
		def reporter = readValue("reporter", null)
		if (reporter == null) {
			reporter = new ScreenshotAndPageSourceReporter()
			setReporter(reporter)
		}
		reporter
	}
	
	/**
	 * Updates the {@code reporter} config entry.
	 * 
	 * @see #getReporter()
	 */
	void setReporter(Reporter reporter) {
		rawConfig.reporter = reporter
	}
	
	/**
	 * 
	 */
	WebDriver getDriver() {
		if (driver == null) {
			driver = createDriver()
		}
		
		driver
	}
	
	void setDriver(WebDriver driver) {
		this.driver = driver
	}
	
	protected WebDriver createDriver() {
		wrapDriverFactoryInCachingIfNeeded(getDriverFactory(getDriverConf())).driver
	}
	
	/**
	 * Whether or not to automatically clear the browser's cookies automatically.
	 * <p>
	 * Different integrations inspect this property at different times.
	 * <p>
	 * @return the config value for {@code autoClearCookies}, defaulting to {@code true} if not set.
	 */
	boolean isAutoClearCookies() {
		readValue('autoClearCookies', true)
	}

	/**
	 * Sets the auto clear cookies flag explicitly, overwriting any value from the config script.
	 */
	void setAutoClearCookies(boolean flag) {
		rawConfig.autoClearCookies = flag
	}
	
	protected DriverFactory getDriverFactory(driverValue) {
		if (driverValue instanceof CharSequence) {
			new NameBasedDriverFactory(classLoader, driverValue.toString())
		} else if (driverValue instanceof Closure) {
			new CallbackDriverFactory(driverValue)
		} else if (driverValue == null) {
			new DefaultDriverFactory(classLoader)
		} else {
			throw new DriverCreationException("Unable to determine factory for 'driver' config value '$driverValue'")
		}
	}
	
	protected DriverFactory wrapDriverFactoryInCachingIfNeeded(DriverFactory factory) {
		if (isCacheDriver()) {
			isCacheDriverPerThread() ? CachingDriverFactory.perThread(factory, isQuitCachedDriverOnShutdown()) : CachingDriverFactory.global(factory, isQuitCachedDriverOnShutdown())
		} else {
			factory
		}
	}

	protected readValue(String name, defaultValue) {
		readValue(rawConfig, name, defaultValue)
	}

	protected readValue(ConfigObject config, String name, defaultValue) {
		if (config.containsKey(name)) {
			config[name]
		} else {
			defaultValue
		}
	}
	
}