/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.dromara.soul.admin.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * this is rule view to web front.
 *
 * @author jiangxiaofeng(programgeek @ 163.com)
 */
@Data
public class RuleVO implements Serializable {

    /**
     * primary key
     */
    private String id;

    /**
     * selector id
     */
    private String selectorId;

    /**
     * match mode
     */
    private Integer matchMode;

    /**
     * rule name
     */
    private String name;

    /**
     * whether enabled
     */
    private Boolean enabled;

    /**
     * whether loged
     */
    private Boolean loged;

    /**
     * sort rank
     */
    private Integer rank;

    /**
     * process logic
     */
    private String handle;

    /**
     * rule conditions
     */
    private List<RuleConditionVO> ruleConditions;

    /**
     * created time.
     */
    private LocalDateTime dateCreated;

    /**
     * updated time.
     */
    private LocalDateTime dateUpdated;
}