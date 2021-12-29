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

package org.apache.shenyu.client.grpc;

import com.google.common.collect.Lists;
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.core.constant.ShenyuClientConstants;
import org.apache.shenyu.client.core.exception.ShenyuClientIllegalArgumentException;
import org.apache.shenyu.client.core.register.BeanPostShenyuClientRegister;
import org.apache.shenyu.client.grpc.common.annotation.ShenyuGrpcClient;
import org.apache.shenyu.client.grpc.common.annotation.ShenyuGrpcClientDelegate;
import org.apache.shenyu.client.grpc.common.dto.GrpcExt;
import org.apache.shenyu.client.grpc.json.JsonServerServiceInterceptor;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * The type Shenyu grpc client bean post processor.
 */
public class GrpcClientBeanPostProcessor extends BeanPostShenyuClientRegister {
    
    private static final Logger LOG = LoggerFactory.getLogger(GrpcClientBeanPostProcessor.class);
    
    private final String ipAndPort;
    
    private List<ServerServiceDefinition> serviceDefinitions = Lists.newArrayList();
    
    /**
     * Instantiates a new Abstract shenyu client register.
     *
     * @param clientConfig                   the client config
     * @param shenyuClientRegisterRepository the shenyu client register repository
     */
    public GrpcClientBeanPostProcessor(final PropertiesConfig clientConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
        Properties props = clientConfig.getProps();
        ipAndPort = props.getProperty(ShenyuClientConstants.IP_PORT);
    }
    
    /**
     * Gets meta data dto.
     *
     * @param bean the object
     * @return the meta data dto
     */
    @Override
    public List<MetaDataRegisterDTO> getMetaDataDto(final Object bean) {
        if (bean instanceof BindableService) {
            exportJsonGenericService(bean);
            return handler(bean);
        }
        return Collections.emptyList();
    }
    
    /**
     * Gets register dto.
     *
     * @return the register dto
     */
    @Override
    public URIRegisterDTO getRegisterDto() {
        return buildURIRegisterDTO();
    }
    
    /**
     * Check param.
     */
    @Override
    public void checkParam() {
        if (StringUtils.isAnyBlank(this.getContextPath(), ipAndPort)) {
            throw new ShenyuClientIllegalArgumentException("grpc client must config the contextPath, ipAndPort");
        }
        
        if (this.getPort() == null) {
            throw new ShenyuClientIllegalArgumentException("grpc client must config the contextPath, ipAndPort");
        }
    }
    
    /**
     * Gets service definitions.
     *
     * @return the service definitions
     */
    public List<ServerServiceDefinition> getServiceDefinitions() {
        return this.serviceDefinitions;
    }
    
    private URIRegisterDTO buildURIRegisterDTO() {
        String host = this.getHost();
        return URIRegisterDTO.builder()
                .contextPath(this.getContextPath())
                .appName(this.ipAndPort)
                .rpcType(RpcTypeEnum.GRPC.getName())
                .host(host)
                .port(this.getPort())
                .build();
    }
    
    private List<MetaDataRegisterDTO> handler(final Object serviceBean) {
        Class<?> clazz;
        try {
            clazz = serviceBean.getClass();
        } catch (Exception e) {
            LOG.error("failed to get grpc target class", e);
            return Collections.emptyList();
        }
        Class<?> parent = clazz.getSuperclass();
        Class<?> classes = parent.getDeclaringClass();
        String packageName;
        try {
            String serviceName = ShenyuClientConstants.SERVICE_NAME;
            Field field = classes.getField(serviceName);
            field.setAccessible(true);
            packageName = field.get(null).toString();
        } catch (Exception e) {
            LOG.error(String.format("SERVICE_NAME field not found: %s", classes), e);
            return Collections.emptyList();
        }
        if (StringUtils.isEmpty(packageName)) {
            LOG.error(String.format("grpc SERVICE_NAME can not found: %s", classes));
            return Collections.emptyList();
        }
        final Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(clazz);
        return Arrays.stream(methods)
                .filter(this::isShenyuClientOrOwner)
                .map(method -> {
                    ShenyuGrpcClient shenyuGrpcClient = this.getAnnotation(method);
                    return this.buildMetaDataDTO(packageName, shenyuGrpcClient, method);
                }).collect(Collectors.toList());
    }
    
    private MetaDataRegisterDTO buildMetaDataDTO(final String packageName, final ShenyuGrpcClient shenyuGrpcClient, final Method method) {
        String methodName = method.getName();
        Class<?>[] parameterTypesClazz = method.getParameterTypes();
        String parameterTypes = Arrays.stream(parameterTypesClazz).map(Class::getName)
                .collect(Collectors.joining(","));
        MethodDescriptor.MethodType methodType = JsonServerServiceInterceptor.getMethodTypeMap().get(packageName + "/" + methodName);
        return this.universalMeta(shenyuGrpcClient, method)
                .appName(ipAndPort)
                .serviceName(packageName)
                .parameterTypes(parameterTypes)
                .rpcType(RpcTypeEnum.GRPC.getName())
                .rpcExt(buildRpcExt(shenyuGrpcClient, methodType)).build();
    }
    
    private String buildRpcExt(final ShenyuGrpcClient shenyuGrpcClient,
                               final MethodDescriptor.MethodType methodType) {
        GrpcExt build = GrpcExt.builder()
                .timeout(shenyuGrpcClient.timeout())
                .methodType(methodType)
                .build();
        return GsonUtils.getInstance().toJson(build);
    }
    
    private void exportJsonGenericService(final Object bean) {
        BindableService bindableService = (BindableService) bean;
        ServerServiceDefinition serviceDefinition = bindableService.bindService();
        
        try {
            ServerServiceDefinition jsonDefinition = JsonServerServiceInterceptor.useJsonMessages(serviceDefinition);
            serviceDefinitions.add(serviceDefinition);
            serviceDefinitions.add(jsonDefinition);
        } catch (Exception e) {
            LOG.error("export json generic service is fail", e);
        }
    }
    
    @Override
    protected ShenyuGrpcClient delegate(final ShenyuClient shenyuClient) {
        return new ShenyuGrpcClientDelegate(shenyuClient);
    }
    
    /**
     * Gets owner class.
     *
     * @return the owner class
     */
    @Override
    protected Class<ShenyuGrpcClient> getOwnerClass() {
        return ShenyuGrpcClient.class;
    }
}
