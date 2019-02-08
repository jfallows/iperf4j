/**
 * Copyright 2016-2019 John Fallows
 *
 * John Fallows licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.jfallows.iperf4j;

public enum IperfState
{
    TEST_START(1),
    TEST_RUNNING(2),
    TEST_END(4),
    PARAM_EXCHANGE(9),
    CREATE_STREAMS(10),
    SERVER_TERMINATE(11),
    CLIENT_TERMINATE(12),
    EXCHANGE_RESULTS(13),
    DISPLAY_RESULTS(14),
    IPERF_START(15),
    IPERF_DONE(16),
    ACCESS_DENIED(-1),
    SERVER_ERROR(-2);

    private static final IperfState[] STATES = IperfState.values();

    private final byte value;

    public byte value()
    {
        return value;
    }

    IperfState(
        int value)
    {
        this.value = (byte)value;
    }

    public static IperfState valueOf(
        int value)
    {
        IperfState match = null;

        for (IperfState state : STATES)
        {
            if (state.value == value)
            {
                match = state;
                break;
            }
        }

        return match;
    }
}
