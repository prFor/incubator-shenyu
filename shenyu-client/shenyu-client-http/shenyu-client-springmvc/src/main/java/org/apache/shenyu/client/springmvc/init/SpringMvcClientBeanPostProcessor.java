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

package org.apache.shenyu.client.springmvc.init;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.core.constant.ShenyuClientConstants;
import org.apache.shenyu.client.core.exception.ShenyuClientIllegalArgumentException;
import org.apache.shenyu.client.core.register.BeanPostShenyuClientRegister;
import org.apache.shenyu.client.springmvc.annotation.ShenyuSpringMvcClient;
import org.apache.shenyu.client.springmvc.annotation.ShenyuSpringMvcClientDelegate;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation;

/**
 * SpringMvcClientBeanPostProcessor .
 */
public class SpringMvcClientBeanPostProcessor extends BeanPostShenyuClientRegister {
    
    private static final Logger LOG = LoggerFactory.getLogger(SpringMvcClientBeanPostProcessor.class);
    
    private static final String PATH_SEPARATOR = "/";
    
    private final Boolean isFull;
    
    private final List<Class<? extends Annotation>> mappingAnnotation = new ArrayList<>(7);
    
    private final String[] pathAttributeNames = new String[]{"path", "value"};
    
    private final String protocol;
    
    private boolean isFullTrue;
    
    /**
     * Instantiates a new Abstract shenyu client register.
     *
     * @param clientConfig                   the client config
     * @param shenyuClientRegisterRepository the shenyu client register repository
     */
    public SpringMvcClientBeanPostProcessor(final PropertiesConfig clientConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
        this.isFull = Boolean.parseBoolean(clientConfig.getProps().getProperty(ShenyuClientConstants.IS_FULL, Boolean.FALSE.toString()));
        mappingAnnotation.add(ShenyuSpringMvcClient.class);
        mappingAnnotation.add(ShenyuClient.class);
        mappingAnnotation.add(PostMapping.class);
        mappingAnnotation.add(GetMapping.class);
        mappingAnnotation.add(DeleteMapping.class);
        mappingAnnotation.add(PutMapping.class);
        mappingAnnotation.add(RequestMapping.class);
        this.protocol = clientConfig.getProps().getProperty(ShenyuClientConstants.PROTOCOL, ShenyuClientConstants.HTTP);
    }
    
    @Override
    public void checkParam() {
        if (StringUtils.isBlank(this.getAppName()) && StringUtils.isBlank(this.getContextPath())) {
            String errorMsg = "http register param must config the appName or contextPath";
            LOG.error(errorMsg);
            throw new ShenyuClientIllegalArgumentException(errorMsg);
        }
        if (this.getPort() == null || this.getPort() <= 0) {
            String errorMsg = "http register param must config the port must > 0";
            LOG.error(errorMsg);
            throw new ShenyuClientIllegalArgumentException(errorMsg);
        }
    }
    
    @Override
    public List<MetaDataRegisterDTO> getMetaDataDto(final Object bean) {
        if (Boolean.TRUE.equals(isFull) || !hasAnnotation(bean.getClass(), Controller.class)) {
            return buildMetaDataDTO();
        }
        ShenyuSpringMvcClient beanShenyuClient = this.getAnnotation(bean.getClass());
        final String superPath = buildApiSuperPath(bean.getClass());
        // Compatible with previous versions
        if (Objects.nonNull(beanShenyuClient) && superPath.contains("*")) {
            return Collections.singletonList(buildMetaDataDTO(beanShenyuClient, pathJoin(this.getContextPath(), superPath)));
        }
        final Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(bean.getClass());
        List<MetaDataRegisterDTO> metaDataRegisters = new ArrayList<>();
        for (Method method : methods) {
            ShenyuSpringMvcClient methodShenyuClient = this.getAnnotation(method);
            methodShenyuClient = Objects.isNull(methodShenyuClient) ? beanShenyuClient : methodShenyuClient;
            if (Objects.nonNull(methodShenyuClient)) {
                metaDataRegisters.add(buildMetaDataDTO(methodShenyuClient, buildApiPath(method, superPath)));
            }
        }
        return metaDataRegisters;
    }
    
