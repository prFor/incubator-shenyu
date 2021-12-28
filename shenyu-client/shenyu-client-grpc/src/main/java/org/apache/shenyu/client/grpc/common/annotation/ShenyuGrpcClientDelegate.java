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

package org.apache.shenyu.client.grpc.common.annotation;

import org.apache.shenyu.client.core.annotaion.ShenyuClient;

import java.lang.annotation.Annotation;

/**
 * ShenyuGrpcClientDelegation .
 */
public class ShenyuGrpcClientDelegate implements ShenyuGrpcClient {
    
    private final ShenyuClient shenyuClient;
    
    public ShenyuGrpcClientDelegate(final ShenyuClient shenyuClient) {
        this.shenyuClient = shenyuClient;
    }
    
    @Override
    public Class<? extends Annotation> annotationType() {
        return ShenyuGrpcClient.class;
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
     * Timeout long.
     *
     * @return the timeout
     */
    @Override
    public int timeout() {
        return 5000;
    }
}
