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

package org.apache.shenyu.client.tars;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.core.register.BeanPostShenyuClientRegister;
import org.apache.shenyu.client.tars.common.annotation.ShenyuTarsClient;
import org.apache.shenyu.client.tars.common.annotation.ShenyuTarsClientDelegate;
import org.apache.shenyu.client.tars.common.annotation.ShenyuTarsService;
import org.apache.shenyu.client.tars.common.dto.TarsRpcExt;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The Tars ServiceBean PostProcessor.
 */
public class TarsServiceBeanPostProcessor extends BeanPostShenyuClientRegister {
    
    private final LocalVariableTableParameterNameDiscoverer localVariableTableParameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
    
    private final String ipAndPort;
    
    public TarsServiceBeanPostProcessor(final PropertiesConfig clientConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
        this.ipAndPort = this.getHost() + ":" + this.getPort();
    }
    
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        if (bean.getClass().getAnnotation(ShenyuTarsService.class) != null) {
            handler(bean);
        }
        return bean;
    }
    
    /**
     * Gets meta data dto.
     *
     * @param bean the object
     * @return the meta data dto
     */
    @Override
    public List<MetaDataRegisterDTO> getMetaDataDto(final Object bean) {
        if (bean.getClass().getAnnotation(ShenyuTarsService.class) != null) {
            return handler(bean);
        }
        return Collections.emptyList();
    }
    
    private List<MetaDataRegisterDTO> handler(final Object serviceBean) {
        Class<?> clazz = serviceBean.getClass();
        if (AopUtils.isAopProxy(serviceBean)) {
            clazz = AopUtils.getTargetClass(serviceBean);
        }
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(clazz);
        final String serviceName = serviceBean.getClass().getAnnotation(ShenyuTarsService.class).serviceName();
        return Arrays.stream(methods)
                .filter(this::isShenyuClientOrOwner)
                .map(method -> buildMetaDataDTO(serviceName, this.getAnnotation(method), method, buildRpcExt(methods))).collect(Collectors.toList());
    }
    
    private MetaDataRegisterDTO buildMetaDataDTO(final String serviceName, final ShenyuTarsClient shenyuTarsClient, final Method method, final String rpcExt) {
        MetaDataRegisterDTO.Builder builder = this.universalMeta(shenyuTarsClient, method);
        Class<?>[] parameterTypesClazz = method.getParameterTypes();
        String parameterTypes = Arrays.stream(parameterTypesClazz).map(Class::getName)
                .collect(Collectors.joining(","));
        return builder.appName(ipAndPort)
                .serviceName(serviceName)
                .parameterTypes(parameterTypes)
                .rpcType(RpcTypeEnum.TARS.getName())
                .rpcExt(rpcExt).build();
    }
    
    private TarsRpcExt.RpcExt buildRpcExt(final Method method) {
        String[] paramNames = localVariableTableParameterNameDiscoverer.getParameterNames(method);
        List<Pair<String, String>> params = new ArrayList<>();
        if (paramNames != null && paramNames.length > 0) {
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramNames.length; i++) {
                params.add(Pair.of(paramTypes[i].getName(), paramNames[i]));
            }
        }
        return new TarsRpcExt.RpcExt(method.getName(), params, method.getReturnType().getName());
    }
    
    private String buildRpcExt(final Method[] methods) {
        List<TarsRpcExt.RpcExt> list = new ArrayList<>();
        for (Method method : methods) {
            ShenyuTarsClient shenyuSofaClient = this.getAnnotation(method);
            if (Objects.nonNull(shenyuSofaClient)) {
                list.add(buildRpcExt(method));
            }
        }
        TarsRpcExt buildList = new TarsRpcExt(list);
        return GsonUtils.getInstance().toJson(buildList);
    }
    
    /**
     * Delegation t.
     *
     * @param client the client
     * @return the t
     */
    @Override
    protected ShenyuTarsClient delegate(final ShenyuClient client) {
        return new ShenyuTarsClientDelegate(client);
    }
    
    /**
     * Gets owner class.
     *
     * @return the owner class
     */
    @Override
    protected Class<ShenyuTarsClient> getOwnerClass() {
        return ShenyuTarsClient.class;
    }
    
    /**
     * Gets register dto.
     *
     * @return the register dto，It may return null,
     * and some services do not need to register the URIRegisterDTO object.
     */
    @Override
    public URIRegisterDTO getRegisterDto() {
        return URIRegisterDTO.builder()
                .contextPath(this.getContextPath())
                .appName(this.ipAndPort)
                .rpcType(RpcTypeEnum.TARS.getName())
                .host(this.getHost())
                .port(this.getPort())
                .build();
    }
}
