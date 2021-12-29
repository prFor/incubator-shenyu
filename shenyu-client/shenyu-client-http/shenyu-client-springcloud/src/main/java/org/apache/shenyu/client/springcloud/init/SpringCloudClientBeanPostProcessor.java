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

package org.apache.shenyu.client.springcloud.init;

import org.apache.commons.lang3.StringUtils;
import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.core.constant.ShenyuClientConstants;
import org.apache.shenyu.client.core.exception.ShenyuClientIllegalArgumentException;
import org.apache.shenyu.client.core.register.BeanPostShenyuClientRegister;
import org.apache.shenyu.client.springcloud.annotation.ShenyuSpringCloudClient;
import org.apache.shenyu.client.springcloud.annotation.ShenyuSpringCloudClientDelegate;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SpringCloudClientBeanPostProcessorEx .
 */
public class SpringCloudClientBeanPostProcessor extends BeanPostShenyuClientRegister {
    
    private static final Logger LOG = LoggerFactory.getLogger(SpringCloudClientBeanPostProcessor.class);
    
    private final String servletContextPath;
    
    private final Boolean isFull;
    
    private final Environment env;
    
    private boolean isFullTrue;
    
    /**
     * Instantiates a new Abstract shenyu client register.
     *
     * @param clientConfig                   the client config
     * @param shenyuClientRegisterRepository the shenyu client register repository
     * @param env                            the env
     */
    public SpringCloudClientBeanPostProcessor(final PropertiesConfig clientConfig,
                                              final ShenyuClientRegisterRepository shenyuClientRegisterRepository,
                                              final Environment env) {
        super(clientConfig, shenyuClientRegisterRepository);
        this.env = env;
        this.isFull = Boolean.parseBoolean(clientConfig.getProps().getProperty(ShenyuClientConstants.IS_FULL, Boolean.FALSE.toString()));
        this.servletContextPath = env.getProperty("server.servlet.context-path", "");
    }
    
    /**
     * Check param.
     */
    @Override
    public void checkParam() {
        if (StringUtils.isBlank(getAppName())) {
            String errorMsg = "spring cloud param must config the appName";
            LOG.error(errorMsg);
            throw new ShenyuClientIllegalArgumentException(errorMsg);
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
        if (Boolean.TRUE.equals(isFull)) {
            return buildMetaDataDTO();
        }
        Controller controller = AnnotationUtils.findAnnotation(bean.getClass(), Controller.class);
        RestController restController = AnnotationUtils.findAnnotation(bean.getClass(), RestController.class);
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(bean.getClass(), RequestMapping.class);
        Annotation[] array = {controller, restController, requestMapping};
        if (isNext(array)) {
            String prePath = "";
            ShenyuSpringCloudClient clazzAnnotation = this.getAnnotation(bean.getClass());
            if (Objects.isNull(clazzAnnotation)) {
                return Collections.emptyList();
            }
            if (clazzAnnotation.path().indexOf("*") > 1) {
                return Collections.singletonList(buildMetaDataDTO(clazzAnnotation, prePath));
            }
            prePath = StringUtils.isBlank(clazzAnnotation.path()) ? getPrePath(requestMapping) : clazzAnnotation.path();
            final Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(bean.getClass());
            List<MetaDataRegisterDTO> metaDataRegisters = new ArrayList<>();
            for (Method method : methods) {
                ShenyuSpringCloudClient shenyuSpringCloudClient = this.getAnnotation(method);
                if (Objects.nonNull(shenyuSpringCloudClient)) {
                    metaDataRegisters.add(buildMetaDataDTO(shenyuSpringCloudClient, prePath));
                }
            }
            return metaDataRegisters;
        }
        return Collections.emptyList();
    }
    
    private String getPrePath(final Annotation annotation) {
        if (annotation == null) {
            return null;
        }
        Class<? extends Annotation> aClass = annotation.annotationType();
        if (aClass.isAssignableFrom(RequestMapping.class)) {
            String[] path = ((RequestMapping) annotation).path();
            if (path.length <= 0) {
                path = ((RequestMapping) annotation).value();
            }
            return path.length <= 0 ? null : path[0];
        }
        return null;
    }
    
    private boolean isNext(final Annotation[] annotations) {
        return Arrays.stream(annotations).anyMatch(Objects::nonNull);
    }
    
    private MetaDataRegisterDTO buildMetaDataDTO(final ShenyuSpringCloudClient shenyuSpringCloudClient, final String prePath) {
        String appName = env.getProperty("spring.application.name");
        String path = this.getContextPath() + Optional.ofNullable(prePath).orElse("") + shenyuSpringCloudClient.path();
        String desc = shenyuSpringCloudClient.desc();
        String configRuleName = shenyuSpringCloudClient.ruleName();
        String ruleName = ("".equals(configRuleName)) ? path : configRuleName;
        return MetaDataRegisterDTO.builder()
                .contextPath(StringUtils.defaultIfBlank(this.getContextPath(), this.servletContextPath))
                .appName(appName)
                .path(path)
                .pathDesc(desc)
                .rpcType(RpcTypeEnum.SPRING_CLOUD.getName())
                .enabled(shenyuSpringCloudClient.enabled())
                .ruleName(ruleName)
                .build();
    }
    
    private List<MetaDataRegisterDTO> buildMetaDataDTO() {
        if (isFullTrue) {
            return Collections.emptyList();
        }
        isFullTrue = true;
        MetaDataRegisterDTO build = this.universalMeta().enabled(true)
                .rpcExt(RpcTypeEnum.SPRING_CLOUD.getName()).build();
        return Collections.singletonList(build);
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
    
    private URIRegisterDTO buildURIRegisterDTO() {
        String host = this.getHost();
        return URIRegisterDTO.builder()
                .contextPath(this.getContextPath())
                .appName(this.getAppName())
                .host(host)
                .port(this.getPort())
                .rpcType(RpcTypeEnum.SPRING_CLOUD.getName())
                .build();
        
    }
    
    @Override
    protected ShenyuSpringCloudClient delegate(final ShenyuClient shenyuClient) {
        return new ShenyuSpringCloudClientDelegate(shenyuClient);
    }
    
    /**
     * Gets owner class.
     *
     * @return the owner class
     */
    @Override
    protected Class<ShenyuSpringCloudClient> getOwnerClass() {
        return ShenyuSpringCloudClient.class;
    }
}
