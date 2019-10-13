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
package org.apache.isis.config.registry;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.Vetoed;

import org.springframework.stereotype.Component;

import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.ViewModel;
import org.apache.isis.commons.internal.base._Strings;
import org.apache.isis.commons.internal.collections._Lists;
import org.apache.isis.commons.internal.collections._Sets;
import org.apache.isis.commons.internal.components.ApplicationScopedComponent;
import org.apache.isis.commons.internal.components.SessionScopedComponent;
import org.apache.isis.commons.internal.components.TransactionScopedComponent;
import org.apache.isis.commons.internal.context._Context;
import org.apache.isis.commons.internal.ioc.BeanSort;
import org.apache.isis.commons.internal.reflection._Reflect;
import org.apache.isis.config.beans.IsisComponentScanInterceptor;

import static org.apache.isis.commons.internal.base._With.requires;
import static org.apache.isis.commons.internal.reflection._Annotations.findNearestAnnotation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * Holds the set of domain services, persistent entities and fixture scripts.services etc.
 * @since 2.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE) @Log4j2
public final class IsisBeanTypeRegistry implements IsisComponentScanInterceptor, AutoCloseable {

    public static IsisBeanTypeRegistry current() {
        return _Context.computeIfAbsent(IsisBeanTypeRegistry.class, IsisBeanTypeRegistry::new);
    }

    /**
     * Inbox for introspection, as used by the SpecificationLoader
     */
    private final Map<Class<?>, BeanSort> introspectableTypes = new HashMap<>();

    // -- DISTINCT CATEGORIES OF BEAN SORTS
    
    @Getter private final Set<Class<?>> managedBeanTypes = new HashSet<>();
    @Getter private final Set<Class<?>> entityTypes = new HashSet<>();
    @Getter private final Set<Class<?>> mixinTypes = new HashSet<>();
    @Getter private final Set<Class<?>> viewModelTypes = new HashSet<>();
    @Getter private final Set<Class<?>> vetoedTypes = _Sets.newConcurrentHashSet();
    
    private final List<Set<? extends Class<? extends Object>>> allCategorySets = _Lists.of(
            managedBeanTypes,
            entityTypes,
            mixinTypes,
            viewModelTypes,
            vetoedTypes
            );

    @Override
    public void close() {

//        if(!_Spring.isContextAvailable()) {
//            // this instance needs to survive a _Context.clear() call when Spring's context 
//            // gets passed over to Isis
//            return;
//        }

        introspectableTypes.clear();
        allCategorySets.forEach(Set::clear);
    }

    // -- INBOX

    public void addIntrospectableType(BeanSort sort, Class<?> type) {
        synchronized (introspectableTypes) {
            introspectableTypes.put(type, sort);
            
            switch (sort) {
            case MANAGED_BEAN:
                managedBeanTypes.add(type);
                return;
            case MIXIN:
                mixinTypes.add(type);
                return;
            case ENTITY:
                entityTypes.add(type); //TODO redundant
                return;
            case VIEW_MODEL:
                viewModelTypes.add(type);
                return;
            
            //XXX skip introspection for these
            case COLLECTION:
            case VALUE:
            case UNKNOWN:
                break;
            }
            
        }
    }

    public Map<Class<?>, BeanSort> snapshotIntrospectableTypes() {

        final Map<Class<?>, BeanSort> defensiveCopy;

        synchronized (introspectableTypes) {
            defensiveCopy = new HashMap<>(introspectableTypes);
        }

        if(log.isDebugEnabled()) {
            defensiveCopy.forEach((k, v)->{
                log.debug("to be introspected: {} [{}]", k.getName(), v.name());
            });
        }

        return defensiveCopy;
    }
    
    public void veto(Class<?> type) {
        vetoedTypes.add(type);
    }

    // -- FILTER

    @Override
    public boolean isInjectable(TypeMetaData typeMeta) {
        
        val type = typeMeta.getUnderlyingClass();
        
        intercept(type);
        
        if(findNearestAnnotation(type, DomainObject.class).isPresent()) {
            return false; // reject
        }
        
        if(findNearestAnnotation(type, ViewModel.class).isPresent()) {
            return false; // reject
        }
        
        if(findNearestAnnotation(type, Mixin.class).isPresent()) {
            return false; // reject
        }
        
        if(findNearestAnnotation(type, Vetoed.class).isPresent()) {
            return false; // reject
        }
        
        return true;
    }
    
