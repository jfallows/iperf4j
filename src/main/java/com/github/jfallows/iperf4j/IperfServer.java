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

import static com.github.jfallows.iperf4j.IperfState.CLIENT_TERMINATE;
import static com.github.jfallows.iperf4j.IperfState.CREATE_STREAMS;
import static com.github.jfallows.iperf4j.IperfState.IPERF_DONE;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

public final class IperfServer implements AutoCloseable
{
    private final Selector selector;
    private final ServerSocketChannel channel;
    private final ByteBuffer readBuffer;

    private IperfTest test;
    private IperfControl control;

    public IperfServer() throws IOException
    {
        this.test = new IperfTest();
        this.selector = Selector.open();
        this.channel = ServerSocketChannel.open();
        this.readBuffer = ByteBuffer.allocateDirect(16384);
        channel.configureBlocking(false);
    }

    public void bind(
        SocketAddress local) throws IOException
    {
        channel.bind(local);

        final SelectionKey key = channel.register(selector, OP_ACCEPT);
        attach(key, this::onReadyOps);
    }

    public boolean isOpen()
    {
        return selector.isOpen();
    }

    public void process() throws IOException
    {
        final int selected = selector.select(MILLISECONDS.toMillis(500));
        if (selected != 0)
        {
            final Set<SelectionKey> selectedKeys = selector.selectedKeys();

            for(Iterator<SelectionKey> i = selectedKeys.iterator(); i.hasNext();)
            {
                SelectionKey selectedKey = i.next();
                Consumer<SelectionKey> handler = attachment(selectedKey);
                handler.accept(selectedKey);
                i.remove();
            }
        }

        if (test.state == IPERF_DONE || test.state == CLIENT_TERMINATE)
        {
            if (control != null)
            {
                control.close();
                control = null;
            }
            test = new IperfTest();
        }
    }

    @Override
    public void close() throws IOException
    {
        selector.close();
    }

    private void onReadyOps(
        SelectionKey key)
    {
        assert key.readyOps() == OP_ACCEPT;

        try
        {
            final SocketChannel child = channel.accept();
            child.configureBlocking(false);

            if (control == null)
            {
                assert test.state != CREATE_STREAMS;

                final IperfControl newControl = new IperfControl(test, child, readBuffer);
                final SelectionKey controlKey = child.register(selector, OP_READ);
                attach(controlKey, newControl::onReadyOps);
                this.control = newControl;
            }
            else if (test.state == CREATE_STREAMS && control.canCreateStreams())
            {
                final IperfStream newStream = control.createStream(child);
                final SelectionKey streamKey = child.register(selector, OP_READ);
                attach(streamKey, newStream::onReadyOps);
            }
            else
            {
                child.close();
            }
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private static void attach(
        SelectionKey key,
        Consumer<SelectionKey> handler)
    {
        key.attach(handler);
    }

    @SuppressWarnings("unchecked")
    private static Consumer<SelectionKey> attachment(
        SelectionKey selectedKey)
    {
        return (Consumer<SelectionKey>) selectedKey.attachment();
    }
}
