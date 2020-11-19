/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomee.loader.log;

import org.apache.juli.logging.Log;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Log4jLog implements Log {
    private final Logger log;

    public Log4jLog(final String logger) {
        this.log = Logger.getLogger(logger);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isEnabledFor(Level.ERROR);
    }

    @Override
    public boolean isFatalEnabled() {
        return log.isEnabledFor(Level.FATAL);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isEnabledFor(Level.TRACE);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isEnabledFor(Level.WARN);
    }

    @Override
    public void trace(final Object message) {
        log.trace(message);
    }

    @Override
    public void trace(final Object message, final Throwable t) {
        log.trace(message, t);
    }

    @Override
    public void debug(final Object message) {
        log.debug(message);
    }

    @Override
    public void debug(final Object message, final Throwable t) {
        log.debug(message, t);
    }

    @Override
    public void info(final Object message) {
        log.info(message);
    }

    @Override
    public void info(final Object message, final Throwable t) {
        log.info(message, t);
    }

    @Override
    public void warn(final Object message) {
        log.warn(message);
    }

    @Override
    public void warn(final Object message, final Throwable t) {
        log.warn(message, t);
    }

    @Override
    public void error(final Object message) {
        log.error(message);
    }

    @Override
    public void error(final Object message, final Throwable t) {
        log.error(message, t);
    }

    @Override
    public void fatal(final Object message) {
        log.fatal(message);
    }

    @Override
    public void fatal(final Object message, final Throwable t) {
        log.fatal(message, t);
    }
}
