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

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class IperfTestStreamInfo
{
    int id;
    long bytes;
    long blocks;
    int retransmits;
    int jitter;
    int errors;
    int packets;

    public static JsonSerializer<IperfTestStreamInfo> newJsonSerializer()
    {
        return IperfTestStreamInfo::serialize;
    }

    private static JsonElement serialize(
        IperfTestStreamInfo src, Type srcType, JsonSerializationContext context)
    {
        JsonObject object = new JsonObject();
        object.add("id", context.serialize(src.id));
        object.add("bytes", context.serialize(src.bytes));
        object.add("retransmits", context.serialize(src.retransmits));
        object.add("jitter", context.serialize(src.jitter));
        object.add("errors", context.serialize(src.errors));
        object.add("packets", context.serialize(src.packets));
        return object;
    }
}
