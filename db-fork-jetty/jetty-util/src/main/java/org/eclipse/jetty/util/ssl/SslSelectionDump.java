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

package org.eclipse.jetty.util.ssl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jetty.util.component.Dumpable;

class SslSelectionDump implements Dumpable
{
    static class CaptionedList extends ArrayList<String> implements Dumpable
    {
        private final String caption;

        public CaptionedList(String caption)
        {
            this.caption = caption;
        }

        @Override
        public String dump()
        {
            return Dumpable.dump(SslSelectionDump.CaptionedList.this);
        }

        @Override
        public void dump(Appendable out, String indent) throws IOException
        {
            Object[] array = toArray();
            Dumpable.dumpObjects(out, indent, caption + " size="+array.length, array);
        }
    }
    
    final String type;
    final SslSelectionDump.CaptionedList enabled = new SslSelectionDump.CaptionedList("Enabled");
    final SslSelectionDump.CaptionedList disabled = new SslSelectionDump.CaptionedList("Disabled");
    
    public SslSelectionDump(String type,
                            String[] supportedByJVM,
                            String[] enabledByJVM,
                            String[] excludedByConfig,
                            String[] includedByConfig)
    {
        this.type = type;

        List<String> jvmEnabled = Arrays.asList(enabledByJVM);
        List<Pattern> excludedPatterns = Arrays.stream(excludedByConfig)
                .map((entry) -> Pattern.compile(entry))
                .collect(Collectors.toList());
        List<Pattern> includedPatterns = Arrays.stream(includedByConfig)
                .map((entry) -> Pattern.compile(entry))
                .collect(Collectors.toList());
        
        Arrays.stream(supportedByJVM)
                .sorted(Comparator.naturalOrder())
                .forEach((entry) ->
                {
                    boolean isPresent = true;
                    
                    StringBuilder s = new StringBuilder();
                    s.append(entry);

                    for (Pattern pattern : excludedPatterns)
                    {
                        Matcher m = pattern.matcher(entry);
                        if (m.matches())
                        {
                            if (isPresent)
                            {
                                s.append(" -");
                                isPresent = false;
                            }
                            else
                            {
                                s.append(",");
                            }
                            s.append(" ConfigExcluded:'").append(pattern.pattern()).append('\'');
                        }
                    }

                    boolean isIncluded = false;

                    if (!includedPatterns.isEmpty())
                    {
                        for (Pattern pattern : includedPatterns)
                        {
                            Matcher m = pattern.matcher(entry);
                            if (m.matches())
                            {
                                isIncluded = true;
                                break;
                            }
                        }
                        
                        if (!isIncluded)
                        {
                            if (isPresent)
                            {
                                s.append(" -");
                                isPresent = false;
                            }
                            else
                            {
                                s.append(",");
                            }

                            s.append(" ConfigIncluded:NotSelected");
                        }
                    }

                    if (!isIncluded && !jvmEnabled.contains(entry))
                    {
                        if (isPresent)
                        {
                            s.append(" -");
                            isPresent = false;
                        }

                        s.append(" JVM:disabled");
                    }

                    if (isPresent)
                    {
                        enabled.add(s.toString());
                    }
                    else
                    {
                        disabled.add(s.toString());
                    }
                });
    }
    
    @Override
    public String dump()
    {
        return Dumpable.dump(this);
    }
    
    @Override
    public void dump(Appendable out, String indent) throws IOException
    {
        Dumpable.dumpObjects(out, indent, this, enabled, disabled);
    }

    @Override
    public String toString()
    {
        return String.format("%s Selections", type);
    }
}
