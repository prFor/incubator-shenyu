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

package org.apache.shenyu.client.dubbo.common;

import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.dubbo.common.annotation.ShenyuDubboClient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * ShenyuClientDelegation .
 */
public class ShenyuClientUtils {
    
    /**
     * Gets shenyu client.
     *
     * @param method the method
     * @return the shenyu client
     */
    public static ShenyuClient getShenyuClient(final Method method) {
        ShenyuDubboClient shenyuDubboClient = method.getAnnotation(ShenyuDubboClient.class);
        ShenyuClient shenyuClient;
        if (shenyuDubboClient == null) {
            shenyuClient = method.getAnnotation(ShenyuClient.class);
        } else {
            shenyuClient = delegation(shenyuDubboClient);
        }
        return shenyuClient;
    }
    
    /**
     * Delegation shenyu client.
     *
     * @param shenyuDubboClient the client
     * @return the shenyu client
     */
    public static ShenyuClient delegation(ShenyuDubboClient shenyuDubboClient) {
        return new ShenyuClient() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ShenyuDubboClient.class;
            }
            
            @Override
            public String path() {
                return shenyuDubboClient.path();
            }
            
            @Override
            public String ruleName() {
                return shenyuDubboClient.ruleName();
            }
            
            @Override
            public String desc() {
                return shenyuDubboClient.desc();
            }
            
            @Override
            public boolean enabled() {
                return shenyuDubboClient.enabled();
            }
        };
    }
}
