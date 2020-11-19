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

package org.eclipse.jetty.websocket.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.is;

import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.api.extensions.OutgoingFrames;

/**
 * Useful for testing the production of sane frame ordering from various components.
 */
public class SaneFrameOrderingAssertion implements OutgoingFrames
{
    boolean priorDataFrame = false;
    public int frameCount = 0;

    @Override
    public void outgoingFrame(Frame frame, WriteCallback callback, BatchMode batchMode)
    {
        byte opcode = frame.getOpCode();
        assertThat("OpCode.isKnown(" + opcode + ")", OpCode.isKnown(opcode), is(true));

        switch (opcode)
        {
            case OpCode.TEXT:
                assertFalse(priorDataFrame, "Unexpected " + OpCode.name(opcode) + " frame, was expecting CONTINUATION");
                break;
            case OpCode.BINARY:
                assertFalse(priorDataFrame, "Unexpected " + OpCode.name(opcode) + " frame, was expecting CONTINUATION");
                break;
            case OpCode.CONTINUATION:
                assertTrue(priorDataFrame, "CONTINUATION frame without prior !FIN");
                break;
            case OpCode.CLOSE:
                assertFalse(frame.isFin(), "Fragmented Close Frame [" + OpCode.name(opcode) + "]");
                break;
            case OpCode.PING:
                assertFalse(frame.isFin(), "Fragmented Close Frame [" + OpCode.name(opcode) + "]");
                break;
            case OpCode.PONG:
                assertFalse(frame.isFin(), "Fragmented Close Frame [" + OpCode.name(opcode) + "]");
                break;
        }

        if (OpCode.isDataFrame(opcode))
        {
            priorDataFrame = !frame.isFin();
        }

        frameCount++;

        if (callback != null)
            callback.writeSuccess();
    }
}
