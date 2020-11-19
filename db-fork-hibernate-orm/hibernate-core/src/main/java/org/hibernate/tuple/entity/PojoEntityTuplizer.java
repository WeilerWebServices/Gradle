/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Subclass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.Instantiator;
import org.hibernate.type.CompositeType;

/**
 * An {@link EntityTuplizer} specific to the pojo entity mode.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PojoEntityTuplizer extends AbstractEntityTuplizer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PojoEntityTuplizer.class );

	private final Class mappedClass;
	private final Class proxyInterface;
	private final boolean lifecycleImplementor;
	private final ReflectionOptimizer optimizer;

	private final boolean isBytecodeEnhanced;


	public PojoEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
		this.mappedClass = mappedEntity.getMappedClass();
		this.proxyInterface = mappedEntity.getProxyInterface();
		this.lifecycleImplementor = Lifecycle.class.isAssignableFrom( mappedClass );
		this.isBytecodeEnhanced = entityMetamodel.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();

		String[] getterNames = new String[propertySpan];
		String[] setterNames = new String[propertySpan];
		Class[] propTypes = new Class[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			getterNames[i] = getters[i].getMethodName();
			setterNames[i] = setters[i].getMethodName();
			propTypes[i] = getters[i].getReturnType();
		}

		if ( hasCustomAccessors || !Environment.useReflectionOptimizer() ) {
			optimizer = null;
		}
		else {
			// todo : YUCK!!!
			optimizer = Environment.getBytecodeProvider().getReflectionOptimizer(
					mappedClass,
					getterNames,
					setterNames,
					propTypes
			);
//			optimizer = getFactory().getSettings().getBytecodeProvider().getReflectionOptimizer(
//					mappedClass, getterNames, setterNames, propTypes
//			);
		}
	}

	@Override
	protected ProxyFactory buildProxyFactory(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
		// determine the id getter and setter methods from the proxy interface (if any)
		// determine all interfaces needed by the resulting proxy
		
		/*
		 * We need to preserve the order of the interfaces they were put into the set, since javassist will choose the
		 * first one's class-loader to construct the proxy class with. This is also the reason why HibernateProxy.class
		 * should be the last one in the order (on JBossAS7 its class-loader will be org.hibernate module's class-
		 * loader, which will not see the classes inside deployed apps.  See HHH-3078
		 */
		Set<Class> proxyInterfaces = new java.util.LinkedHashSet<Class>();

		Class mappedClass = persistentClass.getMappedClass();
		Class proxyInterface = persistentClass.getProxyInterface();

		if ( proxyInterface != null && !mappedClass.equals( proxyInterface ) ) {
			if ( !proxyInterface.isInterface() ) {
				throw new MappingException(
						"proxy must be either an interface, or the class itself: " + getEntityName()
				);
			}
			proxyInterfaces.add( proxyInterface );
		}

		if ( mappedClass.isInterface() ) {
			proxyInterfaces.add( mappedClass );
		}

		Iterator<Subclass> subclasses = persistentClass.getSubclassIterator();
		while ( subclasses.hasNext() ) {
			final Subclass subclass = subclasses.next();
			final Class subclassProxy = subclass.getProxyInterface();
			final Class subclassClass = subclass.getMappedClass();
			if ( subclassProxy != null && !subclassClass.equals( subclassProxy ) ) {
				if ( !subclassProxy.isInterface() ) {
					throw new MappingException(
							"proxy must be either an interface, or the class itself: " + subclass.getEntityName()
					);
				}
				proxyInterfaces.add( subclassProxy );
			}
		}

		proxyInterfaces.add( HibernateProxy.class );

		Iterator properties = persistentClass.getPropertyIterator();
		Class clazz = persistentClass.getMappedClass();
		while ( properties.hasNext() ) {
			Property property = (Property) properties.next();
			Method method = property.getGetter( clazz ).getMethod();
			if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
				LOG.gettersOfLazyClassesCannotBeFinal( persistentClass.getEntityName(), property.getName() );
			}
			method = property.getSetter( clazz ).getMethod();
			if ( method != null && Modifier.isFinal( method.getModifiers() ) ) {
				LOG.settersOfLazyClassesCannotBeFinal( persistentClass.getEntityName(), property.getName() );
			}
		}

		Method idGetterMethod = idGetter == null ? null : idGetter.getMethod();
		Method idSetterMethod = idSetter == null ? null : idSetter.getMethod();

		Method proxyGetIdentifierMethod = idGetterMethod == null || proxyInterface == null ?
				null :
				ReflectHelper.getMethod( proxyInterface, idGetterMethod );
		Method proxySetIdentifierMethod = idSetterMethod == null || proxyInterface == null ?
				null :
				ReflectHelper.getMethod( proxyInterface, idSetterMethod );

		ProxyFactory pf = buildProxyFactoryInternal( persistentClass, idGetter, idSetter );
		try {
			pf.postInstantiate(
					getEntityName(),
					mappedClass,
					proxyInterfaces,
					proxyGetIdentifierMethod,
					proxySetIdentifierMethod,
					persistentClass.hasEmbeddedIdentifier() ?
							(CompositeType) persistentClass.getIdentifier().getType() :
							null
			);
		}
		catch (HibernateException he) {
			LOG.unableToCreateProxyFactory( getEntityName(), he );
			pf = null;
		}
		return pf;
	}

	protected ProxyFactory buildProxyFactoryInternal(
			PersistentClass persistentClass,
			Getter idGetter,
			Setter idSetter) {
		// TODO : YUCK!!!  fix after HHH-1907 is complete
		return Environment.getBytecodeProvider().getProxyFactoryFactory().buildProxyFactory( getFactory() );
//		return getFactory().getSettings().getBytecodeProvider().getProxyFactoryFactory().buildProxyFactory();
	}

	@Override
	protected Instantiator buildInstantiator(EntityMetamodel entityMetamodel, PersistentClass persistentClass) {
		if ( optimizer == null ) {
			return new PojoEntityInstantiator( entityMetamodel, persistentClass, null );
		}
		else {
			return new PojoEntityInstantiator( entityMetamodel, persistentClass, optimizer.getInstantiationOptimizer() );
		}
	}

	@Override
	public void setPropertyValues(Object entity, Object[] values) throws HibernateException {
		if ( !getEntityMetamodel().hasLazyProperties() && optimizer != null && optimizer.getAccessOptimizer() != null ) {
			setPropertyValuesWithOptimizer( entity, values );
		}
		else {
			super.setPropertyValues( entity, values );
		}
	}

	@Override
	public Object[] getPropertyValues(Object entity) throws HibernateException {
		if ( shouldGetAllProperties( entity ) && optimizer != null && optimizer.getAccessOptimizer() != null ) {
			return getPropertyValuesWithOptimizer( entity );
		}
		else {
			return super.getPropertyValues( entity );
		}
	}

	@Override
	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SharedSessionContractImplementor session) {
		if ( shouldGetAllProperties( entity ) && optimizer != null && optimizer.getAccessOptimizer() != null ) {
			return getPropertyValuesWithOptimizer( entity );
		}
		else {
			return super.getPropertyValuesToInsert( entity, mergeMap, session );
		}
	}

	protected void setPropertyValuesWithOptimizer(Object object, Object[] values) {
		optimizer.getAccessOptimizer().setPropertyValues( object, values );
	}

	protected Object[] getPropertyValuesWithOptimizer(Object object) {
		return optimizer.getAccessOptimizer().getPropertyValues( object );
	}

	@Override
	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	@Override
	public Class getMappedClass() {
		return mappedClass;
	}

	@Override
	public boolean isLifecycleImplementor() {
		return lifecycleImplementor;
	}

	@Override
	protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
		return mappedProperty.getGetter( mappedEntity.getMappedClass() );
	}

	@Override
	protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return mappedProperty.getSetter( mappedEntity.getMappedClass() );
	}

	@Override
	public Class getConcreteProxyClass() {
		return proxyInterface;
	}

	//TODO: need to make the majority of this functionality into a top-level support class for custom impl support

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {

		// moving to multiple fetch groups, the idea of `lazyPropertiesAreUnfetched` really
		// needs to become either:
		// 		1) the names of all un-fetched fetch groups
		//		2) the names of all fetched fetch groups
		// probably (2) is best
		//
		// ultimately this comes from EntityEntry, although usage-search seems to show it is never updated there.
		//
		// also org.hibernate.persister.entity.AbstractEntityPersister.initializeLazyPropertiesFromDatastore()
		//		needs to be re-worked

		if ( entity instanceof PersistentAttributeInterceptable ) {
			final LazyAttributeLoadingInterceptor interceptor = getEntityMetamodel().getBytecodeEnhancementMetadata().extractInterceptor( entity );
			if ( interceptor == null ) {
				getEntityMetamodel().getBytecodeEnhancementMetadata().injectInterceptor( entity, session );
			}
			else {
				if ( interceptor.getLinkedSession() == null ) {
					interceptor.setSession( session );
				}
			}
		}

		// clear the fields that are marked as dirty in the dirtyness tracker
		if ( entity instanceof SelfDirtinessTracker ) {
			( (SelfDirtinessTracker) entity ).$$_hibernate_clearDirtyAttributes();
		}
	}

	@Override
	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory) {
		if ( entityInstance == null ) {
			return getEntityName();
		}

		final Class concreteEntityClass = entityInstance.getClass();
		if ( concreteEntityClass == getMappedClass() ) {
			return getEntityName();
		}
		else {
			String entityName = getEntityMetamodel().findEntityNameByEntityClass( concreteEntityClass );
			if ( entityName == null ) {
				throw new HibernateException(
						"Unable to resolve entity name from Class [" + concreteEntityClass.getName() + "]"
								+ " expected instance/subclass of [" + getEntityName() + "]"
				);
			}
			return entityName;
		}
	}

	@Override
	public EntityNameResolver[] getEntityNameResolvers() {
		return null;
	}
}
