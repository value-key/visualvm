/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.jfr.jdk9.model.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.graalvm.visualvm.jfr.model.JFRClass;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRStackTrace;
import org.graalvm.visualvm.jfr.model.JFRThread;

/**
 *
 * @author Jiri Sedlacek
 */
public class JFRJDK9Event extends JFREvent {
    
    protected final RecordedEvent event;
    
    
    public JFRJDK9Event(RecordedEvent event) {
        this.event = event;
    }
    

    @Override
    public Duration getDuration(String key) throws JFRPropertyNotAvailableException {
        Object duration;
        switch (key) {
            case "eventDuration": // NOI18N
                duration = event.getDuration();
                break;
            default:
                duration = getValue(key);
        }

        if (duration == null) return null;
        else if (duration instanceof Duration) return (Duration)duration;
        else if (duration instanceof Number) return Duration.ofMillis(((Number)duration).longValue()); // TODO: verify whether correct for v1 (Java 9 & 10)!!!
        else throw new JFRPropertyNotAvailableException("No duration value available: " + key);
    }

    @Override
    public Instant getInstant(String key) throws JFRPropertyNotAvailableException {
        Object instant;
        switch (key) {
            case "eventTime": // NOI18N
            case "startTime": // NOI18N
                instant = event.getStartTime();
                break;
            case "endTime": // NOI18N
                instant = event.getEndTime();
                break;
            default:
                instant = getValue(key);
        }

        if (instant == null) return null;
        else if (instant instanceof Instant) return (Instant)instant;
        else if (instant instanceof Number) return Instant.ofEpochMilli(((Number)instant).longValue()); // TODO: verify whether correct for v1 (Java 9 & 10)!!!
        else throw new JFRPropertyNotAvailableException("No instant value available: " + key);
    }
    
    
    @Override
    public JFRClass getClass(String key) throws JFRPropertyNotAvailableException {
        Object rclass = getValue(key);
        
        if (rclass == null) return null;
        else if (rclass instanceof RecordedClass) return new JFRJDK9Class((RecordedClass)rclass);
        else throw new JFRPropertyNotAvailableException("No class value available: " + key);
    }
    
    @Override
    public JFRThread getThread(String key) throws JFRPropertyNotAvailableException {
        Object thread;
        switch (key) {
            case "eventThread": // NOI18N
                thread = event.getThread();
                if (thread == null) try {
                    thread = getValue("thread"); // NOI18N // jdk.ThreadAllocationStatistics
                } catch (JFRPropertyNotAvailableException e) {
                    thread = null;
                }
                break;
            default:
                thread = getValue(key);
        }

        if (thread == null) return null;
        else if (thread instanceof RecordedThread) return new JFRJDK9Thread((RecordedThread)thread);
        else throw new JFRPropertyNotAvailableException("No thread value available: " + key);
    }

    @Override
    public JFRStackTrace getStackTrace(String key) throws JFRPropertyNotAvailableException {
        Object stackTrace;
        switch (key) {
            case "eventStackTrace": // NOI18N
                stackTrace = event.getStackTrace();
                break;
            default:
                stackTrace = getValue(key);
        }

        if (stackTrace == null) return null;
        else if (stackTrace instanceof RecordedStackTrace) return new JFRJDK9StackTrace((RecordedStackTrace)stackTrace);
        else throw new JFRPropertyNotAvailableException("No stacktrace value available: " + key);
    }
    
    
    @Override
    public Object getValue(String key) throws JFRPropertyNotAvailableException {
        try {
            return event.getValue(key);
        } catch (IllegalArgumentException e) {
            throw new JFRPropertyNotAvailableException(e);
        }
    }
    
    
    @Override
    public List<Comparable> getDisplayableValues(boolean includeExperimental) {
        List<Comparable> values = new ArrayList();
        Iterator<ValueDescriptor> descriptors = DisplayableSupport.displayableValueDescriptors(event.getEventType(), includeExperimental);
        while (descriptors.hasNext()) values.add(DisplayableSupport.getDisplayValue(this, descriptors.next()));
        return values;
    }
    
    
    @Override
    public int hashCode() {
        return event.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof JFRJDK9Event ? event.equals(((JFRJDK9Event)o).event) : false;
    }
    
    
    @Override
    public String toString() {
        return event.toString();
    }
    
}
