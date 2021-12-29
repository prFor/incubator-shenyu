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
import org.apache.shenyu.client.core.annotaion.ShenyuClientUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RefreshedShenyuClientRegister .
 */
public abstract class RefreshedShenyuClientRegister extends AbstractShenyuClientRegister implements ApplicationListener<ContextRefreshedEvent> {
    
    private ApplicationContext applicationContext;
    
    /**
     * Instantiates a new Abstract shenyu client register.
     *
     * @param propertiesConfig               the properties config
     * @param shenyuClientRegisterRepository the shenyu client register repository
     */
    public RefreshedShenyuClientRegister(final PropertiesConfig propertiesConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(propertiesConfig, shenyuClientRegisterRepository);
    }
    
    /**
     * Gets beans of type.
     *
     * @param <T>   the type parameter
     * @param clazz the clazz
     * @return the beans of type
     */
    protected <T> Map<String, T> getBeansOfType(final Class<T> clazz) {
        if (this.applicationContext == null) {
            return Collections.emptyMap();
        }
        return this.getApplicationContext().getBeansOfType(clazz);
    }
    
    /**
     * Sets application context.
     *
     * @param applicationContext the application context
     */
    protected void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    /**
     * Gets application context.
     *
     * @return the application context
     */
    protected ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }
    
    /**
     * Handle an application event.
     *
     * @param contextRefreshedEvent the event to respond to
     */
    @Override
    public void onApplicationEvent(final ContextRefreshedEvent contextRefreshedEvent) {
        this.setApplicationContext(contextRefreshedEvent.getApplicationContext());
        mergeExecute();
    }
    
    /**
     * Gets annotation.
     *
     * @param <T>   the type parameter
     * @param clazz the clazz
     * @return the annotation
     */
    protected <T extends Annotation> T getAnnotation(final Class<?> clazz) {
        return ShenyuClientUtils.getShenyuClient(clazz, this.getOwnerClass(), this::delegate);
    }
    
    /**
     * Gets annotation.
     *
     * @param <T>    the type parameter
     * @param method the method
     * @return the annotation
     */
    protected <T extends Annotation> T getAnnotation(final Method method) {
        return ShenyuClientUtils.getShenyuClient(method, this.getOwnerClass(), this::delegate);
    }
    
    /**
     * Is shenyu client or owner boolean.
     *
     * @param method the method
     * @return the boolean
     */
    protected boolean isShenyuClientOrOwner(final Method method) {
        return ShenyuClientUtils.isShenyuClientOrOwner(method, this.getOwnerClass());
    }
    
    /**
     * Is shenyu client or owner boolean.
     *
     * @param clazz the clazz
     * @return the boolean
     */
    protected boolean isShenyuClientOrOwner(final Class<?> clazz) {
        return ShenyuClientUtils.isShenyuClientOrOwner(clazz, this.getOwnerClass());
    }
    
    /**
     * Merge execute.
     */
    void mergeExecute() {
        if (!this.isRegistered()) {
            return;
        }
        this.registerMetaData(this.applicationContext);
        this.registerService();
    }
    
    /**
     * Gets meta data dto.
     *
     * @param applicationContext the object
     * @return the meta data dto
     */
    @Override
    public abstract List<MetaDataRegisterDTO> getMetaDataDto(Object applicationContext);
}
