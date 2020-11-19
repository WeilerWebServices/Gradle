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

public class Foo implements java.io.Serializable
{
    int myI = 0;

    public Foo()
    {
    }

    public void setI(int i)
    {
      myI = i;
    }

    public int getI()
    {
        return myI;
    }

    public boolean equals(Object o)
    {
        return ((Foo)o).getI() == myI;
    }
}
