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

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

@RequiredArgsConstructor
class RabbitMqSendTracingHelper {
  private final Tracer tracer;
  private final MessageConverter messageConverter;
  private final RabbitMqSpanDecorator spanDecorator;
  private Scope scope;

  Object doWithTracingHeadersMessage(String exchange, String routingKey, Object message, ProceedFunction proceedCallback)
      throws Throwable {
    Message messageWithTracingHeaders = doBefore(exchange, routingKey, message);
    try {
      Object resp = proceedCallback.apply(messageWithTracingHeaders);
      return resp;
    } catch (AmqpException ex) {
      spanDecorator.onError(ex, scope.span());
      throw ex;
    } finally {
      scope.close();
    }
  }

  private Message doBefore(String exchange, String routingKey, Object message) {
    Message convertedMessage = convertMessageIfNecessary(message);

    final MessageProperties messageProperties = convertedMessage.getMessageProperties();

    // Add tracing header to outgoing AMQP message
    // so that new spans created on the AMQP message consumer side could be associated with span of current trace
    scope = RabbitMqTracingUtils.buildSendSpan(tracer, messageProperties);
    tracer.inject(
        scope.span().context(),
        Format.Builtin.TEXT_MAP,
        new RabbitMqInjectAdapter(messageProperties));

    // Add AMQP related tags to tracing span
    spanDecorator.onSend(messageProperties, exchange, routingKey, scope.span());

    return convertedMessage;
  }

  private Message convertMessageIfNecessary(final Object object) {
    if (object instanceof Message) {
      return (Message) object;
    }

    return messageConverter.toMessage(object, new MessageProperties());
  }

  public interface ProceedFunction {
    Object apply(Message t) throws Throwable;
  }

}
