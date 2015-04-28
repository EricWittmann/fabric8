/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.gateway.apiman;

import io.apiman.gateway.engine.IComponentRegistry;
import io.apiman.gateway.engine.IConnectorFactory;
import io.apiman.gateway.engine.IMetrics;
import io.apiman.gateway.engine.IPluginRegistry;
import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.components.IBufferFactoryComponent;
import io.apiman.gateway.engine.components.IHttpClientComponent;
import io.apiman.gateway.engine.components.IRateLimiterComponent;
import io.apiman.gateway.engine.components.ISharedStateComponent;
import io.apiman.gateway.engine.es.ESRateLimiterComponent;
import io.apiman.gateway.engine.es.ESRegistry;
import io.apiman.gateway.engine.es.ESSharedStateComponent;
import io.apiman.gateway.engine.impl.DefaultComponentRegistry;
import io.apiman.gateway.engine.impl.DefaultEngineFactory;
import io.apiman.gateway.vertx.components.BufferFactoryComponentImpl;
import io.apiman.gateway.vertx.components.HttpClientComponentImpl;
import io.apiman.gateway.vertx.engine.VertxPluginRegistry;
import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.api.handlers.http.HttpGatewayServiceClient;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Vertx;

/**
 * An engine factory used when embedding apiman into the fabric8 gateway.
 */
public class EngineFactory extends DefaultEngineFactory {

    final Vertx vertx;
    final HttpGateway httpGateway;
    final Map<String, String> esConfig = new HashMap<>();

    /**
     * Constructor.
     * @param vertx
     * @param httpGateway
     */
    public EngineFactory(final Vertx vertx, final HttpGateway httpGateway) {
        this.vertx = vertx;
        this.httpGateway = httpGateway;

        // TODO use proper/dynamic configuration values for these!
        esConfig.put("client.type", "transport");
        esConfig.put("client.cluster-name", "apiman");
        esConfig.put("client.host", "localhost");
        esConfig.put("client.port", "9300");
    }

    /**
     * @see io.apiman.gateway.engine.impl.DefaultEngineFactory#createMetrics()
     */
    @Override
    protected IMetrics createMetrics() {
        return new DropWizardMetrics();
    }

    /**
     * @see io.apiman.gateway.engine.impl.AbstractEngineFactory#createConnectorFactory()
     */
    @Override
    protected IConnectorFactory createConnectorFactory() {
        HttpGatewayServiceClient httpGatewayServiceClient = new HttpGatewayServiceClient(vertx, httpGateway);
        return new Fabric8ConnectorFactory(vertx, httpGatewayServiceClient);
    }

    @Override
    protected IRegistry createRegistry() {
        return new DelegatingRegistryWithMapping(new ESRegistry(esConfig));
    }

    /**
     * @see io.apiman.gateway.engine.impl.DefaultEngineFactory#createComponentRegistry()
     */
    @Override
    protected IComponentRegistry createComponentRegistry() {
        return new DefaultComponentRegistry() {
            @Override
            protected void registerRateLimiterComponent() {
                addComponent(IRateLimiterComponent.class, new ESRateLimiterComponent(esConfig));
            }

            @Override
            protected void registerSharedStateComponent() {
                addComponent(ISharedStateComponent.class, new ESSharedStateComponent(esConfig));
            }

            @Override
            protected void registerBufferFactoryComponent() {
                addComponent(IBufferFactoryComponent.class, new BufferFactoryComponentImpl());
            }

            @Override
            protected void registerHttpClientComponent() {
                addComponent(IHttpClientComponent.class, new HttpClientComponentImpl(vertx));
            }
        };
    }

    @Override
    protected IPluginRegistry createPluginRegistry() {
        return new VertxPluginRegistry(vertx);
    }
}
