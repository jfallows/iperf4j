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

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public enum IperfMode
{
    FORWARD(OP_READ),
    REVERSE(OP_WRITE),
    BIDIRECTIONAL(OP_READ | OP_WRITE);

    private final int interestOps;

    public int interestOps()
    {
        return interestOps;
    }

    IperfMode(
        int interestOps)
    {
        this.interestOps = interestOps;
    }
}
