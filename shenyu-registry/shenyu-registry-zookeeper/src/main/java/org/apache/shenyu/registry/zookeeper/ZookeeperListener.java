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

package org.apache.shenyu.registry.zookeeper;

import org.I0Itec.zkclient.IZkDataListener;

/**
 * ZookeeperListener .
 * listener.
 */
public abstract class ZookeeperListener implements IZkDataListener {
    
    
    /**
     * Data change string.
     *
     * @param path the path
     * @param data the data
     * @return the string
     */
    abstract void dataChange(String path, String data);
    
    @Override
    public void handleDataChange(final String dataPath, final Object data) throws Exception {
        this.dataChange(dataPath, data.toString());
    }
    
    @Override
    public void handleDataDeleted(final String dataPath) throws Exception {
    
    }
}
