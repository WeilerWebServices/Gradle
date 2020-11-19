/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.reflection;

import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.classgen.asm.BytecodeHelper;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.codehaus.groovy.runtime.callsite.CallSite;
import org.codehaus.groovy.runtime.callsite.CallSiteGenerator;
import org.codehaus.groovy.runtime.callsite.PogoMetaMethodSite;
import org.codehaus.groovy.runtime.callsite.PojoMetaMethodSite;
import org.codehaus.groovy.runtime.callsite.StaticMetaMethodSite;
import org.codehaus.groovy.runtime.metaclass.MethodHelper;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

public class CachedMethod extends MetaMethod implements Comparable {
    public static final CachedMethod[] EMPTY_ARRAY = new CachedMethod[0];
    public final CachedClass cachedClass;

    private final Method cachedMethod;
    private int hashCode;

    private static final MyComparator COMPARATOR = new MyComparator();

    private SoftReference<Constructor> pogoCallSiteConstructor, pojoCallSiteConstructor, staticCallSiteConstructor;

    private boolean skipCompiled;

    public CachedMethod(CachedClass clazz, Method method) {
        this.cachedMethod = method;
        this.cachedClass = clazz;
    }

    public CachedMethod(Method method) {
        this(ReflectionCache.getCachedClass(method.getDeclaringClass()),method);
    }

    public static CachedMethod find(Method method) {
        CachedMethod[] methods = ReflectionCache.getCachedClass(method.getDeclaringClass()).getMethods();
//        for (int i = 0; i < methods.length; i++) {
//            CachedMethod cachedMethod = methods[i];
//            if (cachedMethod.cachedMethod.equals(method))
//                return cachedMethod;
//        }
//        return null;
        int i = Arrays.binarySearch(methods, method, COMPARATOR);
        if (i < 0)
          return null;

        return methods[i];
    }

    protected Class[] getPT() {
        return cachedMethod.getParameterTypes();
    }

    public String getName() {
        return cachedMethod.getName();
    }

    public String getDescriptor() {
        return BytecodeHelper.getMethodDescriptor(getReturnType(), getNativeParameterTypes());
    }

    public CachedClass getDeclaringClass() {
        return cachedClass;
    }

    public final Object invoke(Object object, Object[] arguments) {
        try {
            AccessPermissionChecker.checkAccessPermission(cachedMethod);
        } catch (CacheAccessControlException ex) {
            throw new InvokerInvocationException(ex);
        }
        try {
            return cachedMethod.invoke(object, arguments);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new InvokerInvocationException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause(); 
            throw (cause instanceof RuntimeException && !(cause instanceof MissingMethodException)) ? 
                    (RuntimeException) cause : new InvokerInvocationException(e);
        }
    }

    public ParameterTypes getParamTypes() {
        return null;
    }

    public Class getReturnType() {
        return cachedMethod.getReturnType();
    }

    public int getParamsCount() {
        return getParameterTypes().length;
    }

    public int getModifiers() {
        return cachedMethod.getModifiers();
    }


    public String getSignature() {
        return getName() + getDescriptor();
    }

    public final Method setAccessible() {
        AccessPermissionChecker.checkAccessPermission(cachedMethod);
//        if (queuedToCompile.compareAndSet(false,true)) {
//            if (isCompilable())
//              CompileThread.addMethod(this);
//        }
        return cachedMethod;
    }

    public boolean isStatic() {
        return MethodHelper.isStatic(cachedMethod);
    }

    public int compareTo(Object o) {
      if (o instanceof CachedMethod)
        return compareToCachedMethod((CachedMethod)o);
      else
        return compareToMethod((Method)o);
    }

    private int compareToCachedMethod(CachedMethod other) {
        if (other == null)
            return -1;

        final int strComp = getName().compareTo(other.getName());
        if (strComp != 0)
            return strComp;

        final int retComp = getReturnType().getName().compareTo(other.getReturnType().getName());
        if (retComp != 0)
            return retComp;

        CachedClass[] params = getParameterTypes();
        CachedClass[] otherParams = other.getParameterTypes();

        final int pd = params.length - otherParams.length;
        if (pd != 0)
            return pd;

        for (int i = 0; i != params.length; ++i) {
            final int nameComp = params[i].getName().compareTo(otherParams[i].getName());
            if (nameComp != 0)
                return nameComp;
        }

        final int classComp = cachedClass.toString().compareTo(other.getDeclaringClass().toString());
        if (classComp != 0)
            return classComp;

        throw new RuntimeException("Should never happen");
    }

