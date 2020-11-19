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
package org.gradle.api.initialization.dsl;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.internal.Actions;
import org.gradle.api.provider.Property;
import org.gradle.internal.HasInternalProtocol;

import java.util.List;

/**
 * A version catalog builder. Dependencies defined via this model
 * will trigger the generation of accessors available in build scripts.
 *
 * @since 6.8
 */
@Incubating
@HasInternalProtocol
public interface VersionCatalogBuilder {

    /**
     * A description for the dependencies model, which will be used in
     * the generated sources as documentation.
     * @return the description for this model
     */
    Property<String> getDescription();

    /**
     * Configures the model by reading it from a version catalog.
     * A version catalog is a component published using the `version-catalog` plugin or
     * a local TOML file.
     *
     * All imports configured by this method will be accumulated in order and executed
     * before any other modification provided by this builder, such that "local" modifications
     * have higher priority than any imported component.
     *
     * @param dependencyNotation any notation supported by {@link org.gradle.api.artifacts.dsl.DependencyHandler}
     */
    default void from(Object dependencyNotation) {
        from(dependencyNotation, Actions.doNothing());
    }

    /**
     * Configures the model by reading it from a version catalog.
     * A version catalog is a component published using the `version-catalog` plugin or
     * a local TOML file.
     *
     * All imports configured by this method will be accumulated in order and executed
     * before any other modification provided by this builder, such that "local" modifications
     * have higher priority than any imported component.
     *
     * @param dependencyNotation any notation supported by {@link org.gradle.api.artifacts.dsl.DependencyHandler}
     * @param importSpec Configures how the version catalog will be imported
     */
    void from(Object dependencyNotation, Action<? super ImportSpec> importSpec);

    /**
     * Configures a dependency version which can then be referenced using
     * the {@link VersionCatalogBuilder.LibraryAliasBuilder#versionRef(String)} )} method.
     *
     * @param name an identifier for the version
     * @param versionSpec the dependency version spec
     * @return the version alias name
     */
    String version(String name, Action<? super MutableVersionConstraint> versionSpec);

    /**
     * Configures a dependency version which can then be referenced using
     * the {@link VersionCatalogBuilder.LibraryAliasBuilder#versionRef(String)} method.
     *
     * @param name an identifier for the version
     * @param version the version string
     */
    String version(String name, String version);

    /**
     * Entry point for registering an alias for a library
     * @param alias the alias identifer
     * @return a builder for this alias
     */
    AliasBuilder alias(String alias);

    /**
     * Declares a bundle of dependencies. A bundle consists of a name for the bundle,
     * and a list of aliases. The aliases must correspond to aliases defined via
     * the {@link #alias(String)} method.
     *
     * @param name the name of the bundle
     * @param aliases the aliases of the dependencies included in the bundle
     */
    void bundle(String name, List<String> aliases);

    /**
     * Returns the name of the extension configured by this builder
     */
    String getLibrariesExtensionName();

    /**
     * Allows configuring an alias
     *
     * @since 6.8
     */
    @Incubating
    interface AliasBuilder {
        /**
         * Sets GAV coordinates for this alias
         * @param groupArtifactVersion the GAV coordinates, in the group:artifact:version form
         */
        void to(String groupArtifactVersion);

        /**
         * Sets the group and name of this alias
         * @param group the group
         * @param name the name (or artifact id)
         * @return a builder to configure the version
         */
        LibraryAliasBuilder to(String group, String name);
    }

    /**
     * Allows configuring the version of a library
     *
     * @since 6.8
     */
    @Incubating
    interface LibraryAliasBuilder {
        /**
         * Configures the version for this alias
         */
        void version(Action<? super MutableVersionConstraint> versionSpec);

        /**
         * Configures the required version for this alias
         */
        void version(String version);

        /**
         * Configures this alias to use a version reference, created
         * via the {@link #version(String, Action)} method.
         *
         * @param versionRef the version reference
         */
        void versionRef(String versionRef);

        /**
         * Do not associate this alias to a particular version, in which
         * case the dependency notation will just have group and artifact.
         *
         */
        void withoutVersion();
    }

    /**
     * Configures how another model is imported into the current model.
     *
     * @since 6.8
     */
    @Incubating
    interface ImportSpec {
        /**
         * Only aliases from the supplied list will be imported.
         * Multiple calls to this method adds to the list of allowed aliases.
         * @param aliases aliases
         */
        void includeDependency(String... aliases);

        /**
         * Aliases which are in the supplied list will not be imported.
         * Multiple calls to this method adds to the list of disallowed aliases.
         * @param aliases aliases to exclude
         */
        void excludeDependency(String... aliases);

        /**
         * Only bundles from the supplied list will be imported.
         * Multiple calls to this method adds to the list of allowed bundles.
         * @param bundles names of the bundles to include
         */
        void includeBundle(String... bundles);

        /**
         * Bundles which are in the supplied list will not be imported.
         * Multiple calls to this method adds to the list of disallowed bundles.
         * @param bundles bundles to exclude
         */
        void excludeBundle(String... bundles);

        /**
         * Only versions from the supplied list will be imported.
         * Multiple calls to this method adds to the list of allowed aliases.
         * @param aliases aliases
         */
        void includeVersion(String... aliases);

        /**
         * Versions which are in the supplied list will not be imported.
         * Multiple calls to this method adds to the list of disallowed aliases.
         * @param aliases aliases to exclude
         */
        void excludeVersion(String... aliases);

        /**
         * Only versions from the supplied list will be imported.
         * Multiple calls to this method adds to the list of allowed aliases.
         * @param ids plugin ids to include when importing
         */
        void includePlugin(String... ids);

        /**
         * Versions which are in the supplied list will not be imported.
         * Multiple calls to this method adds to the list of disallowed aliases.
         * @param ids plugin ids to exclude when importing
         */
        void excludePlugin(String... ids);
    }
}
