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

package org.apache.shenyu.client.motan;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.weibo.api.motan.config.springsupport.BasicServiceConfigBean;
import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.core.constant.ShenyuClientConstants;
import org.apache.shenyu.client.core.exception.ShenyuClientIllegalArgumentException;
import org.apache.shenyu.client.core.disruptor.ShenyuClientRegisterEventPublisher;
import org.apache.shenyu.client.core.register.BeanPostShenyuClientRegister;
import org.apache.shenyu.client.motan.common.annotation.ShenyuMotanClient;
import org.apache.shenyu.client.motan.common.annotation.ShenyuMotanClientDelegate;
import org.apache.shenyu.client.motan.common.dto.MotanRpcExt;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.common.utils.IpUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Motan BeanPostProcessor.
 */
public class MotanServiceBeanPostProcessor extends BeanPostShenyuClientRegister {
    
    private static final String BASE_SERVICE_CONFIG = "baseServiceConfig";
    
    private final LocalVariableTableParameterNameDiscoverer localVariableTableParameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
    
    private final ExecutorService executorService;
    
    private String group;
    
    
    public MotanServiceBeanPostProcessor(final PropertiesConfig clientConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
        executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("shenyu-motan-client-thread-pool-%d").build());
    }
    
    private void handler(final Object bean) {
        if (group == null) {
            group = ((BasicServiceConfigBean) this.getApplicationContext().getBean(BASE_SERVICE_CONFIG)).getGroup();
        }
        Class<?> clazz = bean.getClass();
        if (AopUtils.isAopProxy(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(clazz);
        MotanService service = clazz.getAnnotation(MotanService.class);
        for (Method method : methods) {
            ShenyuMotanClient shenyuMotanClient = method.getAnnotation(ShenyuMotanClient.class);
            if (Objects.nonNull(shenyuMotanClient)) {
                publisher.publishEvent(buildMetaDataDTO(clazz, service,
                        shenyuMotanClient, method, buildRpcExt(methods)));
            }
        }
    }
    
    private MetaDataRegisterDTO buildMetaDataDTO(final Class<?> clazz, final MotanService service,
                                                 final ShenyuMotanClient shenyuMotanClient, final Method method, final String rpcExt) {
        String appName = this.appName;
        String path = this.contextPath + shenyuMotanClient.path();
        String desc = shenyuMotanClient.desc();
        String host = IpUtils.isCompleteHost(this.host) ? this.host : IpUtils.getHost(this.host);
        int port = StringUtils.isBlank(this.port) ? -1 : Integer.parseInt(this.port);
        String configRuleName = shenyuMotanClient.ruleName();
        String ruleName = ("".equals(configRuleName)) ? path : configRuleName;
        String methodName = method.getName();
        Class<?>[] parameterTypesClazz = method.getParameterTypes();
        String parameterTypes = Arrays.stream(parameterTypesClazz).map(Class::getName)
                .collect(Collectors.joining(","));
        String serviceName;
        if (void.class.equals(service.interfaceClass())) {
            if (clazz.getInterfaces().length > 0) {
                serviceName = clazz.getInterfaces()[0].getName();
            } else {
                throw new ShenyuClientIllegalArgumentException("Failed to export remote service class " + clazz.getName()
                        + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
            }
        } else {
            serviceName = service.interfaceClass().getName();
        }
        return MetaDataRegisterDTO.builder()
                .appName(appName)
                .serviceName(serviceName)
                .methodName(methodName)
                .contextPath(this.contextPath)
                .path(path)
                .port(port)
                .host(host)
                .ruleName(ruleName)
                .pathDesc(desc)
                .parameterTypes(parameterTypes)
                .rpcType(RpcTypeEnum.MOTAN.getName())
                .rpcExt(rpcExt)
                .enabled(shenyuMotanClient.enabled())
                .build();
    }
    
    private MotanRpcExt.RpcExt buildRpcExt(final Method method) {
        String[] paramNames = localVariableTableParameterNameDiscoverer.getParameterNames(method);
        List<Pair<String, String>> params = new ArrayList<>();
        if (paramNames != null && paramNames.length > 0) {
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramNames.length; i++) {
                params.add(Pair.of(paramTypes[i].getName(), paramNames[i]));
            }
        }
        return new MotanRpcExt.RpcExt(method.getName(), params);
    }
    
    private String buildRpcExt(final Method[] methods) {
        List<MotanRpcExt.RpcExt> list = new ArrayList<>();
        for (Method method : methods) {
            ShenyuMotanClient shenyuMotanClient = method.getAnnotation(ShenyuMotanClient.class);
            if (Objects.nonNull(shenyuMotanClient)) {
                list.add(buildRpcExt(method));
            }
        }
        MotanRpcExt buildList = new MotanRpcExt(list, group);
        return GsonUtils.getInstance().toJson(buildList);
    }
    
    /**
     * Check param.
     */
    @Override
    public void checkParam() {
        if (StringUtils.isEmpty(this.getContextPath())) {
            throw new ShenyuClientIllegalArgumentException("motan client must config the contextPath");
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
        Class<?> clazz = bean.getClass();
        if (AopUtils.isAopProxy(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        MotanService service = clazz.getAnnotation(MotanService.class);
        if (service != null) {
            executorService.execute(() -> handler(bean));
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
    protected ShenyuMotanClient delegate(final ShenyuClient client) {
        return new ShenyuMotanClientDelegate(client);
    }
    
    /**
     * Gets owner class.
     *
     * @return the owner class
     */
    @Override
    protected Class<ShenyuMotanClient> getOwnerClass() {
        return ShenyuMotanClient.class;
    }
    
    /**
     * Gets register dto.
     *
     * @return the register dto
     */
    @Override
    public URIRegisterDTO getRegisterDto() {
        return null;
    }
}
