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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.ClassLoadingObjectInputStream;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * MongoUtils
 *
 * Some utility methods for manipulating mongo data. This class facilitates testing.
 *
 */
public class MongoUtils
{
    
    public static Object decodeValue(final Object valueToDecode) throws IOException, ClassNotFoundException
    {
        if (valueToDecode == null || valueToDecode instanceof Number || valueToDecode instanceof String || valueToDecode instanceof Boolean || valueToDecode instanceof Date)
        {
            return valueToDecode;
        }
        else if (valueToDecode instanceof byte[])
        {
            final byte[] decodeObject = (byte[])valueToDecode;
            final ByteArrayInputStream bais = new ByteArrayInputStream(decodeObject);
            final ClassLoadingObjectInputStream objectInputStream = new ClassLoadingObjectInputStream(bais);
            return objectInputStream.readUnshared();
        }
        else if (valueToDecode instanceof DBObject)
        {
            Map<String, Object> map = new HashMap<String, Object>();
            for (String name : ((DBObject)valueToDecode).keySet())
            {
                String attr = decodeName(name);
                map.put(attr,decodeValue(((DBObject)valueToDecode).get(name)));
            }
            return map;
        }
        else
        {
            throw new IllegalStateException(valueToDecode.getClass().toString());
        }
    }
   
    
    
    public static String decodeName(String name)
    {
        return name.replace("%2E",".").replace("%25","%");
    }
    

    
    public static String encodeName(String name)
    {
        return name.replace("%","%25").replace(".","%2E");
    }


    
    public static Object encodeName(Object value) throws IOException
    {
        if (value instanceof Number || value instanceof String || value instanceof Boolean || value instanceof Date)
        {
            return value;
        }
        else if (value.getClass().equals(HashMap.class))
        {
            BasicDBObject o = new BasicDBObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet())
            {
                if (!(entry.getKey() instanceof String))
                {
                    o = null;
                    break;
                }
                o.append(encodeName(entry.getKey().toString()),encodeName(entry.getValue()));
            }

            if (o != null)
                return o;
        }
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.reset();
        out.writeUnshared(value);
        out.flush();
        return bout.toByteArray();
    }
    


    /**
     * Dig through a given dbObject for the nested value
     * 
     * @param dbObject the mongo object to search
     * @param nestedKey the field key to find
     * 
     * @return the value of the field key
     */
    public static Object getNestedValue(DBObject dbObject, String nestedKey)
    {
        String[] keyChain = nestedKey.split("\\.");

        DBObject temp = dbObject;

        for (int i = 0; i < keyChain.length - 1; ++i)
        {
            temp = (DBObject)temp.get(keyChain[i]);

            if ( temp == null )
            {
                return null;
            }
        }

        return temp.get(keyChain[keyChain.length - 1]);
    }

    
}
