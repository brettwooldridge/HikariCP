/*
 * Copyright (C) 2013 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari.proxy;

import java.util.TimerTask;

import org.slf4j.LoggerFactory;

/**
 * @author Brett Wooldridge
 */
public class LeakTask extends TimerTask
{
    private final long leakTime;
    private StackTraceElement[] stackTrace;

    public LeakTask(StackTraceElement[] stackTrace, long leakDetectionThreshold)
    {
        this.stackTrace = stackTrace;
        this.leakTime = System.currentTimeMillis() + leakDetectionThreshold;
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        if (System.currentTimeMillis() > leakTime)
        {
            Exception e = new Exception();
            e.setStackTrace(stackTrace);
            LoggerFactory.getLogger(LeakTask.class).warn("Connection leak detection triggered, stack trace follows", e);
            stackTrace = null;
        }
    }

    @Override
    public boolean cancel()
    {
        boolean cancelled = super.cancel();
        if (cancelled)
        {
            stackTrace = null;
        }
        return cancelled;
    }
}
