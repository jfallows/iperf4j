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
import static com.github.jfallows.iperf4j.IperfState.DISPLAY_RESULTS;
import static com.github.jfallows.iperf4j.IperfState.EXCHANGE_RESULTS;
import static com.github.jfallows.iperf4j.IperfState.IPERF_DONE;
import static com.github.jfallows.iperf4j.IperfState.PARAM_EXCHANGE;
import static com.github.jfallows.iperf4j.IperfState.TEST_RUNNING;
import static com.github.jfallows.iperf4j.IperfState.TEST_START;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class IperfControl implements AutoCloseable
{
    private static final int COOKIE_SIZE = 37; // size of ASCII UUID

    private final IperfTest test;
    private final SocketChannel channel;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;
    private final Set<IperfStream> streams;

    public IperfControl(
        IperfTest test,
        SocketChannel channel,
        ByteBuffer readBuffer)
    {
        this.test = test;
        this.channel = channel;
        this.readBuffer = readBuffer;
        this.writeBuffer = ByteBuffer.allocateDirect(32768);
        this.streams = new LinkedHashSet<>();
    }

    public IperfStream createStream(
        SocketChannel child)
    {
        final IperfTestStreamInfo newStreamInfo = new IperfTestStreamInfo();
        newStreamInfo.id = streams.isEmpty() ? 1 : streams.size() + 2;
        final IperfStream newStream = new IperfStream(newStreamInfo, child, readBuffer);
        streams.add(newStream);
        test.info.streams.add(newStreamInfo);

        if (streams.size() == test.streams)
        {
            doChangeState(TEST_START);
            // TODO: (setup timers, etc)
            doChangeState(TEST_RUNNING);
        }

        return newStream;
    }

    public boolean canCreateStreams()
    {
        return streams.size() < test.streams;
    }

    @Override
    public void close() throws IOException
    {
        channel.close();
        streams.forEach(IperfStream::close);
    }

    void onReadyOps(
        SelectionKey key)
    {
        assert (key.readyOps() & OP_READ) != 0;

        switch (test.state)
        {
        case IPERF_START:
            doReadCookie();
            doChangeState(PARAM_EXCHANGE);
            break;
        case PARAM_EXCHANGE:
            doExchangeParams();
            doChangeState(CREATE_STREAMS);
            break;
        case EXCHANGE_RESULTS:
            doExchangeResults();
            doChangeState(DISPLAY_RESULTS);
            break;
        default:
            doReadState();
            onStateChange();
            break;
        }
    }

    private void onStateChange()
    {
        switch (test.state)
        {
        case TEST_START:
            break;
        case TEST_END:
            streams.forEach(IperfStream::close);
            doChangeState(EXCHANGE_RESULTS);
            break;
        case IPERF_DONE:
            break;
        case CLIENT_TERMINATE:
            // TODO: display results
            streams.forEach(IperfStream::close);
            test.state = IPERF_DONE;
            break;
        default:
            // TODO: error
            break;
        }
    }

    private void doReadState()
    {
        ByteBuffer stateBuf = doReadNBytes(1);

        if (stateBuf.hasRemaining())
        {
            byte stateByte = stateBuf.get();
            IperfState state = IperfState.valueOf(stateByte);

            test.state = state;
        }
        else
        {
            test.state = CLIENT_TERMINATE;
        }
    }

    private void doReadCookie()
    {
        ByteBuffer cookieBuf = doReadNBytes(COOKIE_SIZE);
        byte[] cookieBytes = new byte[cookieBuf.remaining()];
        cookieBuf.get(cookieBytes);
        test.cookie = new String(cookieBytes, UTF_8);
    }

    private void doExchangeParams()
    {
        // read JSON params (4-byte length prefix)
        ByteBuffer sizeBuf = doReadNBytes(Integer.BYTES);
        int size = sizeBuf.getInt();
        ByteBuffer paramsBuf = doReadNBytes(size);
        byte[] paramsBytes = new byte[paramsBuf.remaining()];
        paramsBuf.get(paramsBytes);
        String paramsUTF8 = new String(paramsBytes, UTF_8);

        final JsonObject params = (JsonObject) new JsonParser().parse(paramsUTF8);
        final JsonElement tcp = params.get("tcp");
        if (tcp != null && tcp.getAsBoolean())
        {
            test.protocol = "tcp";
        }

        final JsonElement parallel = params.get("parallel");
        if (parallel != null)
        {
            test.streams = parallel.getAsInt();
        }

        // TODO: other params
        test.info.senderHasRetransmits = -1;
    }

    private void doExchangeResults()
    {
        // read JSON results (4-byte length prefix)
        ByteBuffer sizeBuf = doReadNBytes(Integer.BYTES);
        int size = sizeBuf.getInt();
        doReadNBytes(size); // results

        Gson gson = IperfUtil.newGson();
        String newResultsUTF8 = gson.toJson(test.info);

        byte[] newResultsBytes = newResultsUTF8.getBytes(UTF_8);

        writeBuffer.clear();
        writeBuffer.putInt(newResultsBytes.length);
        writeBuffer.put(newResultsBytes);
        writeBuffer.flip();
        doWriteBytes(writeBuffer);
    }

    private void doChangeState(
        IperfState state)
    {
        writeBuffer.clear();
        writeBuffer.put(state.value());
        writeBuffer.flip();

        doWriteBytes(writeBuffer);

        test.state = state;
    }

    private ByteBuffer doReadNBytes(
        int nbytes)
    {
        try
        {
            readBuffer.clear();
            readBuffer.limit(nbytes);

            while (channel.read(readBuffer) != -1 && readBuffer.hasRemaining())
            {
                Thread.onSpinWait();
            }

            // TODO: handle channel close

            readBuffer.flip();

            return readBuffer;
        }
        catch (IOException ex)
        {
            readBuffer.clear();
            readBuffer.limit(1);
            readBuffer.put(CLIENT_TERMINATE.value());
            readBuffer.flip();
            return readBuffer;
        }
    }

    private void doWriteBytes(
        ByteBuffer writeBuffer)
    {
        try
        {
            while (channel.write(writeBuffer) != -1 && writeBuffer.hasRemaining())
            {
                Thread.onSpinWait();
            }

            // TODO: handle channel close

        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
