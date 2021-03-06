/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.spring.boot.cloud;

import org.apache.camel.Expression;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootApplication
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        CamelCloudServiceCallRefExpressionTest.TestConfiguration.class
    },
    properties = {
        "service.name=custom-svc-list",
        "camel.cloud.load-balancer.enabled=false",
        "camel.cloud.service-call.component=jetty",
        "camel.cloud.service-call.expression=myExpression",
        "camel.cloud.service-discovery.services[custom-svc-list]=localhost:9090,localhost:9091,localhost:9092",
        "camel.cloud.service-filter.blacklist[custom-svc-list]=localhost:9091",
        "ribbon.enabled=false",
        "debug=true"
    }
)
public class CamelCloudServiceCallRefExpressionTest {
    @Autowired
    private ProducerTemplate template;

    @Test
    public void testServiceCall() throws Exception {
        Assert.assertEquals("9090", template.requestBody("direct:start", null, String.class));
        Assert.assertEquals("9092", template.requestBody("direct:start", null, String.class));
    }

    // *************************************
    // Config
    // *************************************

    @Configuration
    public static class TestConfiguration {
        @Bean
        Expression myExpression() {
            return new SimpleExpression(
                "jetty:http://${header.CamelServiceCallServiceHost}:${header.CamelServiceCallServicePort}/hello"
            );
        }

        @Bean
        public RouteBuilder myRouteBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                        .serviceCall("{{service.name}}");

                    from("jetty:http://localhost:9090/hello")
                        .transform()
                        .constant("9090");
                    from("jetty:http://localhost:9091/hello")
                        .transform()
                        .constant("9091");
                    from("jetty:http://localhost:9092/hello")
                        .transform()
                        .constant("9092");
                }
            };
        }
    }
}

