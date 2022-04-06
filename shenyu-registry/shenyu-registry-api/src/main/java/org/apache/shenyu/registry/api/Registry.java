/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.registry.api;

import java.util.List;
import java.util.Map;

/**
 * Registry .
 * Implementation of registry using and not being able to.
 */
public interface Registry {

    /**
     * Registry.
     * data registration.
     *
     * @param pathList  the path list
     * @param data      the data
     * @param ephemeral node properties.
     */
    void registry(List<String> pathList,
                  Map<String, Object> data,
                  boolean ephemeral);

    /**
     * unregister.
     *
     * @param path the path
     */
    void unRegistry(Map<String, Object> path);

    /**
     * Subscribe.
     * Subscribe to different path data.
     *
     * @param path     the path
     * @param consumer the consumer
     */
    void subscribe(Map<String, Object> path, RegistryConsumer consumer);
}
