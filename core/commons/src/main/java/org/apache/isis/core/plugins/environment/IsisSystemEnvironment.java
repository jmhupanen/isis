/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.core.plugins.environment;

/**
 * Represents configuration, that is available in an early phase of bootstrapping 
 * and should is regarded immutable during application's life-cycle.
 * 
 * @since 2.0.0-M2
 */
public interface IsisSystemEnvironment {

    // -- INTERFACE
    
    public DeploymentType getDeploymentType();
    
    // -- FACTORIES
    
    public static IsisSystemEnvironment getDefault() {
        return DEFAULT;
    }
    
    public static IsisSystemEnvironment of(DeploymentType deploymentType) {
        return ()->deploymentType;        
    }

    // -- DEFAULT IMPLEMENTATION
    
    public static final IsisSystemEnvironment DEFAULT = new IsisSystemEnvironment() {
        @Override
        public DeploymentType getDeploymentType() {
            boolean anyVoteForPrototyping = false;
            
            anyVoteForPrototyping|=
                    "true".equalsIgnoreCase(System.getenv("PROTOTYPING"));
            
            anyVoteForPrototyping|=
                    "server-prototype".equalsIgnoreCase(System.getProperty("isis.deploymentType"));
            
            anyVoteForPrototyping|=
                    "PROTOTYPING".equalsIgnoreCase(System.getProperty("isis.deploymentType"));
            
            final DeploymentType deploymentType =
                    anyVoteForPrototyping
                        ? DeploymentType.PROTOTYPING
                                : DeploymentType.PRODUCTION;
            
            return deploymentType;
        }
    };
        
    
}


