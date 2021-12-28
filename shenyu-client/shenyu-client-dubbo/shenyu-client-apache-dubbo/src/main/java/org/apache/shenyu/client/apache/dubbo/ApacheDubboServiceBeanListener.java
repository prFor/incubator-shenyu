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

package org.apache.shenyu.client.apache.dubbo;

import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.shenyu.client.dubbo.common.DubboServiceBeanListener;
import org.apache.shenyu.client.dubbo.common.ServiceBeanData;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.springframework.aop.support.AopUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ApacheDubboServiceBeanListenerEx .
 */
public class ApacheDubboServiceBeanListener extends DubboServiceBeanListener {
    
    /**
     * Instantiates a new Apache dubbo service bean listener ex.
     *
     * @param clientConfig                   the client config
     * @param shenyuClientRegisterRepository the shenyu client register repository
     */
    public ApacheDubboServiceBeanListener(final PropertiesConfig clientConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
        
    }
    
    /**
     * Gets meta data dto.
     *
     * @param applicationContext the object
     * @return the meta data dto
     */
    @Override
    @SuppressWarnings("all")
    public List<MetaDataRegisterDTO> getMetaDataDto(final Object applicationContext) {
        Map<String, ServiceBean> beans = this.getBeansOfType(ServiceBean.class);
        return beans.values().stream().flatMap(this::buildMetaData).collect(Collectors.toList());
    }
    
    private Stream<MetaDataRegisterDTO> buildMetaData(final ServiceBean<?> serviceBean) {
        Object refProxy = serviceBean.getRef();
        Class<?> clazz = refProxy.getClass();
        if (AopUtils.isAopProxy(clazz)) {
            clazz = AopUtils.getTargetClass(refProxy);
        }
        return buildMetaDataDTO(serviceBean, clazz).stream();
    }
    
    /**
     * Gets register dto.
     *
     * @return the register dto
     */
    @Override
    @SuppressWarnings("all")
    public URIRegisterDTO getRegisterDto() {
        Map<String, ServiceBean> beans = this.getBeansOfType(ServiceBean.class);
        return beans.values().stream().findFirst().map(e -> {
            return this.buildURIRegisterDTO(new ServiceBeanData(e));
        }).orElse(null);
    }
}