    private String buildApiPath(@NonNull final Method method, @NonNull final String superPath) {
        ShenyuSpringMvcClient shenyuSpringMvcClient = this.getAnnotation(method);
        if (Objects.nonNull(shenyuSpringMvcClient) && StringUtils.isNotBlank(shenyuSpringMvcClient.path())) {
            return pathJoin(this.getContextPath(), superPath, shenyuSpringMvcClient.path());
        }
        final String path = getPathByMethod(method);
        if (StringUtils.isNotBlank(path)) {
            return pathJoin(this.getContextPath(), superPath, path);
        }
        return pathJoin(this.getContextPath(), superPath);
    }
    
    private String getPathByMethod(@NonNull final Method method) {
        for (Class<? extends Annotation> mapping : mappingAnnotation) {
            final String pathByAnnotation = getPathByAnnotation(AnnotationUtils.findAnnotation(method, mapping), pathAttributeNames);
            if (StringUtils.isNotBlank(pathByAnnotation)) {
                return pathByAnnotation;
            }
        }
        return null;
    }
    
    private String getPathByAnnotation(@Nullable final Annotation annotation, @NonNull final String... pathAttributeName) {
        if (Objects.isNull(annotation)) {
            return null;
        }
        for (String s : pathAttributeName) {
            final Object value = AnnotationUtils.getValue(annotation, s);
            if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                return (String) value;
            }
            // Only the first path is supported temporarily
            if (value instanceof String[] && ArrayUtils.isNotEmpty((String[]) value) && StringUtils.isNotBlank(((String[]) value)[0])) {
                return ((String[]) value)[0];
            }
        }
        return null;
    }
    
    private String pathJoin(@NonNull final String... path) {
        StringBuilder result = new StringBuilder(PATH_SEPARATOR);
        for (String p : path) {
            if (!result.toString().endsWith(PATH_SEPARATOR)) {
                result.append(PATH_SEPARATOR);
            }
            result.append(p.startsWith(PATH_SEPARATOR) ? p.replaceFirst(PATH_SEPARATOR, "") : p);
        }
        return result.toString();
    }
    
    private String buildApiSuperPath(@NonNull final Class<?> method) {
        ShenyuSpringMvcClient shenyuSpringMvcClient = this.getAnnotation(method);
        if (Objects.nonNull(shenyuSpringMvcClient) && StringUtils.isNotBlank(shenyuSpringMvcClient.path())) {
            return shenyuSpringMvcClient.path();
        }
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        // Only the first path is supported temporarily
        if (Objects.nonNull(requestMapping) && ArrayUtils.isNotEmpty(requestMapping.path()) && StringUtils.isNotBlank(requestMapping.path()[0])) {
            return requestMapping.path()[0];
        }
        return "";
    }
    
    private MetaDataRegisterDTO buildMetaDataDTO(@NonNull final ShenyuSpringMvcClient shenyuSpringMvcClient, final String path) {
        MetaDataRegisterDTO.Builder builder = this.universalMeta(shenyuSpringMvcClient);
        return builder.rpcType(RpcTypeEnum.HTTP.getName())
                .path(path)
                .ruleName(StringUtils.defaultIfBlank(shenyuSpringMvcClient.ruleName(), path))
                .registerMetaData(shenyuSpringMvcClient.registerMetaData()).build();
    }
    
    private List<MetaDataRegisterDTO> buildMetaDataDTO() {
        if (isFullTrue) {
            return Collections.emptyList();
        }
        isFullTrue = true;
        MetaDataRegisterDTO.Builder builder = this.universalMeta();
        MetaDataRegisterDTO registerDTO = builder.rpcExt(RpcTypeEnum.HTTP.getName())
                .enabled(true)
                .build();
        return Collections.singletonList(registerDTO);
    }
    
    @Override
    public URIRegisterDTO getRegisterDto() {
        return buildURIRegisterDTO();
    }
    
    @Override
    protected ShenyuSpringMvcClient delegate(final ShenyuClient shenyuClient) {
        return new ShenyuSpringMvcClientDelegate(shenyuClient);
    }
    
    @Override
    protected Class<ShenyuSpringMvcClient> getOwnerClass() {
        return ShenyuSpringMvcClient.class;
    }
    
    private URIRegisterDTO buildURIRegisterDTO() {
        return URIRegisterDTO.builder()
                .contextPath(this.getContextPath())
                .appName(getAppName())
                .protocol(protocol)
                .host(this.getHost())
                .port(this.getPort())
                .rpcType(RpcTypeEnum.HTTP.getName())
                .build();
    }
}
