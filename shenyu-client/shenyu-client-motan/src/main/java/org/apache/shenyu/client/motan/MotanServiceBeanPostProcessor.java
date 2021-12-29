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

package org.apache.shenyu.client.motan;

import com.weibo.api.motan.config.springsupport.BasicServiceConfigBean;
import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shenyu.client.core.annotaion.ShenyuClient;
import org.apache.shenyu.client.core.exception.ShenyuClientIllegalArgumentException;
import org.apache.shenyu.client.core.register.BeanPostShenyuClientRegister;
import org.apache.shenyu.client.motan.common.annotation.ShenyuMotanClient;
import org.apache.shenyu.client.motan.common.annotation.ShenyuMotanClientDelegate;
import org.apache.shenyu.client.motan.common.dto.MotanRpcExt;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.register.client.api.ShenyuClientRegisterRepository;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Motan BeanPostProcessor.
 */
public class MotanServiceBeanPostProcessor extends BeanPostShenyuClientRegister implements ApplicationContextAware {
    
    private static final String BASE_SERVICE_CONFIG = "baseServiceConfig";
    
    private final LocalVariableTableParameterNameDiscoverer localVariableTableParameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
    
    private String group;
    
    
    public MotanServiceBeanPostProcessor(final PropertiesConfig clientConfig, final ShenyuClientRegisterRepository shenyuClientRegisterRepository) {
        super(clientConfig, shenyuClientRegisterRepository);
    }
    
    private List<MetaDataRegisterDTO> handler(final Object bean) {
        if (group == null) {
            group = ((BasicServiceConfigBean) this.getApplicationContext().getBean(BASE_SERVICE_CONFIG)).getGroup();
        }
        Class<?> clazz = bean.getClass();
        if (AopUtils.isAopProxy(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(clazz);
        MotanService service = clazz.getAnnotation(MotanService.class);
        final Class<?> finalClazz = clazz;
        return Arrays.stream(methods)
                .filter(this::isShenyuClientOrOwner)
                .map(method -> buildMetaDataDTO(finalClazz, service,
                        this.getAnnotation(method), method, buildRpcExt(methods))).collect(Collectors.toList());
    }
    
    private MetaDataRegisterDTO buildMetaDataDTO(final Class<?> clazz, final MotanService service,
                                                 final ShenyuMotanClient shenyuMotanClient,
                                                 final Method method,
                                                 final String rpcExt) {
        Class<?>[] parameterTypesClazz = method.getParameterTypes();
        String parameterTypes = Arrays.stream(parameterTypesClazz).map(Class::getName)
                .collect(Collectors.joining(","));
        String serviceName;
        if (void.class.equals(service.interfaceClass())) {
            if (clazz.getInterfaces().length > 0) {
                serviceName = clazz.getInterfaces()[0].getName();
            } else {
                throw new ShenyuClientIllegalArgumentException("Failed to export remote service class " + clazz.getName()
                        + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
            }
        } else {
            serviceName = service.interfaceClass().getName();
        }
        MetaDataRegisterDTO.Builder builder = this.universalMeta(shenyuMotanClient, method);
        return builder.serviceName(serviceName)
                .parameterTypes(parameterTypes)
                .rpcType(RpcTypeEnum.MOTAN.getName())
                .rpcExt(rpcExt).build();
    }
    
    private MotanRpcExt.RpcExt buildRpcExt(final Method method) {
        String[] paramNames = localVariableTableParameterNameDiscoverer.getParameterNames(method);
        List<Pair<String, String>> params = new ArrayList<>();
        if (paramNames != null && paramNames.length > 0) {
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramNames.length; i++) {
                params.add(Pair.of(paramTypes[i].getName(), paramNames[i]));
            }
        }
        return new MotanRpcExt.RpcExt(method.getName(), params);
    }
    
    private String buildRpcExt(final Method[] methods) {
        List<MotanRpcExt.RpcExt> list = Arrays.stream(methods)
                .filter(this::isShenyuClientOrOwner).map(this::buildRpcExt).collect(Collectors.toList());
        MotanRpcExt buildList = new MotanRpcExt(list, group);
        return GsonUtils.getInstance().toJson(buildList);
    }
    
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);
    }
    
    @Override
    public void checkParam() {
        if (StringUtils.isEmpty(this.getContextPath())) {
            throw new ShenyuClientIllegalArgumentException("motan client must config the contextPath");
        }
    }
    
    @Override
    public List<MetaDataRegisterDTO> getMetaDataDto(final Object bean) {
        Class<?> clazz = bean.getClass();
        if (AopUtils.isAopProxy(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        MotanService service = clazz.getAnnotation(MotanService.class);
        if (service != null) {
            return handler(bean);
        }
        return Collections.emptyList();
    }
    
    @Override
    protected ShenyuMotanClient delegate(final ShenyuClient client) {
        return new ShenyuMotanClientDelegate(client);
    }
    
    @Override
    protected Class<ShenyuMotanClient> getOwnerClass() {
        return ShenyuMotanClient.class;
    }
    
    @Override
    public URIRegisterDTO getRegisterDto() {
        return null;
    }
}
