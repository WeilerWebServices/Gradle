/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.play.internal.toolchain

import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.internal.ClassPathRegistry
import org.gradle.api.internal.file.FileCollectionFactory
import org.gradle.initialization.ClassLoaderRegistry
import org.gradle.internal.fingerprint.classpath.ClasspathFingerprinter
import org.gradle.language.base.internal.compile.CompileSpec
import org.gradle.play.internal.DefaultPlayPlatform
import org.gradle.play.internal.platform.PlayMajorVersion
import org.gradle.play.internal.run.PlayApplicationRunner
import org.gradle.play.internal.run.PlayRunAdapterV23X
import org.gradle.play.internal.run.PlayRunAdapterV24X
import org.gradle.play.internal.run.PlayRunAdapterV25X
import org.gradle.play.internal.run.PlayRunAdapterV26X
import org.gradle.play.platform.PlayPlatform
import org.gradle.process.internal.JavaForkOptionsFactory
import org.gradle.process.internal.worker.WorkerProcessFactory
import org.gradle.workers.internal.ActionExecutionSpecFactory
import org.gradle.workers.internal.WorkerDaemonFactory
import spock.lang.Specification
import spock.lang.Unroll

class DefaultPlayToolProviderTest extends Specification {
    def forkOptionsFactory = Mock(JavaForkOptionsFactory)
    WorkerDaemonFactory workerDaemonFactory = Mock()
    ConfigurationContainer configurationContainer = Mock()
    DependencyHandler dependencyHandler = Mock()
    PlayPlatform playPlatform = Mock()
    WorkerProcessFactory workerProcessBuilderFactory = Mock()
    File daemonWorkingDir = Mock()
    ClasspathFingerprinter fingerprinter = Mock()
    ClassPathRegistry classPathRegistry = Mock()
    ClassLoaderRegistry classLoaderRegistry = Mock()
    ActionExecutionSpecFactory actionExecutionSpecFactory = Mock()
    FileCollectionFactory fileCollectionFactory = Mock()
    Set<File> twirlClasspath = Stub(Set)
    Set<File> routesClasspath = Stub(Set)
    Set<File> javascriptClasspath = Stub(Set)

    DefaultPlayToolProvider playToolProvider

    private DefaultPlayToolProvider createProvider() {
        return new DefaultPlayToolProvider(forkOptionsFactory, daemonWorkingDir, workerDaemonFactory, workerProcessBuilderFactory, playPlatform, twirlClasspath, routesClasspath, javascriptClasspath, fingerprinter, classPathRegistry, classLoaderRegistry, actionExecutionSpecFactory, fileCollectionFactory)
    }

    @Unroll
    def "provides playRunner for play #playVersion"() {
        setup:
        _ * playPlatform.getPlayVersion() >> playVersion

        when:
        playToolProvider = createProvider()
        def runner = playToolProvider.get(PlayApplicationRunner.class)

        then:
        runner != null
        runner.adapter.class == adapter

        where:
        playVersion | adapter
        "2.3.x"     | PlayRunAdapterV23X
        "2.4.x"     | PlayRunAdapterV24X
        "2.5.x"     | PlayRunAdapterV25X
        "2.6.x"     | PlayRunAdapterV26X
    }

    def "cannot create tool provider for unsupported play versions"() {
        when:
        _ * playPlatform.getPlayVersion() >> playVersion
        playToolProvider = createProvider()

        then: "fails with meaningful error message"
        def exception = thrown(InvalidUserDataException)
        exception.message == "Not a supported Play version: ${playVersion}. This plugin is compatible with: [${PlayMajorVersion.values().join(', ')}]."

        and: "no dependencies resolved"
        0 * dependencyHandler.create(_)
        0 * configurationContainer.detachedConfiguration(_)

        where:
        playVersion << ["2.1.x", "3.0.0"]
    }

    def "newCompiler provides decent error for unsupported CompileSpec"() {
        setup:
        _ * playPlatform.getPlayVersion() >> DefaultPlayPlatform.DEFAULT_PLAY_VERSION
        playToolProvider = createProvider()

        when:
        playToolProvider.newCompiler(UnknownCompileSpec.class)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Cannot create Compiler for unsupported CompileSpec type 'UnknownCompileSpec'"
    }

    class UnknownCompileSpec implements CompileSpec {}
}