    private int compareToMethod(Method other) {
        if (other == null)
            return -1;

        final int strComp = getName().compareTo(other.getName());
        if (strComp != 0)
            return strComp;

        final int retComp = getReturnType().getName().compareTo(other.getReturnType().getName());
        if (retComp != 0)
            return retComp;

        CachedClass[] params = getParameterTypes();
        Class[] mparams = other.getParameterTypes();

        final int pd = params.length - mparams.length;
        if (pd != 0)
            return pd;

        for (int i = 0; i != params.length; ++i) {
            final int nameComp = params[i].getName().compareTo(mparams[i].getName());
            if (nameComp != 0)
                return nameComp;
        }

        return 0;
    }

    public boolean equals(Object o) {
        return (o instanceof CachedMethod && cachedMethod.equals(((CachedMethod)o).cachedMethod))
                || (o instanceof Method && cachedMethod.equals(o));
    }

    public int hashCode() {
        if (hashCode == 0) {
           hashCode = cachedMethod.hashCode();
           if (hashCode == 0)
             hashCode = 0xcafebebe;
        }
        return hashCode;
    }

    public String toString() {
        return cachedMethod.toString();
    }
    
    private static Constructor getConstructor(SoftReference<Constructor> ref) {
        if (ref==null) return null;
        return ref.get();
    }

    public CallSite createPogoMetaMethodSite(CallSite site, MetaClassImpl metaClass, Class[] params) {
        if (!skipCompiled) {
            Constructor constr = getConstructor(pogoCallSiteConstructor);
            if (constr==null) {
                if (CallSiteGenerator.isCompilable(this)) {
                  constr = CallSiteGenerator.compilePogoMethod(this);
                }
                if (constr != null) {
                     pogoCallSiteConstructor = new SoftReference<Constructor> (constr);
                } else {
                    skipCompiled = true;
                }
            }
    
            if (constr!=null) {
                try {
                    return (CallSite) constr.newInstance(site, metaClass, this, params, constr);
                } catch (Error e) {
                    skipCompiled=true;
                    throw e;
                } catch (Throwable e) {
                    skipCompiled=true;
                }
            }
        }
        return new PogoMetaMethodSite.PogoCachedMethodSiteNoUnwrapNoCoerce(site, metaClass, this, params);
    }


    public CallSite createPojoMetaMethodSite(CallSite site, MetaClassImpl metaClass, Class[] params) {
        if (!skipCompiled) {
            Constructor constr = getConstructor(pojoCallSiteConstructor);
            if (constr==null) {
                if (CallSiteGenerator.isCompilable(this)) {
                  constr = CallSiteGenerator.compilePojoMethod(this);
                }
                if (constr != null) {
                    pojoCallSiteConstructor = new SoftReference<Constructor> (constr);
                } else {
                    skipCompiled = true;
                }
            }
    
            if (constr!=null) {
                try {
                    return (CallSite) constr.newInstance(site, metaClass, this, params, constr);
                } catch (Error e) {
                    skipCompiled=true;
                    throw e;
                } catch (Throwable e) {
                    skipCompiled=true;
                }
            }
        }
        return new PojoMetaMethodSite.PojoCachedMethodSiteNoUnwrapNoCoerce(site, metaClass, this, params);
    }

    public CallSite createStaticMetaMethodSite(CallSite site, MetaClassImpl metaClass, Class[] params) {
        if (!skipCompiled) {
            Constructor constr = getConstructor(staticCallSiteConstructor);
            if (constr==null) {
                if (CallSiteGenerator.isCompilable(this)) {
                  constr = CallSiteGenerator.compileStaticMethod(this);
                }
                if (constr != null) {
                    staticCallSiteConstructor = new SoftReference<Constructor> (constr);
                } else {
                    skipCompiled = true;
                }
            }
    
            if (constr!=null) {
                try {
                    return (CallSite) constr.newInstance(site, metaClass, this, params, constr);
                } catch (Error e) {
                    skipCompiled=true;
                    throw e;
                } catch (Throwable e) {
                    skipCompiled=true;
                }
            }
        }

        return new StaticMetaMethodSite.StaticMetaMethodSiteNoUnwrapNoCoerce(site, metaClass, this, params);
    }

    private static class MyComparator implements Comparator, Serializable {
        private static final long serialVersionUID = 8909277090690131302L;

        public int compare(Object o1, Object o2) {
            if (o1 instanceof CachedMethod)
                return ((CachedMethod)o1).compareTo(o2);
            else if (o2 instanceof CachedMethod)
                return -((CachedMethod)o2).compareTo(o1);
            else
                // really, this should never happen, it's evidence of corruption if it does
                throw new ClassCastException("One of the two comparables must be a CachedMethod");
        }
    }

    public Method getCachedMethod() {
        AccessPermissionChecker.checkAccessPermission(cachedMethod);
        return cachedMethod;
    }

}

