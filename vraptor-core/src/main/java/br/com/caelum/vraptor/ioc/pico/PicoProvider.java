/***
 *
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package br.com.caelum.vraptor.ioc.pico;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;

import br.com.caelum.vraptor.Interceptor;
import br.com.caelum.vraptor.core.DefaultConverters;
import br.com.caelum.vraptor.core.DefaultInterceptorStack;
import br.com.caelum.vraptor.core.DefaultRequestExecution;
import br.com.caelum.vraptor.core.DefaultResult;
import br.com.caelum.vraptor.core.VRaptorRequest;
import br.com.caelum.vraptor.http.AsmBasedTypeCreator;
import br.com.caelum.vraptor.http.OgnlParametersProvider;
import br.com.caelum.vraptor.http.StupidTranslator;
import br.com.caelum.vraptor.interceptor.DefaultInterceptorRegistry;
import br.com.caelum.vraptor.interceptor.ExecuteMethodInterceptor;
import br.com.caelum.vraptor.interceptor.InstantiateInterceptor;
import br.com.caelum.vraptor.interceptor.InterceptorListPriorToExecutionExtractor;
import br.com.caelum.vraptor.interceptor.InterceptorRegistry;
import br.com.caelum.vraptor.interceptor.ResourceLookupInterceptor;
import br.com.caelum.vraptor.ioc.Container;
import br.com.caelum.vraptor.ioc.ContainerProvider;
import br.com.caelum.vraptor.resource.DefaultMethodLookupBuilder;
import br.com.caelum.vraptor.resource.DefaultResourceRegistry;
import br.com.caelum.vraptor.resource.ResourceRegistry;
import br.com.caelum.vraptor.view.DefaultPathResolver;
import br.com.caelum.vraptor.view.jsp.PageResult;

/**
 * Managing internal components by using pico container.<br>
 * There is an extension point through the getDefaultContainer method, which
 * allows one to give a customized container.
 * 
 * @author Guilherme Silveira
 */
public class PicoProvider implements ContainerProvider {

    private final MutablePicoContainer container;

    public PicoProvider() {
        this.container = new PicoBuilder().withCaching().withLifecycle().build();
        for(Class<?> componentType : getCoreComponents()) {
            container.addComponent(componentType);
        }
        for(Class<?> componentType : getApplicationComponents()) {
            container.addComponent(componentType);
        }
        // cache(CacheBasedResourceRegistry.class, ResourceRegistry.class);
        // cache(CacheBasedTypeCreator.class, AsmBasedTypeCreator.class);
    }

    /**
     * While extending pico provider, do not register any INSTANCE component!
     * Cached instances might give problems later on.<br>
     * If there is any component instantiated and we change the implementation,
     * those who access the previous implementation will keep the reference
     * while new components will reference the new one -> NASTY!
     */
    protected List<Class<?>> getCoreComponents() {
        List<Class<?>> components = new ArrayList<Class<?>>();
        components.add(StupidTranslator.class);
        components.add(DefaultResourceRegistry.class);
        components.add(DefaultDirScanner.class);
        components.add(WebInfClassesScanner.class);
        components.add(DefaultInterceptorRegistry.class);
        components.add(AsmBasedTypeCreator.class);
        components.add(DefaultMethodLookupBuilder.class);
        components.add(DefaultPathResolver.class);
        return components;
    }

    public <T> T instanceFor(Class<T> type) {
        return container.getComponent(type);
    }

    public void start(ServletContext context) {
        this.container.addComponent(context);
        instanceFor(ResourceLocator.class).loadAll();
        container.start();
    }

    public void stop() {
        container.stop();
    }

    public Container provide(VRaptorRequest request) {
        MutablePicoContainer container = new PicoBuilder(this.container).withCaching().build();
        for(Class<?> componentType : getChildComponentTypes()) {
            container.addComponent(componentType);
        }
        for(Class<? extends Interceptor> type : instanceFor(InterceptorRegistry.class).all()) {
            container.addComponent(type);
        }
        for(Class<?> componentType : getRequestComponents()) {
            container.addComponent(componentType);
        }
        container.addComponent(request.getRequest().getSession());
        container.addComponent(request).addComponent(request.getRequest()).addComponent(request.getResponse());
        // cache(CachedConverters.class, Converters.class);
        PicoBasedContainer baseContainer = new PicoBasedContainer(this.container, container, request, instanceFor(ResourceRegistry.class));
        return baseContainer;
    }

    protected List<Class<?>> getChildComponentTypes() {
        List<Class<?>> components = new ArrayList<Class<?>>();
        components.add(InterceptorListPriorToExecutionExtractor.class);
        components.add(DefaultInterceptorStack.class);
        components.add(DefaultRequestExecution.class);
        components.add(ResourceLookupInterceptor.class);
        components.add(InstantiateInterceptor.class);
        components.add(DefaultResult.class);
        components.add(ExecuteMethodInterceptor.class);
        components.add(PageResult.class);
        components.add(OgnlParametersProvider.class);
        components.add(DefaultConverters.class);
        return components;
    }

    protected Class<?>[] getApplicationComponents() {
        return new Class<?>[0];
    }

    protected Class<?>[] getRequestComponents() {
        return new Class<?>[0];
    }
    
}
