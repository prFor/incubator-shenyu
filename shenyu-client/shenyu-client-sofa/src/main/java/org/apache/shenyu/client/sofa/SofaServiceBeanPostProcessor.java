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

package org.apache.shenyu.client.sofa;

import com.alipay.sofa.runtime.service.component.Service;
import com.alipay.sofa.runtime.spring.factory.ServiceFactoryBean;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.core.constant.ShenyuClientConstants;
import org.apache.shenyu.client.core.exception.ShenyuClientIllegalArgumentException;
import org.apache.shenyu.client.core.disruptor.ShenyuClientRegisterEventPublisher;
import org.apache.shenyu.client.core.register.BeanPostShenyuClientRegister;
import org.apache.shenyu.client.sofa.common.annotation.ShenyuSofaClient;
import org.apache.shenyu.client.sofa.common.annotation.ShenyuSofaClientDelegate;
import org.apache.shenyu.client.sofa.common.dto.SofaRpcExt;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.common.utils.IpUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The Sofa ServiceBean PostProcessor.
 */
public class SofaServiceBeanPostProcessor extends BeanPostShenyuClientRegister {
    
    private static final Logger LOG = LoggerFactory.getLogger(SofaServiceBeanPostProcessor.class);
    
    public SofaServiceBeanPostProcessor(final PropertiesConfig clientConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
    }
    
    private List<MetaDataRegisterDTO> handler(final ServiceFactoryBean serviceBean) {
        Class<?> clazz;
        Object targetProxy;
        try {
            targetProxy = ((Service) Objects.requireNonNull(serviceBean.getObject())).getTarget();
            clazz = targetProxy.getClass();
        } catch (Exception e) {
            LOG.error("failed to get sofa target class", e);
            return Collections.emptyList();
        }
        if (AopUtils.isAopProxy(targetProxy)) {
            clazz = AopUtils.getTargetClass(targetProxy);
        }
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(clazz);
        return Arrays.stream(methods)
                .filter(this::isShenyuClientOrOwner)
                .map(method -> buildMetaDataDTO(serviceBean,
                        this.getAnnotation(method), method)).collect(Collectors.toList());
    }
    
    private MetaDataRegisterDTO buildMetaDataDTO(final ServiceFactoryBean serviceBean, final ShenyuSofaClient shenyuSofaClient, final Method method) {
//        String appName = this.getAppName();
//        String path = this.getContextPath() + shenyuSofaClient.path();
//        String desc = shenyuSofaClient.desc();
//        String serviceName = serviceBean.getInterfaceClass().getName();
//        String host = this.getHost();
//        int port = this.getPort();
        String configRuleName = shenyuSofaClient.ruleName();
        String ruleName = ("".equals(configRuleName)) ? path : configRuleName;
        String methodName = method.getName();
        String parameterTypes = Arrays.stream(method.getParameters())
                .map(parameter -> {
                    StringBuilder result = new StringBuilder(parameter.getType().getName());
                    final Type type = parameter.getParameterizedType();
                    if (type instanceof ParameterizedType) {
                        final Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                        for (Type actualTypeArgument : actualTypeArguments) {
                            result.append("#").append(actualTypeArgument.getTypeName());
                        }
                    }
                    return result.toString();
                }).collect(Collectors.joining(","));
        return MetaDataRegisterDTO.builder()
                .appName(appName)
                .serviceName(serviceName)
                .methodName(methodName)
                .contextPath(this.getContextPath())
                .host(host)
                .port(port)
                .path(path)
                .ruleName(ruleName)
                .pathDesc(desc)
                .parameterTypes(parameterTypes)
                .rpcType(RpcTypeEnum.SOFA.getName())
                .rpcExt(buildRpcExt(shenyuSofaClient))
                .enabled(shenyuSofaClient.enabled())
                .build();
    }
    
    private String buildRpcExt(final ShenyuSofaClient shenyuSofaClient) {
        SofaRpcExt build = SofaRpcExt.builder()
                .loadbalance(shenyuSofaClient.loadBalance())
                .retries(shenyuSofaClient.retries())
                .timeout(shenyuSofaClient.timeout())
                .build();
        return GsonUtils.getInstance().toJson(build);
    }
    
    /**
     * Check param.
     */
    @Override
    public void checkParam() {
        if (StringUtils.isEmpty(this.getContextPath())) {
            throw new ShenyuClientIllegalArgumentException("sofa client must config the contextPath");
        }
    }
    
    /**
     * Gets meta data dto.
     *
     * @param bean the object
     * @return the meta data dto
     */
    @Override
    public List<MetaDataRegisterDTO> getMetaDataDto(final Object bean) {
        if (bean instanceof ServiceFactoryBean) {
            return handler((ServiceFactoryBean) bean);
        }
        return Collections.emptyList();
    }
    
    /**
     * Delegation t.
     *
     * @param client the client
     * @return the t
     */
    @Override
    protected ShenyuSofaClient delegate(final ShenyuClient client) {
        return new ShenyuSofaClientDelegate(client);
    }
    
    /**
     * Gets owner class.
     *
     * @return the owner class
     */
    @Override
    protected Class<ShenyuSofaClient> getOwnerClass() {
        return ShenyuSofaClient.class;
    }
    
    /**
     * Gets register dto.
     *
     * @return the register dto，It may return null,
     * and some services do not need to register the URIRegisterDTO object.
     */
    @Override
    public URIRegisterDTO getRegisterDto() {
        return null;
    }
}