    @Override
    public String getBeanNameOverride(TypeMetaData typeMeta) {
        return extractObjectType(typeMeta.getUnderlyingClass()).orElse(null);
    }
    
    @Override
    public boolean isManagedBean(Class<?> type) {
        if(vetoedTypes.contains(type)) { // vetos are coming from the spec-loader during init
            return false;
        }
        return managedBeanTypes.contains(type);
    }
    
    // -- HELPER
    
    private void intercept(Class<?> type) {

        val beanSort = quickClassify(type);

        val isToBeRegistered = beanSort.isManagedBean();
        val isToBeInspected = !beanSort.isUnknown();

        if(isToBeInspected) {
            addIntrospectableType(beanSort, type);
        }

        if(log.isDebugEnabled()) {
            if(isToBeInspected || isToBeRegistered) {
                log.debug("{} {} [{}]",
                        isToBeRegistered ? "provision" : beanSort.isEntity() ? "entity" : "skip",
                                type,
                                beanSort.name());
            }
        }

    }

    // the SpecLoader does a better job at this
    private BeanSort quickClassify(Class<?> type) {

        requires(type, "type");

        
        if(findNearestAnnotation(type, Vetoed.class).isPresent()) {
            return BeanSort.UNKNOWN; // reject
        }

        if(Collection.class.isAssignableFrom(type)) {
            return BeanSort.COLLECTION;
        }

        val aDomainService = findNearestAnnotation(type, DomainService.class);
        if(aDomainService.isPresent()) {
            return BeanSort.MANAGED_BEAN;
        }

        //this takes precedence over whatever @DomainObject has to say
        if(_Reflect.containsAnnotation(type, "javax.jdo.annotations.PersistenceCapable")) {
            return BeanSort.ENTITY;
        }

        if(findNearestAnnotation(type, Mixin.class).isPresent()) {
            return BeanSort.MIXIN;
        }

        if(findNearestAnnotation(type, ViewModel.class).isPresent()) {
            return BeanSort.VIEW_MODEL;
        }

        if(org.apache.isis.applib.ViewModel.class.isAssignableFrom(type)) {
            return BeanSort.VIEW_MODEL;
        }

        if(ApplicationScopedComponent.class.isAssignableFrom(type)) {
            return BeanSort.MANAGED_BEAN;
        }

        if(SessionScopedComponent.class.isAssignableFrom(type)) {
            return BeanSort.MANAGED_BEAN;
        }

        if(TransactionScopedComponent.class.isAssignableFrom(type)) {
            return BeanSort.MANAGED_BEAN;
        }
        
        val aDomainObject = findNearestAnnotation(type, DomainObject.class).orElse(null);
        if(aDomainObject!=null) {
            switch (aDomainObject.nature()) {
            case MIXIN:
                return BeanSort.MIXIN;
            case JDO_ENTITY:
                return BeanSort.ENTITY;
            case EXTERNAL_ENTITY:
            case INMEMORY_ENTITY:
            case VIEW_MODEL:
            case NOT_SPECIFIED:
                return BeanSort.VIEW_MODEL; //because object is not associated with a persistence context unless discovered above
            } 
        }

//XXX RequestScoped is just a qualifier, don't decide on that
//        
//        if(findNearestAnnotation(type, RequestScoped.class).isPresent()) {
//            return BeanSort.MANAGED_BEAN;
//        }

        if(findNearestAnnotation(type, Component.class).isPresent()) {
            return BeanSort.MANAGED_BEAN;
        }

        if(Serializable.class.isAssignableFrom(type)) {
            return BeanSort.VALUE;
        }

        return BeanSort.UNKNOWN;
    }

    private Optional<String> extractObjectType(Class<?> type) {

        val aDomainService = _Reflect.getAnnotation(type, DomainService.class);
        if(aDomainService!=null) {
            val objectType = aDomainService.objectType();
            if(_Strings.isNotEmpty(objectType)) {
                return Optional.of(objectType); 
            }
            return Optional.empty(); // stop processing
        }

        return Optional.empty();

    }




}