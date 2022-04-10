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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RegistryInfo .
 * Registered Information Definitions.
 */
public class RegistryInfo {
    
    private String host;
    
    private Integer port;
    
    private Set<String> paths;
    
    private Map<String, String> parameters;
    
    
    /**
     * Gets host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Sets host.
     *
     * @param host the host
     */
    public void setHost(final String host) {
        this.host = host;
    }
    
    /**
     * Gets port.
     *
     * @return the port
     */
    public Integer getPort() {
        return port;
    }
    
    /**
     * Sets port.
     *
     * @param port the port
     */
    public void setPort(final Integer port) {
        this.port = port;
    }
    
    /**
     * Gets parameters.
     *
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }
    
    /**
     * Sets parameters.
     *
     * @param parameters the parameters
     */
    public void setParameters(final Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Gets paths.
     *
     * @return the paths
     */
    public Set<String> getPaths() {
        return paths;
    }
    
    /**
     * Sets paths.
     *
     * @param paths the paths
     */
    public void setPaths(final Set<String> paths) {
        this.paths = paths;
    }
    
    /**
     * Gets service key.
     *
     * @param type the type
     * @return service key
     */
    public String getServiceKey(String type) {
        Assert.notNull(type, "type is null");
        Set<String> newPaths = Optional.ofNullable(this.getPaths()).orElse(Collections.emptySet());
        String path = String.join("/", newPaths);
        Map<String, String> parameters = this.getParameters();
        StringBuilder url = new StringBuilder(type);
        url.append("://");
        url.append(this.getHost()).append(":").append(this.getPort());
        url.append("/");
        url.append(path);
        if (parameters != null && !parameters.isEmpty()) {
            url.append("?");
            AtomicInteger count = new AtomicInteger(0);
            parameters.forEach((k, v) -> {
                url.append(k).append("=").append(v);
                int symbolCount = count.incrementAndGet();
                if (symbolCount < parameters.size()) {
                    url.append("&");
                }
            });
        }
        return url.toString();
    }
    
    @Override
    public String toString() {
        return this.getServiceKey("todo");
    }
}
