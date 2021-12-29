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

package org.apache.shenyu.client.core.register;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.core.constant.ShenyuClientConstants;
import org.apache.shenyu.client.core.disruptor.ShenyuClientRegisterEventPublisher;
import org.apache.shenyu.common.utils.IpUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AbstractShenyuClientRegister .
 */
public abstract class AbstractShenyuClientRegister implements ShenyuClientRegister {
    
    private final AtomicBoolean registered = new AtomicBoolean(false);
    
    private final ShenyuClientRegisterEventPublisher publisher = ShenyuClientRegisterEventPublisher.getInstance();
    
    private String contextPath;
    
    private String appName;
    
    private String host;
    
    private String port;
    
    /**
     * Instantiates a new Abstract shenyu client register.
     *
     * @param propertiesConfig               the properties config
     * @param shenyuClientRegisterRepository the shenyu client register repository
     */
    public AbstractShenyuClientRegister(final PropertiesConfig propertiesConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        Properties props = propertiesConfig.getProps();
        String contextPath = props.getProperty(ShenyuClientConstants.CONTEXT_PATH);
        String appName = props.getProperty(ShenyuClientConstants.APP_NAME);
        setContextPath(contextPath);
        setAppName(appName);
        setHost(props.getProperty(ShenyuClientConstants.HOST));
        setPort(props.getProperty(ShenyuClientConstants.PORT));
        publisher.start(shenyuClientRegisterRepository);
    }
    
    /**
     * Sets context path.
     *
     * @param contextPath the context path
     */
    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }
    
    /**
     * Sets app name.
     *
     * @param appName the app name
     */
    public void setAppName(final String appName) {
        this.appName = appName;
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
     * Sets port.
     *
     * @param port the port
     */
    public void setPort(final String port) {
        this.port = port;
    }
    
    /**
     * Gets context path.
     *
     * @return the context path
     */
    public String getContextPath() {
        return contextPath;
    }
    
    /**
     * Gets app name.
     *
     * @return the app name
     */
    public String getAppName() {
        return appName;
    }
    
    /**
     * Gets app name.
     *
     * @param defaultValue the default value
     * @return the app name
     */
    public String getAppName(final String defaultValue) {
        return StringUtils.isBlank(this.appName) ? defaultValue : this.appName;
    }
    
    /**
     * Gets host.
     *
     * @return the host
     */
    public String getHost() {
        return IpUtils.isCompleteHost(this.host) ? this.host : IpUtils.getHost(this.host);
    }
    
    /**
     * Gets port.
     *
     * @return the port
     */
    public Integer getPort() {
        return StringUtils.isBlank(this.port) ? null : Integer.parseInt(this.port);
    }
    
    /**
     * Gets port.
     *
     * @param defaultValue the default value
     * @return the port
     */
    public Integer getPort(final int defaultValue) {
        return StringUtils.isBlank(this.port) ? defaultValue : Integer.parseInt(this.port);
    }
    
    /**
     * If your path is empty, take the name of the method.
     * (path is null or empty) return method.getName().
     *
     * @param method the method
     * @param path   the path
     * @return the string (path is null or empty) return method.getName();
     */
    public String formatPath(Method method, String path) {
        return StringUtils.defaultIfBlank(path, "/" + method.getName());
    }
    
    @Override
    public void registerMetaData(final Object object) {
        this.checkParam();
        List<MetaDataRegisterDTO> metas = this.getMetaDataDto(object);
        if (metas != null && !metas.isEmpty()) {
            metas.forEach(this::publishEvent);
        }
    }
    
    /**
     * Register.
     */
    @Override
    public void registerService() {
        URIRegisterDTO registerDto = this.getRegisterDto();
        if (registerDto != null) {
            this.publishEvent(registerDto);
        }
    }
    
    /**
     * Publish event.
     *
     * @param <T> the type parameter
     * @param dto the dto
     */
    <T> void publishEvent(final T dto) {
        this.publisher.publishEvent(dto);
    }
    
    /**
     * Is registered boolean.
     *
     * @return the boolean
     */
    boolean isRegistered() {
        return registered.compareAndSet(false, true);
    }
    
    /**
     * Universal meta meta data register dto . builder.
     * appName=getAppName
     * host=getHost
     * port=getPort(-1)
     * methodName=method.name()
     * pathDesc=annotation.desc()
     * ruleName=annotation.ruleName()
     * enable=annotation.enable()
     * path=getContextPath()+annotation.path()
     * contextPath=getContextPath()
     *
     * @param annotation the shenyu client delegate
     * @param method     the method
     * @return the meta data register dto . builder
     */
    protected MetaDataRegisterDTO.Builder universalMeta(final Annotation annotation, final Method method) {
        String dlPath = this.getContextPath();
        String desc = "";
        boolean enable = false;
        String ruleName = "";
        if (annotation != null) {
            try {
                dlPath = (String) MethodUtils.invokeMethod(annotation, "path");
                desc = (String) MethodUtils.invokeMethod(annotation, "desc");
                ruleName = (String) MethodUtils.invokeMethod(annotation, "ruleName");
                enable = (Boolean) MethodUtils.invokeMethod(annotation, "enabled");
            } catch (Exception ignored) {
                //There should be no exception.
            }
        }
        String path = dlPath;
        String methodName = null;
        if (method != null) {
            path = this.getContextPath() + formatPath(method, dlPath);
            methodName = method.getName();
        }
        ruleName = formatRuleName(ruleName, path);
        return MetaDataRegisterDTO.builder()
                .appName(this.getAppName())
                .host(this.getHost())
                .port(this.getPort(-1))
                .methodName(methodName)
                .pathDesc(desc)
                .path(path)
                .enabled(enable)
                .ruleName(ruleName)
                .contextPath(this.getContextPath());
    }
    
    protected MetaDataRegisterDTO.Builder universalMeta(final Annotation annotation) {
        return this.universalMeta(annotation, null);
    }
    
    protected MetaDataRegisterDTO.Builder universalMeta() {
        return this.universalMeta(null, null);
    }
    
    
    private String formatRuleName(String ruleName, String path) {
        return StringUtils.defaultIfBlank(ruleName, path);
    }
    
    /**
     * Delegation t.
     *
     * @param <T>    the type parameter
     * @param client the client
     * @return the t
     */
    protected abstract <T extends Annotation> T delegate(ShenyuClient client);
    
    /**
     * Gets owner class.
     *
     * @param <T> the type parameter
     * @return the owner class
     */
    protected abstract <T extends Annotation> Class<T> getOwnerClass();
    
    /**
     * Check param.
     */
    public void checkParam() {
    }
}
