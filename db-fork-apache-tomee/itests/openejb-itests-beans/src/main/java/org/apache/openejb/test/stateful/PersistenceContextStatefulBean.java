/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.test.stateful;

import org.junit.Assert;
import junit.framework.AssertionFailedError;
import org.apache.openejb.test.TestFailureException;

import javax.ejb.SessionContext;
import javax.ejb.Remove;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;
import javax.annotation.Resource;

/**
 * @version $Rev$ $Date$
 */
public class PersistenceContextStatefulBean {

    private EntityManager extendedEntityManager;

    // Used for testing propigation
    private static EntityManager inheritedDelegate;
    private SessionContext ejbContext;

    @Resource
    public void setSessionContext(final SessionContext ctx) {
        ejbContext = ctx;
    }

    @Remove
    public void remove() {
    }

    public String remove(final String arg) {
        return arg;
    }

    public void testPersistenceContext() throws TestFailureException {
        try {
            try {
                final InitialContext ctx = new InitialContext();
                Assert.assertNotNull("The InitialContext is null", ctx);
                final EntityManager em = (EntityManager) ctx.lookup("java:comp/env/persistence/TestContext");
                Assert.assertNotNull("The EntityManager is null", em);

                // call a do nothing method to assure entity manager actually exists
                em.getFlushMode();
            } catch (final Exception e) {
                Assert.fail("Received Exception " + e.getClass() + " : " + e.getMessage());
            }
        } catch (final AssertionFailedError afe) {
            throw new TestFailureException(afe);
        }
    }

    public void testExtendedPersistenceContext() throws TestFailureException {
        try {
            try {
                final InitialContext ctx = new InitialContext();
                Assert.assertNotNull("The InitialContext is null", ctx);
                final EntityManager em = (EntityManager) ctx.lookup("java:comp/env/persistence/ExtendedTestContext");
                Assert.assertNotNull("The EntityManager is null", em);

                // call a do nothing method to assure entity manager actually exists
                em.getFlushMode();

                if (extendedEntityManager != null) {
                    Assert.assertSame("Extended entity manager should be the same instance that was found last time",
                        extendedEntityManager,
                        em);
                    Assert.assertSame("Extended entity manager delegate should be the same instance that was found last time",
                        extendedEntityManager.getDelegate(),
                        em.getDelegate());
                }
                extendedEntityManager = em;

                final UserTransaction userTransaction = ejbContext.getUserTransaction();
                userTransaction.begin();
                try {
                    em.getFlushMode();
                } finally {
                    userTransaction.commit();
                }
            } catch (final Exception e) {
                e.printStackTrace();
                Assert.fail("Received Exception " + e.getClass() + " : " + e.getMessage());
            }
        } catch (final AssertionFailedError afe) {
            throw new TestFailureException(afe);
        }
    }

    public void testPropagatedPersistenceContext() throws TestFailureException {
        try {
            try {
                final InitialContext ctx = new InitialContext();
                Assert.assertNotNull("The InitialContext is null", ctx);
                final EntityManager em = (EntityManager) ctx.lookup("java:comp/env/persistence/ExtendedTestContext");
                Assert.assertNotNull("The EntityManager is null", em);

                // call a do nothing method to assure entity manager actually exists
                em.getFlushMode();

                // get the raw entity manager so we can test it below
                inheritedDelegate = (EntityManager) em.getDelegate();

                // The extended entity manager is not propigated to a non-extended entity manager unless there is a transaction
                final EntityManager nonExtendedEm = (EntityManager) ctx.lookup("java:comp/env/persistence/TestContext");
                nonExtendedEm.getFlushMode();
                final EntityManager nonExtendedDelegate = ((EntityManager) nonExtendedEm.getDelegate());
                Assert.assertTrue("non-extended entity manager should be open", nonExtendedDelegate.isOpen());
                Assert.assertNotSame("Extended non-extended entity manager shound not be the same instance as extendend entity manager when accessed out side of a transactions",
                    inheritedDelegate,
                    nonExtendedDelegate);

                // When the non-extended entity manager is accessed within a transaction is should see the stateful extended context.
                //
                // Note: this code also tests EBJ 3.0 Persistence spec 5.9.1 "UserTransaction is begun within the method, the
                // container associates the persistence context with the JTA transaction and calls EntityManager.joinTransaction."
                // If our the extended entity manager were not associted with the transaction, the non-extended entity manager would
                // not see it.
                final UserTransaction userTransaction = ejbContext.getUserTransaction();
                userTransaction.begin();
                try {
                    Assert.assertSame("Extended non-extended entity manager to be same instance as extendend entity manager",
                        inheritedDelegate,
                        nonExtendedEm.getDelegate());
                } finally {
                    userTransaction.commit();
                }

                // When a stateful bean with an extended entity manager creates another stateful bean, the new bean will
                // inherit the extended entity manager (assuming it contains an extended entity manager for the same persistence
                // unit).
                final PersistenceContextStatefulHome home = (PersistenceContextStatefulHome) ejbContext.getEJBHome();
                final PersistenceContextStatefulObject object = home.create();

                // test the new stateful bean recieved the context
                object.testPropgation();

                // remove the bean
                object.remove();
            } catch (final Exception e) {
                Assert.fail("Received Exception " + e.getClass() + " : " + e.getMessage());
            }
        } catch (final AssertionFailedError afe) {
            throw new TestFailureException(afe);
        }
    }

    public void testPropgation() throws TestFailureException {
        if (inheritedDelegate == null) return;
        try {
            try {
                final InitialContext ctx = new InitialContext();
                Assert.assertNotNull("The InitialContext is null", ctx);
                final EntityManager em = (EntityManager) ctx.lookup("java:comp/env/persistence/ExtendedTestContext");
                Assert.assertNotNull("The EntityManager is null", em);

                // call a do nothing method to assure entity manager actually exists
                em.getFlushMode();

                final EntityManager delegate = (EntityManager) em.getDelegate();
                Assert.assertSame("Extended entity manager delegate should be the same instance that was found last time",
                    inheritedDelegate,
                    delegate);
            } catch (final Exception e) {
                e.printStackTrace();
                Assert.fail("Received Exception " + e.getClass() + " : " + e.getMessage());
            }
        } catch (final AssertionFailedError afe) {
            throw new TestFailureException(afe);
        }
    }

}
