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

import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.AntPathMatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * BeanPostShenyuClientRegister .
 */
public abstract class BeanPostShenyuClientRegister extends RefreshedShenyuClientRegister implements BeanPostProcessor {
    
    /**
     * Instantiates a new Abstract shenyu client register.
     *
     * @param clientConfig                   the client config
     * @param shenyuClientRegisterRepository the shenyu client register repository
     */
    public BeanPostShenyuClientRegister(final PropertiesConfig clientConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
    }
    
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        this.registerMetaData(bean);
        return bean;
    }
    
    /**
     * Handle an application event.
     *
     * @param contextRefreshedEvent the event to respond to
     */
    @Override
    public void onApplicationEvent(final ContextRefreshedEvent contextRefreshedEvent) {
        this.setApplicationContext(contextRefreshedEvent.getApplicationContext());
        if (!this.isRegistered()) {
            return;
        }
        this.registerService();
    }
    
    /**
     * Gets meta data dto.
     *
     * @param bean the object
     * @return the meta data dto
     */
    @Override
    public abstract List<MetaDataRegisterDTO> getMetaDataDto(Object bean);
    
    public MetaDataRegisterDTO buildMetaDataDto(final Object serviceBean, final ShenyuClient shenyuSofaClient, final Method method) {
        String appName = this.getAppName();
        String path = this.getContextPath() + shenyuSofaClient.path();
        String desc = shenyuSofaClient.desc();
//        String serviceName = serviceBean.getInterfaceClass().getName();
        String host = this.getHost();
        String methodName = method.getName();
        int port = this.getPort();
        MetaDataRegisterDTO.Builder builder = MetaDataRegisterDTO
                .builder()
                .appName(appName)
                .serviceName("serviceName")
                .methodName(methodName)
                .contextPath(this.getContextPath())
                .host(host)
                .port(port)
                .path(path);
        return builder.build();
    }
    
//    public abstract void metaDataDiff(MetaDataRegisterDTO.Builder);
}

