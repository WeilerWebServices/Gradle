package io.quarkus.arc.test.unused;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import java.math.BigDecimal;
import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class RemoveUnusedBeansTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(HasObserver.class, Foo.class, FooAlternative.class, HasName.class, UnusedProducers.class,
                    InjectedViaInstance.class, InjectedViaProvider.class, Excluded.class, UsedProducers.class)
            .removeUnusedBeans(true)
            .addRemovalExclusion(b -> b.getBeanClass().toString().equals(Excluded.class.getName()))
            .build();

    @Test
    public void testRemoval() {
        ArcContainer container = Arc.container();
        assertTrue(container.instance(HasObserver.class).isAvailable());
        assertTrue(container.instance(HasName.class).isAvailable());
        assertTrue(container.instance(InjectedViaInstance.class).isAvailable());
        assertTrue(container.instance(InjectedViaProvider.class).isAvailable());
        assertTrue(container.instance(String.class).isAvailable());
        assertTrue(container.instance(UsedProducers.class).isAvailable());
        assertFalse(container.instance(UnusedProducers.class).isAvailable());
        assertFalse(container.instance(BigDecimal.class).isAvailable());
        // Foo is injected in HasObserver#observe()
        Foo foo = container.instance(Foo.class).get();
        assertEquals(FooAlternative.class.getName(), foo.ping());
        assertTrue(foo.provider.get().isValid());
        assertEquals(1, container.beanManager().getBeans(Foo.class).size());
        assertEquals("pong", container.instance(Excluded.class).get().ping());
    }

    @Dependent
    static class HasObserver {

        void observe(@Observes String event, Foo foo) {
        }

    }

    @Named
    @Dependent
    static class HasName {

    }

    @Dependent
    static class Foo {

        @Inject
        Provider<InjectedViaProvider> provider;

        String ping() {
            return getClass().getName();
        }

    }

    @Alternative
    @Priority(1)
    @Dependent
    static class FooAlternative extends Foo {

        @Inject
        Instance<InjectedViaInstance> instance;

        @Inject
        String foo;

    }

    @Singleton
    static class InjectedViaInstance {

    }

    @Singleton
    static class InjectedViaProvider {

        private boolean isValid;

        @PostConstruct
        void init() {
            isValid = true;
        }

        boolean isValid() {
            return isValid;
        }

    }

    @Singleton
    static class UnusedProducers {

        @Produces
        BigDecimal unusedNumber() {
            return BigDecimal.ZERO;
        }

    }

    @Singleton
    static class UsedProducers {

        @Produces
        String usedString() {
            return "ok";
        }

    }

    @Singleton
    static class Excluded {

        String ping() {
            return "pong";
        }

    }

}
