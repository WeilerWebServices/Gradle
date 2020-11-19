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


package org.eclipse.jetty.nosql.mongodb;

import java.net.UnknownHostException;

import org.eclipse.jetty.server.session.AbstractSessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.server.session.SessionDataStore;


import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;

/**
 * MongoSessionDataStoreFactory
 *
 *
 */
public class MongoSessionDataStoreFactory extends AbstractSessionDataStoreFactory
{
    String _dbName;
    String _collectionName;
    String _host;
    String _connectionString;
    int _port = -1;

    /**
     * @return the host
     */
    public String getHost()
    {
        return _host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host)
    {
        _host = host;
    }

    /**
     * @return the port
     */
    public int getPort()
    {
        return _port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port)
    {
        _port = port;
    }

    /**
     * @return the dbName
     */
    public String getDbName()
    {
        return _dbName;
    }

    /**
     * @param dbName the dbName to set
     */
    public void setDbName(String dbName)
    {
        _dbName = dbName;
    }

    /**
     * @return the connectionString
     */
    public String getConnectionString()
    {
        return _connectionString;
    }

    /**
     * @param  connectionString the connection string to set. This has priority over dbHost and port
     */
    public void setConnectionString(String connectionString)
    {
        _connectionString = connectionString;
    }

    /**
     * @return the collectionName
     */
    public String getCollectionName()
    {
        return _collectionName;
    }

    /**
     * @param collectionName the collectionName to set
     */
    public void setCollectionName(String collectionName)
    {
        _collectionName = collectionName;
    }


    /**
     * @throws Exception  {@link UnknownHostException} if any issue while resolving MongoDB Host
     * @see org.eclipse.jetty.server.session.SessionDataStoreFactory#getSessionDataStore(org.eclipse.jetty.server.session.SessionHandler)
     */
    @Override
    public SessionDataStore getSessionDataStore(SessionHandler handler) throws Exception
    {
        MongoSessionDataStore store = new MongoSessionDataStore();
        store.setGracePeriodSec(getGracePeriodSec());
        store.setSavePeriodSec(getSavePeriodSec());
        Mongo mongo;

        if (!StringUtil.isBlank(getConnectionString()))
            mongo = new Mongo(new MongoURI(getConnectionString()));
        else if (!StringUtil.isBlank(getHost()) && getPort() != -1)
            mongo = new Mongo(getHost(), getPort());
        else if (!StringUtil.isBlank(getHost()))
            mongo = new Mongo(getHost());
        else
            mongo = new Mongo();
        store.setDBCollection(mongo.getDB(getDbName()).getCollection(getCollectionName()));
        return store;
    }



}
