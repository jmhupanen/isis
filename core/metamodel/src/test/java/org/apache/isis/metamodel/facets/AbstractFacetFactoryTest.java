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

package org.apache.isis.metamodel.facets;

import java.lang.reflect.Method;
import java.util.List;

import org.jmock.Expectations;
import org.junit.Rule;

import org.apache.isis.applib.Identifier;
import org.apache.isis.applib.services.i18n.TranslationService;
import org.apache.isis.config.IsisConfiguration;
import org.apache.isis.metamodel.MetaModelContext;
import org.apache.isis.metamodel.adapter.ObjectAdapterProvider;
import org.apache.isis.metamodel.facetapi.FacetHolder;
import org.apache.isis.metamodel.facetapi.FacetHolderImpl;
import org.apache.isis.metamodel.facetapi.FeatureType;
import org.apache.isis.metamodel.facetapi.IdentifiedHolder;
import org.apache.isis.metamodel.services.persistsession.PersistenceSessionServiceInternal;
import org.apache.isis.metamodel.spec.ObjectSpecification;
import org.apache.isis.metamodel.specloader.SpecificationLoader;
import org.apache.isis.security.authentication.AuthenticationSession;
import org.apache.isis.security.authentication.AuthenticationSessionProvider;
import org.apache.isis.unittestsupport.jmocking.JUnitRuleMockery2;

import junit.framework.TestCase;

public abstract class AbstractFacetFactoryTest extends TestCase {

    @Rule
    public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

    public static class Customer {

        private String firstName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }
    }

    protected ObjectAdapterProvider mockObjectAdapterProvider;
    protected TranslationService mockTranslationService;
    protected AuthenticationSessionProvider mockAuthenticationSessionProvider;
    protected AuthenticationSession mockAuthenticationSession;

    protected PersistenceSessionServiceInternal mockPersistenceSessionServiceInternal;

    protected SpecificationLoader mockSpecificationLoader;
    protected MethodRemoverForTesting methodRemover;

    protected FacetHolder facetHolder;
    protected FacetedMethod facetedMethod;
    protected FacetedMethodParameter facetedMethodParameter;

    public static class IdentifiedHolderImpl extends FacetHolderImpl implements IdentifiedHolder {

        private Identifier identifier;

        public IdentifiedHolderImpl(final Identifier identifier) {
            this.identifier = identifier;
        }

        @Override
        public Identifier getIdentifier() {
            return identifier;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // PRODUCTION

        facetHolder = new IdentifiedHolderImpl(
                Identifier.propertyOrCollectionIdentifier(Customer.class, "firstName"));
        facetedMethod = FacetedMethod.createForProperty(Customer.class, "firstName");
        facetedMethodParameter = new FacetedMethodParameter(
                FeatureType.ACTION_PARAMETER_SCALAR, facetedMethod.getOwningType(), facetedMethod.getMethod(), String.class
                );

        methodRemover = new MethodRemoverForTesting();

        mockAuthenticationSessionProvider = context.mock(AuthenticationSessionProvider.class);

        mockTranslationService = context.mock(TranslationService.class);
        mockAuthenticationSession = context.mock(AuthenticationSession.class);

        mockPersistenceSessionServiceInternal = context.mock(PersistenceSessionServiceInternal.class);

        mockSpecificationLoader = context.mock(SpecificationLoader.class);

        mockObjectAdapterProvider = context.mock(ObjectAdapterProvider.class);

        MetaModelContext.preset(MetaModelContext.builder()
                .specificationLoader(mockSpecificationLoader)
                .translationService(mockTranslationService)
                .objectAdapterProvider(mockPersistenceSessionServiceInternal)
                .authenticationSessionProvider(mockAuthenticationSessionProvider)
                .build());

        context.checking(new Expectations() {{

            allowing(mockAuthenticationSessionProvider).getAuthenticationSession();
            will(returnValue(mockAuthenticationSession));
        }});
    }



    protected void allowing_specificationLoader_loadSpecification_any_willReturn(final ObjectSpecification objectSpecification) {
        context.checking(new Expectations() {{
            allowing(mockSpecificationLoader).loadSpecification(with(any(Class.class)));
            will(returnValue(objectSpecification));
        }});
    }

    @Override
    protected void tearDown() throws Exception {
        mockSpecificationLoader = null;
        methodRemover = null;
        facetedMethod = null;
        super.tearDown();
    }

    protected static boolean contains(final Class<?>[] types, final Class<?> type) {
        return Utils.contains(types, type);
    }

    protected static boolean contains(final List<FeatureType> featureTypes, final FeatureType featureType) {
        return Utils.contains(featureTypes, featureType);
    }

    protected static Method findMethod(final Class<?> type, final String methodName, final Class<?>[] methodTypes) {
        return Utils.findMethod(type, methodName, methodTypes);
    }

    protected Method findMethod(final Class<?> type, final String methodName) {
        return Utils.findMethod(type, methodName);
    }

    protected void assertNoMethodsRemoved() {
        assertTrue(methodRemover.getRemovedMethodMethodCalls().isEmpty());
        assertTrue(methodRemover.getRemoveMethodArgsCalls().isEmpty());
    }

}
