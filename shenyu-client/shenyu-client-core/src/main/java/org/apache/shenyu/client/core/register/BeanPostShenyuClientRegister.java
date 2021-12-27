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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * BeanPostShenyuClientRegister .
 */
public abstract class BeanPostShenyuClientRegister extends RefreshedShenyuClientRegister implements BeanPostProcessor {
    
    private Object bean;
    
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
        this.registerMetaData();
        this.bean = bean;
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
     * Gets bean.
     *
     * @return the bean
     */
    public Object getBean() {
        return this.bean;
    }
}
