package org.spekframework.spek2.junit

import org.junit.platform.engine.DiscoverySelector
import org.spekframework.spek2.runtime.scope.Path

class SpekPathDiscoverySelector(val path: Path) : DiscoverySelector
