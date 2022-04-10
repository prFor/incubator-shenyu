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

import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * AbstractRegistry .
 */
public abstract class AbstractRegistry implements Registry {
    
    
    /**
     * The constant SEPARATOR.
     */
    protected static final String SEPARATOR = "/";
    
    private final RegistryConfig registryConfig;
    
    private final Map<String, Set<RegistryConsumer>> subscribed = new ConcurrentHashMap<>(16);
    
    /**
     * Instantiates a new Abstract registry.
     *
     * @param registryConfig the registry config
     */
    public AbstractRegistry(final RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }
    
    /**
     * Gets registry config.
     *
     * @return the registry config
     */
    public RegistryConfig getRegistryConfig() {
        return registryConfig;
    }
    
    /**
     * Subscribe.
     * Subscribe to different path data.
     *
     * @param pathList the path list
     * @param consumer the consumer
     */
    @Override
    public void subscribe(final List<String> pathList, final RegistryConsumer consumer) {
        Assert.notNull(pathList, "pathList is null");
        Assert.notNull(consumer, "consumer is null");
        String path = this.getPath(pathList);
        Set<RegistryConsumer> lists = subscribed.computeIfAbsent(path, set -> new CopyOnWriteArraySet<>());
        lists.add(consumer);
        this.doSubscribe(pathList);
    }
    
    /**
     * notify data.
     *
     * @param path  the path
     * @param datas the datas
     */
    protected void notify(String path, String data) {
        Set<RegistryConsumer> consumers = subscribed.get(path);
        Optional.ofNullable(consumers).ifPresent(c -> c.forEach(cs -> cs.data(data)));
    }
    
    /**
     * Convert data into relevant paths.
     *
     * @param pathList pathList.
     * @return String. path
     */
    protected String getPath(List<String> pathList) {
        return SEPARATOR + String.join(SEPARATOR, pathList);
    }
    
    /**
     * Subclasses implement subscription operations.
     *
     * @param pathList pathList.
     */
    protected abstract void doSubscribe(final List<String> pathList);
    
}
