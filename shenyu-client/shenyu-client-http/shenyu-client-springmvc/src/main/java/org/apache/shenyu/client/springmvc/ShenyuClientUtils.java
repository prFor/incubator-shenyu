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

package org.apache.shenyu.client.springmvc;

import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.springmvc.annotation.ShenyuSpringMvcClient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * ShenyuClientDelegation .
 */
public class ShenyuClientUtils {
    
    /**
     * Gets shenyu client.
     *
     * @param clazz the method
     * @return the shenyu client
     */
    public static ShenyuSpringMvcClient getShenyuClient(final Class<?> clazz) {
        ShenyuClient shenyuClient = clazz.getAnnotation(ShenyuClient.class);
        ShenyuSpringMvcClient shenyuGrpcClient;
        if (shenyuClient == null) {
            shenyuGrpcClient = clazz.getAnnotation(ShenyuSpringMvcClient.class);
        } else {
            shenyuGrpcClient = delegation(shenyuClient);
        }
        return shenyuGrpcClient;
    }
    
    /**
     * Gets shenyu client.
     *
     * @param method the method
     * @return the shenyu client
     */
    public static ShenyuSpringMvcClient getShenyuClient(final Method method) {
        ShenyuClient shenyuClient = method.getAnnotation(ShenyuClient.class);
        ShenyuSpringMvcClient shenyuGrpcClient;
        if (shenyuClient == null) {
            shenyuGrpcClient = method.getAnnotation(ShenyuSpringMvcClient.class);
        } else {
            shenyuGrpcClient = delegation(shenyuClient);
        }
        return shenyuGrpcClient;
    }
    
    /**
     * Delegation shenyu client.
     *
     * @param shenyuClient the shenyu client
     * @return the shenyu client
     */
    public static ShenyuSpringMvcClient delegation(ShenyuClient shenyuClient) {
        return new ShenyuSpringMvcClient() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ShenyuSpringMvcClient.class;
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
            
            /**
             * Register meta data boolean.
             *
             * @return the boolean
             */
            @Override
            public boolean registerMetaData() {
                return false;
            }
        };
    }
}
