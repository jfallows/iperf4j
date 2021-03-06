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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class IperfStream implements AutoCloseable
{
    private final IperfTestStreamInfo info;
    private final SocketChannel channel;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;

    public IperfStream(
        IperfTestStreamInfo info,
        SocketChannel channel,
        ByteBuffer readBuffer,
        ByteBuffer writeBuffer)
    {
        this.info = info;
        this.channel = channel;
        this.readBuffer = readBuffer;
        this.writeBuffer = writeBuffer.duplicate().clear();
    }

    @Override
    public void close()
    {
        try
        {
            channel.close();
        }
        catch (IOException ex)
        {
            // ignore
        }
    }

    void onReadyOps(
        SelectionKey key)
    {
        final int readyOps = key.isValid() ? key.readyOps() : 0;

        try
        {
            if ((readyOps & OP_READ) != 0)
            {
                final ByteBuffer buffer = this.readBuffer;
                do
                {
                    buffer.clear();
                    info.bytes += Math.max(channel.read(buffer), 0L);
                } while (!buffer.hasRemaining());
            }

            if ((readyOps & OP_WRITE) != 0)
            {
                final ByteBuffer buffer = this.writeBuffer;
                info.bytes += channel.write(buffer);

                if (!buffer.hasRemaining())
                {
                    buffer.clear();
                    info.blocks++;
                }
            }
        }
        catch (IOException ex)
        {
            close();
        }
    }
}
