//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.util.component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.util.TypeUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class ContainerLifeCycleTest
{
    @Test
    public void testStartStop() throws Exception
    {
        ContainerLifeCycle a0 = new ContainerLifeCycle();
        TestContainerLifeCycle a1 = new TestContainerLifeCycle();
        a0.addBean(a1);

        a0.start();
        assertEquals(1, a1.started.get());
        assertEquals(0, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.start();
        assertEquals(1, a1.started.get());
        assertEquals(0, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.stop();
        assertEquals(1, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.start();
        assertEquals(2, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.stop();
        assertEquals(2, a1.started.get());
        assertEquals(2, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());
    }

    @Test
    public void testStartStopDestroy() throws Exception
    {
        ContainerLifeCycle a0 = new ContainerLifeCycle();
        TestContainerLifeCycle a1 = new TestContainerLifeCycle();

        a0.start();
        assertEquals(0, a1.started.get());
        assertEquals(0, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.addBean(a1);
        assertEquals(0, a1.started.get());
        assertEquals(0, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());
        assertFalse(a0.isManaged(a1));

        a0.start();
        assertEquals(0, a1.started.get());
        assertEquals(0, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a1.start();
        a0.manage(a1);
        assertEquals(1, a1.started.get());
        assertEquals(0, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.removeBean(a1);
        assertEquals(1, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.stop();
        a0.destroy();
        assertEquals(1, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a1.stop();
        assertEquals(1, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a1.destroy();
        assertEquals(1, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(1, a1.destroyed.get());
    }

    @Test
    public void testIllegalToStartAfterDestroy() throws Exception
    {
        ContainerLifeCycle container = new ContainerLifeCycle();
        container.start();
        container.stop();
        container.destroy();

        assertThrows(IllegalStateException.class, ()-> {
            container.start();
        });
    }

    @Test
    public void testDisJoint() throws Exception
    {
        ContainerLifeCycle a0 = new ContainerLifeCycle();
        TestContainerLifeCycle a1 = new TestContainerLifeCycle();

        // Start the a1 bean before adding, makes it auto disjoint
        a1.start();

        // Now add it
        a0.addBean(a1);
        assertFalse(a0.isManaged(a1));

        a0.start();
        assertEquals(1, a1.started.get());
        assertEquals(0, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.start();
        assertEquals(1, a1.started.get());
        assertEquals(0, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.stop();
        assertEquals(1, a1.started.get());
        assertEquals(0, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a1.stop();
        assertEquals(1, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.start();
        assertEquals(1, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.manage(a1);
        assertTrue(a0.isManaged(a1));

        a0.stop();
        assertEquals(1, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.start();
        assertEquals(2, a1.started.get());
        assertEquals(1, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.stop();
        assertEquals(2, a1.started.get());
        assertEquals(2, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a0.unmanage(a1);
        assertFalse(a0.isManaged(a1));

        a0.destroy();
        assertEquals(2, a1.started.get());
        assertEquals(2, a1.stopped.get());
        assertEquals(0, a1.destroyed.get());

        a1.destroy();
        assertEquals(2, a1.started.get());
        assertEquals(2, a1.stopped.get());
        assertEquals(1, a1.destroyed.get());
    }

    @Test
    public void testDumpable() throws Exception
    {
        ContainerLifeCycle a0 = new ContainerLifeCycle();
        String dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");

        ContainerLifeCycle aa0 = new ContainerLifeCycle();
        a0.addBean(aa0);
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+? ContainerLife");

        ContainerLifeCycle aa1 = new ContainerLifeCycle();
        a0.addBean(aa1);
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+? ContainerLife");
        dump = check(dump, "+? ContainerLife");
        dump = check(dump, "");

        ContainerLifeCycle aa2 = new ContainerLifeCycle();
        a0.addBean(aa2, false);
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+? ContainerLife");
        dump = check(dump, "+? ContainerLife");
        dump = check(dump, "+~ ContainerLife");
        dump = check(dump, "");

        aa1.start();
        a0.start();
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "+~ ContainerLife");
        dump = check(dump, "+~ ContainerLife");
        dump = check(dump, "");

        a0.manage(aa1);
        a0.removeBean(aa2);
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "");

        ContainerLifeCycle aaa0 = new ContainerLifeCycle();
        aa0.addBean(aaa0);
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  +~ Container");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "");

        ContainerLifeCycle aa10 = new ContainerLifeCycle();
        aa1.addBean(aa10, true);
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  +~ Container");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "   += Container");
        dump = check(dump, "");

        final ContainerLifeCycle a1 = new ContainerLifeCycle();
        final ContainerLifeCycle a2 = new ContainerLifeCycle();
        final ContainerLifeCycle a3 = new ContainerLifeCycle();
        final ContainerLifeCycle a4 = new ContainerLifeCycle();

        ContainerLifeCycle aa = new ContainerLifeCycle()
        {
            @Override
            public void dump(Appendable out, String indent) throws IOException
            {
                Dumpable.dumpObjects(out, indent, this.toString(), TypeUtil.asList(new Object[]{a1, a2}), TypeUtil.asList(new Object[]{a3, a4}));
            }
        };
        a0.addBean(aa, true);


        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  +~ Container");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  += Container");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "   +> java.util.Arrays$ArrayList");
        dump = check(dump, "   |  +: ContainerLifeCycle");
        dump = check(dump, "   |  +: ContainerLifeCycle");
        dump = check(dump, "   +> java.util.Arrays$ArrayList");
        dump = check(dump, "      +: ContainerLifeCycle");
        dump = check(dump, "      +: ContainerLifeCycle");
        dump = check(dump, "");

        a2.addBean(aa0, true);
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  +~ Container");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  += Container");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "   +> java.util.Arrays$ArrayList");
        dump = check(dump, "   |  +: ContainerLifeCycle");
        dump = check(dump, "   |  +: ContainerLifeCycle");
        dump = check(dump, "   |     += Conta");
        dump = check(dump, "   |        +~ C");
        dump = check(dump, "   +> java.util.Arrays$ArrayList");
        dump = check(dump, "      +: ContainerLifeCycle");
        dump = check(dump, "      +: ContainerLifeCycle");
        dump = check(dump, "");

        a2.unmanage(aa0);
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  +~ Container");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  += Container");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "   +> java.util.Arrays$ArrayList");
        dump = check(dump, "   |  +: ContainerLifeCycle");
        dump = check(dump, "   |  +: ContainerLifeCycle");
        dump = check(dump, "   |     +~ Conta");
        dump = check(dump, "   +> java.util.Arrays$ArrayList");
        dump = check(dump, "      +: ContainerLifeCycle");
        dump = check(dump, "      +: ContainerLifeCycle");
        dump = check(dump, "");

        a0.unmanage(aa);
        dump = trim(a0.dump());
        dump = check(dump, "ContainerLifeCycl");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  +~ Container");
        dump = check(dump, "+= ContainerLife");
        dump = check(dump, "|  += Container");
        dump = check(dump, "+~ ContainerLife");
        dump = check(dump, "");
    }

    @Test
    public void listenerTest() throws Exception
    {
        final Queue<String> handled = new ConcurrentLinkedQueue<>();
        final Queue<String> operation = new ConcurrentLinkedQueue<>();
        final Queue<Container> parent = new ConcurrentLinkedQueue<>();
        final Queue<Object> child = new ConcurrentLinkedQueue<>();

        Container.Listener listener = new Container.Listener()
        {
            @Override
            public void beanRemoved(Container p, Object c)
            {
                handled.add(toString());
                operation.add("removed");
                parent.add(p);
                child.add(c);
            }

            @Override
            public void beanAdded(Container p, Object c)
            {
                handled.add(toString());
                operation.add("added");
                parent.add(p);
                child.add(c);
            }

            public
            @Override
            String toString()
            {
                return "listener";
            }
        };

        ContainerLifeCycle c0 = new ContainerLifeCycle()
        {
            public
            @Override
            String toString()
            {
                return "c0";
            }
        };
        ContainerLifeCycle c00 = new ContainerLifeCycle()
        {
            public
            @Override
            String toString()
            {
                return "c00";
            }
        };
        c0.addBean(c00);
        String b000 = "b000";
        c00.addBean(b000);

        c0.addBean(listener);

        assertEquals("listener", handled.poll());
        assertEquals("added", operation.poll());
        assertEquals(c0, parent.poll());
        assertEquals(c00, child.poll());

        assertEquals("listener", handled.poll());
        assertEquals("added", operation.poll());
        assertEquals(c0, parent.poll());
        assertEquals(listener, child.poll());

        Container.InheritedListener inherited = new Container.InheritedListener()
        {
            @Override
            public void beanRemoved(Container p, Object c)
            {
                handled.add(toString());
                operation.add("removed");
                parent.add(p);
                child.add(c);
            }

            @Override
            public void beanAdded(Container p, Object c)
            {
                handled.add(toString());
                operation.add("added");
                parent.add(p);
                child.add(c);
            }

            public
            @Override
            String toString()
            {
                return "inherited";
            }
        };

        c0.addBean(inherited);

        assertEquals("inherited", handled.poll());
        assertEquals("added", operation.poll());
        assertEquals(c0, parent.poll());
        assertEquals(c00, child.poll());

        assertEquals("inherited", handled.poll());
        assertEquals("added", operation.poll());
        assertEquals(c0, parent.poll());
        assertEquals(listener, child.poll());

        assertEquals("listener", handled.poll());
        assertEquals("added", operation.poll());
        assertEquals(c0, parent.poll());
        assertEquals(inherited, child.poll());

        assertEquals("inherited", handled.poll());
        assertEquals("added", operation.poll());
        assertEquals(c0, parent.poll());
        assertEquals(inherited, child.poll());

        c0.start();

        assertEquals("inherited", handled.poll());
        assertEquals("added", operation.poll());
        assertEquals(c00, parent.poll());
        assertEquals(b000, child.poll());

        assertEquals("inherited", handled.poll());
        assertEquals("added", operation.poll());
        assertEquals(c00, parent.poll());
        assertEquals(inherited, child.poll());

        c0.removeBean(c00);

        assertEquals("inherited", handled.poll());
        assertEquals("removed", operation.poll());
        assertEquals(c00, parent.poll());
        assertEquals(inherited, child.poll());

        assertEquals("inherited", handled.poll());
        assertEquals("removed", operation.poll());
        assertEquals(c00, parent.poll());
        assertEquals(b000, child.poll());

        assertEquals("listener", handled.poll());
        assertEquals("removed", operation.poll());
        assertEquals(c0, parent.poll());
        assertEquals(c00, child.poll());

        assertEquals("inherited", handled.poll());
        assertEquals("removed", operation.poll());
        assertEquals(c0, parent.poll());
        assertEquals(c00, child.poll());
    }

    private final class InheritedListenerLifeCycle extends AbstractLifeCycle implements Container.InheritedListener
    {
        @Override
        public void beanRemoved(Container p, Object c)
        {
        }

        @Override
        public void beanAdded(Container p, Object c)
        {
        }

        @Override
        public String toString()
        {
            return "inherited";
        }
    }

    @Test
    public void testInheritedListener() throws Exception
    {
        ContainerLifeCycle c0 = new ContainerLifeCycle()
        {
            public
            @Override
            String toString()
            {
                return "c0";
            }
        };
        ContainerLifeCycle c00 = new ContainerLifeCycle()
        {
            public
            @Override
            String toString()
            {
                return "c00";
            }
        };
        ContainerLifeCycle c01 = new ContainerLifeCycle()
        {
            public
            @Override
            String toString()
            {
                return "c01";
            }
        };
        Container.InheritedListener inherited = new InheritedListenerLifeCycle();

        c0.addBean(c00);
        c0.start();
        c0.addBean(inherited);
        c0.manage(inherited);
        c0.addBean(c01);
        c01.start();
        c0.manage(c01);

        assertTrue(c0.isManaged(inherited));
        assertFalse(c00.isManaged(inherited));
        assertFalse(c01.isManaged(inherited));
    }

    String trim(String s) throws IOException
    {
        StringBuilder b = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(s));

        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            if (line.length() > 50)
                line = line.substring(0, 50);
            b.append(line).append('\n');
        }

        return b.toString();
    }

    String check(String s, String x)
    {
        String r = s;
        int nl = s.indexOf('\n');
        if (nl > 0)
        {
            r = s.substring(nl + 1);
            s = s.substring(0, nl);
        }


        assertThat( s, Matchers.startsWith( x));

        return r;
    }

    private static class TestContainerLifeCycle extends ContainerLifeCycle
    {
        private final AtomicInteger destroyed = new AtomicInteger();
        private final AtomicInteger started = new AtomicInteger();
        private final AtomicInteger stopped = new AtomicInteger();

        @Override
        protected void doStart() throws Exception
        {
            started.incrementAndGet();
            super.doStart();
        }

        @Override
        protected void doStop() throws Exception
        {
            stopped.incrementAndGet();
            super.doStop();
        }

        @Override
        public void destroy()
        {
            destroyed.incrementAndGet();
            super.destroy();
        }
    }


    @Test
    public void testGetBeans() throws Exception
    {
        TestContainerLifeCycle root = new TestContainerLifeCycle();
        TestContainerLifeCycle left = new TestContainerLifeCycle();
        root.addBean(left);
        TestContainerLifeCycle right = new TestContainerLifeCycle();
        root.addBean(right);
        TestContainerLifeCycle leaf = new TestContainerLifeCycle();
        right.addBean(leaf);

        root.addBean(Integer.valueOf(0));
        root.addBean(Integer.valueOf(1));
        left.addBean(Integer.valueOf(2));
        right.addBean(Integer.valueOf(3));
        leaf.addBean(Integer.valueOf(4));
        leaf.addBean("leaf");

        assertThat(root.getBeans(Container.class), containsInAnyOrder(left,right));
        assertThat(root.getBeans(Integer.class), containsInAnyOrder(Integer.valueOf(0),Integer.valueOf(1)));
        assertThat(root.getBeans(String.class), containsInAnyOrder());

        assertThat(root.getContainedBeans(Container.class), containsInAnyOrder(left,right,leaf));
        assertThat(root.getContainedBeans(Integer.class), containsInAnyOrder(Integer.valueOf(0),Integer.valueOf(1),Integer.valueOf(2),Integer.valueOf(3),Integer.valueOf(4)));
        assertThat(root.getContainedBeans(String.class), containsInAnyOrder("leaf"));
    }
}
