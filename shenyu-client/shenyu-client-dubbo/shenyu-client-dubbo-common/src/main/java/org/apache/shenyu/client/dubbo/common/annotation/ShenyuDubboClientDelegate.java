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

package org.apache.shenyu.client.dubbo.common.annotation;

import org.apache.shenyu.client.core.annotaion.ShenyuClient;

import java.lang.annotation.Annotation;

/**
 * ShenyuDubboClientDelegation .
 */
public class ShenyuDubboClientDelegate implements ShenyuDubboClient {
    
    private final ShenyuClient shenyuClient;
    
    public ShenyuDubboClientDelegate(final ShenyuClient shenyuClient) {
        this.shenyuClient = shenyuClient;
    }
    
    /**
     * Path string.
     *
     * @return the string
     */
    @Override
    public String path() {
        return shenyuClient.path();
    }
    
    /**
     * Rule name string.
     *
     * @return the string
     */
    @Override
    public String ruleName() {
        return shenyuClient.ruleName();
    }
    
    /**
     * Desc string.
     *
     * @return String string
     */
    @Override
    public String desc() {
        return shenyuClient.desc();
    }
    
    /**
     * Enabled boolean.
     *
     * @return the boolean
     */
    @Override
    public boolean enabled() {
        return shenyuClient.enabled();
    }
    
    /**
     * Returns the annotation type of this annotation.
     *
     * @return the annotation type of this annotation
     */
    @Override
    public Class<? extends Annotation> annotationType() {
        return ShenyuDubboClient.class;
    }
}
