/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnExposedEndpoint}.
 *
 * @author Brian Clozel
 */
public class ConditionalOnExposedEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(AllEndpointsConfiguration.class);

	@Test
	public void outcomeShouldMatchDefaults() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("info")
				.hasBean("health").doesNotHaveBean("spring").doesNotHaveBean("test"));
	}

	@Test
	public void outcomeWhenIncludeAllWebShouldMatch() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=*")
				.run((context) -> assertThat(context).hasBean("info").hasBean("health")
						.hasBean("test").hasBean("spring"));
	}

	@Test
	public void outcomeWhenIncludeAllJmxButJmxDisabledShouldMatchDefaults() {
		this.contextRunner
				.withPropertyValues("management.endpoints.jmx.exposure.include=*")
				.run((context) -> assertThat(context).hasBean("info").hasBean("health")
						.doesNotHaveBean("spring").doesNotHaveBean("test"));
	}

	@Test
	public void outcomeWhenIncludeAllJmxAndJmxEnabledShouldMatch() {
		this.contextRunner
				.withPropertyValues("management.endpoints.jmx.exposure.include=*",
						"spring.jmx.enabled=true")
				.run((context) -> assertThat(context).hasBean("info").hasBean("health")
						.hasBean("test").hasBean("spring"));
	}

	@Test
	public void outcomeWhenIncludeAllWebAndExcludeMatchesShouldNotMatch() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=*",
						"management.endpoints.web.exposure.exclude=spring,info")
				.run((context) -> assertThat(context).hasBean("health").hasBean("test")
						.doesNotHaveBean("info").doesNotHaveBean("spring"));
	}

	@Test
	public void outcomeWhenIncludeMatchesAndExcludeMatchesShouldNotMatch() {
		this.contextRunner.withPropertyValues(
				"management.endpoints.web.exposure.include=info,health,spring,test",
				"management.endpoints.web.exposure.exclude=spring,info")
				.run((context) -> assertThat(context).hasBean("health").hasBean("test")
						.doesNotHaveBean("info").doesNotHaveBean("spring"));
	}

	@Test
	public void outcomeWhenIncludeMatchesShouldMatch() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=spring")
				.run((context) -> assertThat(context).hasBean("spring")
						.doesNotHaveBean("health").doesNotHaveBean("info")
						.doesNotHaveBean("test"));
	}

	@Test
	public void outcomeWhenIncludeMatchesWithCaseShouldMatch() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=sPRing")
				.run((context) -> assertThat(context).hasBean("spring")
						.doesNotHaveBean("health").doesNotHaveBean("info")
						.doesNotHaveBean("test"));
	}

	@Test
	public void outcomeWhenIncludeMatchesAndExcludeAllShouldNotMatch() {
		this.contextRunner.withPropertyValues(
				"management.endpoints.web.exposure.include=info,health,spring,test",
				"management.endpoints.web.exposure.exclude=*")
				.run((context) -> assertThat(context).doesNotHaveBean("health")
						.doesNotHaveBean("info").doesNotHaveBean("spring")
						.doesNotHaveBean("test"));
	}

	@Test
	public void outcomeWhenIncludeMatchesShoulMatchWithExtensionsAndComponents() {
		this.contextRunner
				.withUserConfiguration(
						ComponentEnabledIfEndpointIsExposedConfiguration.class)
				.withPropertyValues("management.endpoints.web.exposure.include=spring")
				.run((context) -> assertThat(context).hasBean("spring")
						.hasBean("springComponent").hasBean("springExtension")
						.doesNotHaveBean("info").doesNotHaveBean("health")
						.doesNotHaveBean("test"));
	}

	@Test
	public void outcomeWithNoEndpointReferenceShouldFail() {
		this.contextRunner
				.withUserConfiguration(
						ComponentWithNoEndpointReferenceConfiguration.class)
				.withPropertyValues("management.endpoints.web.exposure.include=*")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure().getCause().getMessage())
							.contains(
									"No endpoint is specified and the return type of the @Bean method "
											+ "is neither an @Endpoint, nor an @EndpointExtension");
				});
	}

	@Test
	public void outcomeOnCloudFoundryShouldMatchAll() {
		this.contextRunner.withPropertyValues("VCAP_APPLICATION:---")
				.run((context) -> assertThat(context).hasBean("info").hasBean("health")
						.hasBean("spring").hasBean("test"));
	}

	@Endpoint(id = "health")
	static class HealthEndpoint {

	}

	@Endpoint(id = "info")
	static class InfoEndpoint {

	}

	@Endpoint(id = "spring")
	static class SpringEndpoint {

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

	}

	@EndpointExtension(endpoint = SpringEndpoint.class, filter = TestFilter.class)
	static class SpringEndpointExtension {

	}

	static class TestFilter implements EndpointFilter<ExposableEndpoint<?>> {

		@Override
		public boolean match(ExposableEndpoint<?> endpoint) {
			return true;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AllEndpointsConfiguration {

		@Bean
		@ConditionalOnExposedEndpoint
		public HealthEndpoint health() {
			return new HealthEndpoint();
		}

		@Bean
		@ConditionalOnExposedEndpoint
		public InfoEndpoint info() {
			return new InfoEndpoint();
		}

		@Bean
		@ConditionalOnExposedEndpoint
		public SpringEndpoint spring() {
			return new SpringEndpoint();
		}

		@Bean
		@ConditionalOnExposedEndpoint
		public TestEndpoint test() {
			return new TestEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ComponentEnabledIfEndpointIsExposedConfiguration {

		@Bean
		@ConditionalOnExposedEndpoint(endpoint = SpringEndpoint.class)
		public String springComponent() {
			return "springComponent";
		}

		@Bean
		@ConditionalOnExposedEndpoint
		public SpringEndpointExtension springExtension() {
			return new SpringEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ComponentWithNoEndpointReferenceConfiguration {

		@Bean
		@ConditionalOnExposedEndpoint
		public String springcomp() {
			return "springcomp";
		}

	}

}
