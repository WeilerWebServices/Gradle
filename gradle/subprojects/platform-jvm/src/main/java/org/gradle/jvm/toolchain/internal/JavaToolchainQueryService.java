/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.jvm.toolchain.internal;

import org.gradle.api.GradleException;
import org.gradle.api.Transformer;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.gradle.jvm.toolchain.install.internal.DefaultJavaToolchainProvisioningService;
import org.gradle.jvm.toolchain.install.internal.JavaToolchainProvisioningService;

import javax.inject.Inject;
import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class JavaToolchainQueryService {

    private final SharedJavaInstallationRegistry registry;
    private final JavaToolchainFactory toolchainFactory;
    private final JavaToolchainProvisioningService installService;
    private final Provider<Boolean> detectEnabled;
    private final Provider<Boolean> downloadEnabled;

    @Inject
    public JavaToolchainQueryService(SharedJavaInstallationRegistry registry, JavaToolchainFactory toolchainFactory, JavaToolchainProvisioningService provisioningService, ProviderFactory factory) {
        this.registry = registry;
        this.toolchainFactory = toolchainFactory;
        this.installService = provisioningService;
        detectEnabled = factory.gradleProperty(AutoDetectingInstallationSupplier.AUTO_DETECT).forUseAtConfigurationTime().map(Boolean::parseBoolean);
        downloadEnabled = factory.gradleProperty(DefaultJavaToolchainProvisioningService.AUTO_DOWNLOAD).forUseAtConfigurationTime().map(Boolean::parseBoolean);
    }

    <T> Provider<T> toolFor(JavaToolchainSpec spec, Transformer<T, JavaToolchain> toolFunction) {
        return findMatchingToolchain(spec).map(toolFunction);
    }

    Provider<JavaToolchain> findMatchingToolchain(JavaToolchainSpec filter) {
        return new DefaultProvider<>(() -> {
            if (((DefaultToolchainSpec) filter).isConfigured()) {
                return query(filter);
            } else {
                return null;
            }
        });
    }

    private JavaToolchain query(JavaToolchainSpec filter) {
        return registry.listInstallations().stream()
            .map(InstallationLocation::getLocation)
            .map(this::asToolchain)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(matchingToolchain(filter))
            .sorted(new JavaToolchainComparator())
            .findFirst()
            .orElseGet(() -> downloadToolchain(filter));
    }

    private JavaToolchain downloadToolchain(JavaToolchainSpec spec) {
        final Optional<File> installation = installService.tryInstall(spec);
        final Optional<JavaToolchain> toolchain = installation
            .map(this::asToolchain)
            .orElseThrow(noToolchainAvailable(spec));
        return toolchain.orElseThrow(provisionedToolchainIsInvalid(installation::get));
    }

    private Supplier<GradleException> noToolchainAvailable(JavaToolchainSpec spec) {
        return () -> new NoToolchainAvailableException(spec, detectEnabled.getOrElse(true), downloadEnabled.getOrElse(true));
    }

    private Supplier<GradleException> provisionedToolchainIsInvalid(Supplier<File> javaHome) {
        return () -> new GradleException("Provisioned toolchain '" + javaHome.get() + "' could not be probed.");
    }

    private Predicate<JavaToolchain> matchingToolchain(JavaToolchainSpec spec) {
        final Predicate<JavaToolchain> languagePredicate = toolchain -> toolchain.getLanguageVersion().equals(spec.getLanguageVersion().get());
        final Predicate<? super JavaToolchain> vendorSpec = getVendorPredicate(spec);
        return languagePredicate.and(vendorSpec);
    }

    @SuppressWarnings("unchecked")
    private Predicate<? super JavaToolchain> getVendorPredicate(JavaToolchainSpec spec) {
        return (Predicate<? super JavaToolchain>) spec.getVendor().get();
    }

    private Optional<JavaToolchain> asToolchain(File javaHome) {
        return toolchainFactory.newInstance(javaHome);
    }
}
