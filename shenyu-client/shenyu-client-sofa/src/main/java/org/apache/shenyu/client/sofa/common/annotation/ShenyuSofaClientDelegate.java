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

package org.apache.shenyu.client.sofa.common.annotation;

import org.apache.shenyu.client.core.annotaion.ShenyuClient;

import java.lang.annotation.Annotation;

/**
 * ShenyuSofaClientDelegate .
 */
public class ShenyuSofaClientDelegate implements ShenyuSofaClient {
    
    private final ShenyuClient shenyuClient;
    
    public ShenyuSofaClientDelegate(final ShenyuClient shenyuClient) {
        this.shenyuClient = shenyuClient;
    }
    
    @Override
    public String path() {
        return shenyuClient.path();
    }
    
    @Override
    public String ruleName() {
        return shenyuClient.ruleName();
    }
    
    @Override
    public String desc() {
        return shenyuClient.desc();
    }
    
    @Override
    public boolean enabled() {
        return shenyuClient.enabled();
    }
    
    @Override
    public String loadBalance() {
        return "hash";
    }
    
    @Override
    public int timeout() {
        return -1;
    }
    
    @Override
    public int retries() {
        return 3;
    }
    
    /**
     * Returns the annotation type of this annotation.
     *
     * @return the annotation type of this annotation
     */
    @Override
    public Class<? extends Annotation> annotationType() {
        return ShenyuSofaClient.class;
    }
}
