/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cdi.extended;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence21.PersistenceDescriptor;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Arquillian.class)
@Ignore( "WildFly has not released a version supporting JPA 2.2 and CDI 2.0" )
public class ConversationalPersistenceContextExtendedTest {

	@Deployment
	public static Archive<?> buildDeployment() {
		return ShrinkWrap.create( JavaArchive.class, "test.jar" )
				.addClass( Event.class )
				.addClass( ConversationalEventManager.class )
				.addAsManifestResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addAsManifestResource( new StringAsset( persistenceXml().exportAsString() ), "persistence.xml" );
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
				.createPersistenceUnit().name( "pu-beans-basic" )
				.clazz( Event.class.getName() )
				.excludeUnlistedClasses( true )
				.nonJtaDataSource( "java:jboss/datasources/ExampleDS" )
				.getOrCreateProperties().createProperty().name( "jboss.as.jpa.providerModule" ).value( "org.hibernate:5.3" ).up().up()
				.getOrCreateProperties().createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up().up().up();
	}

	@EJB
	private ConversationalEventManager eventManager;

	@PersistenceContext
	private EntityManager em;

	@Test
	@SuppressWarnings("unchecked")
	public void testIt() throws Exception {
		Event event = eventManager.saveEvent( "Untold" );
		assertEquals(0, ((Number) em.createNativeQuery( "select count(*) from Event" ).getSingleResult()).intValue());
		eventManager.endConversation();
		assertEquals(1, ((Number) em.createNativeQuery( "select count(*) from Event" ).getSingleResult()).intValue());
	}

}
