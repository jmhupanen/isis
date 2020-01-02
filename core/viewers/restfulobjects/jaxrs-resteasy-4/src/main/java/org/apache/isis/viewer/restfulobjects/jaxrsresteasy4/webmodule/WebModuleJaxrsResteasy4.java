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
package org.apache.isis.viewer.restfulobjects.jaxrsresteasy4.webmodule;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import lombok.var;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.services.inject.ServiceInjector;
import org.apache.isis.config.RestEasyConfiguration;
import org.apache.isis.viewer.restfulobjects.viewer.webmodule.IsisRestfulObjectsSessionFilter;
import org.apache.isis.viewer.restfulobjects.viewer.webmodule.IsisTransactionFilterForRestfulObjects;
import org.apache.isis.viewer.restfulobjects.viewer.webmodule.auth.AuthenticationSessionStrategyBasicAuth;
import org.apache.isis.webapp.modules.WebModule;
import org.apache.isis.webapp.modules.WebModuleAbstract;
import org.apache.isis.webapp.modules.WebModuleContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * WebModule that provides the RestfulObjects Viewer.
 * 
 * @since 2.0
 */
@Service
@Named("isisRoViewer.WebModuleJaxrsRestEasy4")
@Order(OrderPrecedence.MIDPOINT - 80)
@Qualifier("JaxrsRestEasy4")
@Log4j2
public final class WebModuleJaxrsResteasy4 extends WebModuleAbstract {

    private static final String ISIS_SESSION_FILTER_FOR_RESTFUL_OBJECTS = "IsisSessionFilterForRestfulObjects";
    private static final String ISIS_TRANSACTION_FILTER = "IsisTransactionFilterForRestfulObjects";

    private final RestEasyConfiguration restEasyConfiguration;

    private final String restfulPath;

    @Inject
    public WebModuleJaxrsResteasy4(
            final RestEasyConfiguration restEasyConfiguration,
            final ServiceInjector serviceInjector) {
        super(serviceInjector);
        this.restEasyConfiguration = restEasyConfiguration;
        this.restfulPath = this.restEasyConfiguration.getJaxrs().getDefaultPath() + "/";
    }

    @Getter
    private final String name = "JaxrsRestEasy4";

    @Override
    public void prepare(WebModuleContext ctx) {
        super.prepare(ctx);

        if(!isApplicable(ctx)) {
            return;
        }

        // register this module as a viewer
        ctx.addViewer("restfulobjects");
        ctx.addProtectedPath(this.restfulPath + "*");
    }

    @Override
    public List<ServletContextListener> init(ServletContext ctx) throws ServletException {

        registerFilter(ctx, ISIS_SESSION_FILTER_FOR_RESTFUL_OBJECTS, IsisRestfulObjectsSessionFilter.class)
                .ifPresent(filterReg -> {
                    // this is mapped to the entire application;
                    // however the IsisSessionFilter will
                    // "notice" if the session filter has already been
                    // executed for the request pipeline, and if so will do nothing
                    filterReg.addMappingForUrlPatterns(
                            null,
                            true,
                            getUrlPattern());

                    filterReg.setInitParameter(
                            "authenticationSessionStrategy",
                            AuthenticationSessionStrategyBasicAuth.class.getName());
                    filterReg.setInitParameter(
                            "whenNoSession", // what to do if no session was found ...
                            "auto"); // ... 401 and a basic authentication challenge if request originates from web browser
                    filterReg.setInitParameter(
                            "passThru",
                            String.join(",", getRestfulPath()+"swagger", getRestfulPath()+"health"));

                } );

        registerFilter(ctx, ISIS_TRANSACTION_FILTER, IsisTransactionFilterForRestfulObjects.class)
            .ifPresent(filterReg -> {
                filterReg.addMappingForUrlPatterns(
                        null,
                        true,
                        getUrlPattern());
            });


        // add RestEasy

//        // used by RestEasy to determine the JAX-RS resources and other related configuration
//        ctx.setInitParameter(
//                "javax.ws.rs.Application",
//                org.apache.isis.viewer.restfulobjects.viewer.jaxrsapp.RestfulObjectsApplication.class.getName());
//
//        ctx.setInitParameter("resteasy.servlet.mapping.prefix", getRestfulPath());
//        var servlet = ctx.addServlet(RESTEASY_DISPATCHER, HttpServletDispatcher.class);
//        if(servlet != null) {
//            serviceInjector.injectServicesInto(servlet);
//            servlet.addMapping(getUrlPattern());
//        }
//
//        // as per https://docs.jboss.org/resteasy/docs/4.4.2.Final/userguide/html/RESTEasy_Spring_Integration.html
//        var bootstrapper = ctx.createListener(ResteasyBootstrap.class);
//        serviceInjector.injectServicesInto(bootstrapper);
//
//        return Arrays.asList(bootstrapper);
        return Collections.emptyList();
    }


    // -- HELPER

    private String getUrlPattern() {
        return getRestfulPath() + "*";
    }

    private String getRestfulPath() {
        return this.restfulPath;
    }

}
