/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.play.internal

import org.gradle.internal.logging.text.DiagnosticsVisitor
import org.gradle.platform.base.binary.BaseBinaryFixtures
import org.gradle.platform.base.internal.BinaryBuildAbility
import org.gradle.play.PlayApplicationBinarySpec
import org.gradle.play.internal.toolchain.PlayToolChainInternal
import org.gradle.play.internal.toolchain.PlayToolProvider
import org.gradle.play.platform.PlayPlatform
import spock.lang.Specification

class DefaultPlayApplicationBinarySpecTest extends Specification {
    def playBinary = BaseBinaryFixtures.create(PlayApplicationBinarySpec.class, DefaultPlayApplicationBinarySpec.class, "test", null)

    def "sets binary build ability for unavailable toolchain" () {
        PlayToolProvider result = Mock(PlayToolProvider) {
            isAvailable() >> false
        }
        def toolChain = Mock(PlayToolChainInternal)
        def platform = Mock(PlayPlatform)
        toolChain.select(platform) >> result

        when:
        playBinary.setToolChain(toolChain)
        playBinary.setTargetPlatform(platform)
        BinaryBuildAbility buildAbility = playBinary.getBuildAbility()

        then:
        ! buildAbility.buildable

        when:
        buildAbility.explain(Stub(DiagnosticsVisitor))

        then:
        1 * result.explain(_)
    }
}
