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
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class IperfTestInfo
{
    int cpuUtilTotal;
    int cpuUtilUser;
    int cpuUtilSystem;
    int senderHasRetransmits;

    final Set<IperfTestStreamInfo> streams = new LinkedHashSet<>();

    public static JsonSerializer<IperfTestInfo> newJsonSerializer()
    {
        return IperfTestInfo::serialize;
    }

    private static JsonElement serialize(
        IperfTestInfo src, Type srcType, JsonSerializationContext context)
    {
        JsonObject object = new JsonObject();
        object.add("cpu_util_total", context.serialize(src.cpuUtilTotal));
        object.add("cpu_util_user", context.serialize(src.cpuUtilUser));
        object.add("cpu_util_system", context.serialize(src.cpuUtilSystem));
        object.add("sender_has_retransmits", context.serialize(src.senderHasRetransmits));
        object.add("streams", context.serialize(src.streams));
        return object;
    }
}
