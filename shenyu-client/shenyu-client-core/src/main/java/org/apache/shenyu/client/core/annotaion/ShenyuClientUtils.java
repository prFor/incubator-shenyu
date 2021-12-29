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

package org.apache.shenyu.client.core.annotaion;

import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;

/**
 * ShenyuClientDelegation .
 */
public class ShenyuClientUtils {
    
    /**
     * Gets shenyu client.
     *
     * @param <T>             the type parameter
     * @param clazz           the method
     * @param annotationClazz the annotation clazz
     * @param delegation      the delegation
     * @return the shenyu client
     */
    public static <T extends Annotation> T getShenyuClient(final Class<?> clazz,
                                                           final Class<T> annotationClazz,
                                                           final Function<ShenyuClient, T> delegation) {
        ShenyuClient shenyuClient = clazz.getAnnotation(ShenyuClient.class);
        T ownerClient;
        if (shenyuClient == null) {
            ownerClient = clazz.getAnnotation(annotationClazz);
        } else {
            ownerClient = delegation.apply(shenyuClient);
        }
        return ownerClient;
    }
    
    /**
     * Gets shenyu client.
     *
     * @param <T>             the type parameter
     * @param method          the method
     * @param annotationClazz the annotation clazz
     * @param delegation      the delegation
     * @return the shenyu client
     */
    public static <T extends Annotation> T getShenyuClient(final Method method,
                                                           final Class<T> annotationClazz,
                                                           final Function<ShenyuClient, T> delegation) {
        ShenyuClient shenyuClient = method.getAnnotation(ShenyuClient.class);
        T ownerClient;
        if (shenyuClient == null) {
            ownerClient = method.getAnnotation(annotationClazz);
        } else {
            ownerClient = delegation.apply(shenyuClient);
        }
        return ownerClient;
    }
    
    /**
     * Is shenyu client or owner boolean.
     *
     * @param <A>    the type parameter
     * @param method the method
     * @param owner  the owner
     * @return the boolean
     */
    public static <A extends Annotation> boolean isShenyuClientOrOwner(final Method method, final Class<A> owner) {
        return Objects.nonNull(AnnotationUtils.findAnnotation(method, ShenyuClient.class))
                || Objects.nonNull(AnnotationUtils.findAnnotation(method, owner));
    }
    
    /**
     * Is shenyu client or owner boolean.
     *
     * @param <A>   the type parameter
     * @param clazz the clazz
     * @param owner the owner
     * @return the boolean
     */
    public static <A extends Annotation> boolean isShenyuClientOrOwner(final Class<?> clazz, final Class<A> owner) {
        return Objects.nonNull(AnnotationUtils.findAnnotation(clazz, ShenyuClient.class))
                || Objects.nonNull(AnnotationUtils.findAnnotation(clazz, owner));
    }
}
