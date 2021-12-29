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

import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.core.exception.ShenyuClientIllegalArgumentException;
import org.apache.shenyu.client.core.register.RefreshedShenyuClientRegister;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClientDelegate;
import org.apache.shenyu.client.dubbo.common.dto.DubboRpcExt;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DubboServiceBeanListener .
 */
public abstract class DubboServiceBeanListener extends RefreshedShenyuClientRegister {
    
    /**
     * Instantiates a new Abstract shenyu client register.
     *
     * @param clientConfig                   the client config
     * @param shenyuClientRegisterRepository the shenyu client register repository
     */
    public DubboServiceBeanListener(final PropertiesConfig clientConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
    }
    
    /**
     * Build uri register dto uri register dto.
     *
     * @param serviceBean the service bean
     * @return the uri register dto
     */
    protected URIRegisterDTO buildURIRegisterDTO(final ServiceBeanData serviceBean) {
        return URIRegisterDTO.builder()
                .contextPath(this.getContextPath())
                .appName(this.getAppName(serviceBean.getAppName()))
                .rpcType(RpcTypeEnum.DUBBO.getName())
                .host(this.getHost())
                .port(this.getPort(serviceBean.getPort()))
                .build();
    }
    
    /**
     * Check param.
     */
    @Override
    public void checkParam() {
        if (StringUtils.isAnyBlank(this.getContextPath(), this.getAppName())) {
            throw new ShenyuClientIllegalArgumentException("apache dubbo client must config the contextPath or appName");
        }
    }
    
    /**
     * Build meta data dto list.
     *
     * @param serviceBean the service bean
     * @param clazz       the clazz
     * @return the list
     */
    protected List<MetaDataRegisterDTO> buildMetaDataDTO(final Object serviceBean, final Class<?> clazz) {
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(clazz);
        return Arrays.stream(methods)
                .filter(this::isShenyuClientOrOwner)
                .map(method -> buildMetaDataDTO(new ServiceBeanData(serviceBean), this.getAnnotation(method), method)).collect(Collectors.toList());
    }
    
    private MetaDataRegisterDTO buildMetaDataDTO(final ServiceBeanData serviceBean, final ShenyuDubboClient shenyuDubboClient, final Method method) {
        String serviceName = serviceBean.getInterface();
        Class<?>[] parameterTypesClazz = method.getParameterTypes();
        String parameterTypes = Arrays.stream(parameterTypesClazz).map(Class::getName).collect(Collectors.joining(","));
        return this.universalMeta(shenyuDubboClient, method)
                .serviceName(serviceName)
                .port(getPort(serviceBean.getPort()))
                .parameterTypes(parameterTypes)
                .rpcType(RpcTypeEnum.DUBBO.getName())
                .rpcExt(buildRpcExt(serviceBean)).build();
    }
    
    private String buildRpcExt(final ServiceBeanData serviceBean) {
        DubboRpcExt build = DubboRpcExt.builder()
                .group(serviceBean.getGroup())
                .version(serviceBean.getVersion())
                .loadbalance(serviceBean.getLoadbalance())
                .retries(serviceBean.getRetries())
                .timeout(serviceBean.getTimeout())
                .sent(serviceBean.getSent())
                .cluster(serviceBean.getCluster())
                .url("")
                .build();
        return GsonUtils.getInstance().toJson(build);
    }
    
    /**
     * Delegation t.
     *
     * @param shenyuClient the client
     * @return the t
     */
    @Override
    @SuppressWarnings("all")
    protected ShenyuDubboClient delegate(final ShenyuClient shenyuClient) {
        return new ShenyuDubboClientDelegate(shenyuClient);
    }
    
    /**
     * Gets owner class.
     *
     * @return the owner class
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Class<ShenyuDubboClient> getOwnerClass() {
        return ShenyuDubboClient.class;
    }
}
