/**
 * Copyright 2017-2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.rabbitmq;

import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Gilles Robert
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {RabbitMqTracingNoConfigurationItTest.TestConfig.class}
)
public class RabbitMqTracingNoConfigurationItTest extends BaseRabbitMqTracingItTest {

  @Autowired private RabbitTemplate rabbitTemplate;

  @Test
  public void testSendAndReceiveRabbitMessage_whenNoRabbitMqTracingConfig() {
    final String message = "hello world message!";
    rabbitTemplate.convertAndSend("myExchange", "#", message);
    checkNoSpans();
  }

  @Configuration
  @Import({
      RabbitWithRabbitTemplateConfig.class,
      TracerConfig.class,
      // RabbitMqTracingManualConfig.class - using neither manual nor auto-configuration for tracing RabbitMq
  })
  static class TestConfig {
  }
}
