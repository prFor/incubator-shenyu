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

package org.apache.shenyu.client.dubbo.common;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * ServiceData .
 */
public class ServiceData {
    
    
    private final Object serverConfig;
    
    /**
     * Instantiates a new Service data.
     *
     * @param serverConfig the server config
     */
    public ServiceData(final Object serverConfig) {
        this.serverConfig = serverConfig;
    }
    
    /**
     * Gets app name.
     * dubbo: ServiceConfig.getApplication().name()
     *
     * @return the app name
     */
    public String getAppName() {
        Object application = this.getProperty(this.serverConfig, "getApplication");
        if (application == null) {
            return null;
        }
        String name = this.getProperty(application, "getName");
        return Optional.ofNullable(name).orElse("");
    }
    
    /**
     * Gets interface.
     * dubbo: ServiceConfig.interfaceName.
     *
     * @return the interface
     */
    public String getInterface() {
        return this.getProperty("getInterface");
    }
    
    /**
     * Gets port.
     * dubbo: ServiceConfig.protocols[0].port.
     *
     * @return the port
     */
    public Integer getPort() {
        Object protocol = this.getProperty("getProtocol");
        if (protocol == null) {
            return 0;
        }
        Integer port = this.getProperty(protocol, "getPort");
        return Optional.ofNullable(port).orElse(0);
    }
    
    /**
     * Group string.
     * dubbo: ServiceConfig.group.
     *
     * @return the string
     */
    public String getGroup() {
        String group = this.getProperty("getGroup");
        return Optional.ofNullable(group).orElse("");
    }
    
    /**
     * Gets version.
     * dubbo: ServiceConfig.version.
     *
     * @return the version
     */
    public String getVersion() {
        String version = this.getProperty("getVersion");
        return Optional.ofNullable(version).orElse("");
    }
    
    /**
     * Gets loadbalance.
     * dubbo: ServiceConfig.loadbalance.
     *
     * @return the loadbalance
     */
    public String getLoadbalance() {
        String loadbalance = this.getProperty("getLoadbalance");
        return Optional.ofNullable(loadbalance).orElse("random");
    }
    
    /**
     * Gets retries.
     * dubbo: ServiceConfig.retries.
     *
     * @return the retries
     */
    public Integer getRetries() {
        Integer retries = this.getProperty("getRetries");
        return Optional.ofNullable(retries).orElse(2);
    }
    
    /**
     * Gets timeout.
     * dubbo: ServiceConfig.timeout.
     *
     * @return the timeout
     */
    public Integer getTimeout() {
        Integer timeout = this.getProperty("getTimeout");
        return Optional.ofNullable(timeout).orElse(3000);
    }
    
    /**
     * Gets sent.
     * dubbo: ServiceConfig.sent.
     *
     * @return the sent
     */
    public Boolean getSent() {
        Boolean sent = this.getProperty("getSent");
        return Optional.ofNullable(sent).orElse(false);
    }
    
    /**
     * Gets cluster.
     * dubbo: ServiceConfig.cluster.
     *
     * @return the cluster
     */
    public String getCluster() {
        //failover
        String cluster = this.getProperty("getCluster");
        return Optional.ofNullable(cluster).orElse("failover");
    }
    
    
    @SuppressWarnings("all")
    private <T> T getProperty(Object obj, String property) {
        Method method = ReflectionUtils.findMethod(obj.getClass(), property);
        try {
            return (T) method.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
    
    private <T> T getProperty(String property) {
        return this.getProperty(this.serverConfig, property);
    }
}
