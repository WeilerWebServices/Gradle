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


package org.eclipse.jetty.servlet;

import java.util.EventListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * ListenerHolder
 *
 * Specialization of BaseHolder for servlet listeners. This
 * allows us to record where the listener originated - web.xml,
 * annotation, api etc.
 */
public class ListenerHolder extends BaseHolder<EventListener>
{
    private EventListener _listener;
    private boolean _initialized = false;
    

    public ListenerHolder ()
    {
        this (Source.EMBEDDED);
    }
    
    public ListenerHolder(Source source)
    {
        super(source);
    }

    public ListenerHolder(Class<? extends EventListener> listenerClass)
    {
        super(Source.EMBEDDED);
        setHeldClass(listenerClass);
    }
   
    public EventListener getListener()
    {
        return _listener;
    }

    /**
     * Set an explicit instance. In this case,
     * just like ServletHolder and FilterHolder,
     * the listener will not be introspected for
     * annotations like Resource etc.
     * 
     * @param listener
     */
    public void setListener (EventListener listener)
    {
        _listener = listener;
        _extInstance=true;
        setHeldClass(_listener.getClass());
    }


    public void initialize (ServletContext context) throws Exception
    {
        if (!_initialized)
        {
            initialize();

            if (_listener == null)
            {
                //create an instance of the listener and decorate it
                try
                {                    
                    _listener = (context instanceof ServletContextHandler.Context)
                            ?((ServletContextHandler.Context)context).createListener(getHeldClass())
                            :getHeldClass().getDeclaredConstructor().newInstance();

                }
                catch (ServletException se)
                {
                    Throwable cause = se.getRootCause();
                    if (cause instanceof InstantiationException)
                        throw (InstantiationException)cause;
                    if (cause instanceof IllegalAccessException)
                        throw (IllegalAccessException)cause;
                    throw se;
                }
            }
            _initialized = true;
        }
    }


    @Override
    public void doStart() throws Exception
    {
        super.doStart();
        if (!java.util.EventListener.class.isAssignableFrom(_class))
        {
            String msg = _class+" is not a java.util.EventListener";
            super.stop();
            throw new IllegalStateException(msg);
        }
    }



    @Override
    public void doStop() throws Exception
    {
        super.doStop();
        if (!_extInstance)
            _listener = null;
        _initialized = false;
    }

    @Override
    public String toString()
    {
        return super.toString()+": "+getClassName();
    } 
}
