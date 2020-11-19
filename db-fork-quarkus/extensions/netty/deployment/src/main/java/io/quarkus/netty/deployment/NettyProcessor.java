/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.netty.deployment;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;

class NettyProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    private static final Logger log = Logger.getLogger(NettyProcessor.class);

    @BuildStep
    SubstrateConfigBuildItem build() {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioSocketChannel"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioServerSocketChannel"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "java.util.LinkedHashMap"));

        SubstrateConfigBuildItem.Builder builder = SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("io.netty.noUnsafe", "true")
                .addRuntimeInitializedClass("io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator")
                .addRuntimeInitializedClass("io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
                .addRuntimeInitializedClass("io.netty.handler.ssl.util.ThreadLocalInsecureRandom")
                .addNativeImageSystemProperty("io.netty.leakDetection.level", "DISABLED");
        try {
            Class.forName("io.netty.handler.codec.http.HttpObjectEncoder");
            builder.addRuntimeReinitializedClass("io.netty.handler.codec.http2.Http2CodecUtil")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.HttpObjectEncoder")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http2.DefaultHttp2FrameWriter")
                    .addRuntimeInitializedClass("io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder");
        } catch (ClassNotFoundException e) {
            //ignore
            log.debug("Not registering Netty HTTP classes as they were not found");
        }
        return builder //TODO: make configurable
                .build();
    }

}
