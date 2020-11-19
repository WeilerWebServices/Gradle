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
package org.apache.openejb.testing;

import org.apache.openejb.OpenEJB;
import org.apache.openejb.jee.EjbJar;
import org.apache.openejb.junit.ApplicationComposerRule;
import org.apache.openejb.junit.TransactionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;
import javax.transaction.SystemException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TransactionRuleTest {
    @Rule
    public final TestRule container = RuleChain
            .outerRule(new ApplicationComposerRule(this))
            .around(new TransactionRule());

    @Module
    public EjbJar jar() {
        return new EjbJar();
    }

    @Test
    public void no() throws NamingException, SystemException {
        assertNull(OpenEJB.getTransactionManager().getTransaction());
    }

    @Test
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void yes() throws NamingException, SystemException {
        assertNotNull(OpenEJB.getTransactionManager().getTransaction());
    }
}
