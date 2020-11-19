/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gradleware.tooling.toolingmodel;

import java.util.Map;
import com.gradleware.tooling.toolingutils.ImmutableCollection;

/**
 * Describes a build command in an Eclipse project.
 *
 * @author Donát Csikós
 */
public interface OmniEclipseBuildCommand {

    /**
     * Returns the name of the build command.
     *
     * @return the name of the build command
     */
    String getName();

    /**
     * Returns the arguments supplied for the build command.
     *
     * @return the build command arguments
     */
    @ImmutableCollection
    Map<String, String> getArguments();

}
