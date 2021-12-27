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

import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Collections;
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
    protected <T> Map<String, T> getBeansOfType(Class<T> clazz) {
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
    protected void setApplicationContext(ApplicationContext applicationContext) {
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
}
