/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.oraclecloud.queue.annotation;

import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.AliasFor;
import io.micronaut.messaging.annotation.MessageListener;
import io.micronaut.oraclecloud.queue.GenericQueueListener;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation for beans that implement {@link GenericQueueListener}, to create a bean and mix in
 * method handling for the interface.
 */
@Documented
@Retention(RUNTIME)
@Target({ANNOTATION_TYPE, TYPE})
@Introduction
@MessageListener
public @interface QueueListener {

    @AliasFor(annotation = QueueListener.class, member = "name") String value() default "";

    /**
     * The name of the queue as defined in application configuration.
     */
    String name() default "";

    /**
     * Optional channel that can be supplied to restrict this listener to only receive messages
     * from a particular channel id.
     */
    String channel() default "";

    /**
     * Whether to proceed with triggering the onMessageReceived() call if the message is expired
     * when the executor service invokes run().
     */
    boolean proceedIfExpired() default false;

    /**
     * Whether to proceed with triggering the onMessageReceived() call if the message is visible
     * to other consumers, i.e. the visibility lease exclusivity contract no longer applies
     * when the executor service invokes run().
     */
    boolean proceedIfVisible() default false;

    /**
     * At time of executor service run() invocation, whether to automatically extend the visibility
     * exclusivity lease contract of the message prior to triggering the onMessageReceived() call.
     * <p>
     * This setting is a convenience mechanism that is equivalent to calling
     * <pre>updateMessage(message, seconds)</pre> from <pre>onMessageReceived()</pre>.
     * <p>
     * Note that if the message is already visible to other consumers at the time of executor run,
     * no extension will take place and whether the onMessageReceive() call proceeds
     * is dictated by the proceedIfVisible() setting.
     * <p>
     * If the auto extend is triggered, the original GetMessage object will be replaced
     * by a clone reflecting the updated visible duration date.
     */
    boolean autoExtendLease() default false;

    /**
     * The number of seconds the visibility exclusivity lease should be requested, if the
     * autoExtendLease feature is enabled. This is equivalent to calling
     * <pre>updateMessage(message, seconds)</pre>.
     * <p>
     * The maximum duration is 43200 seconds - See
     * <a href="https://docs.oracle.com/en-us/iaas/api/#/en/queue/20210201/datatypes/UpdateMessagesDetailsEntry">the API documentation</a>.
     */
    int autoExtendLeaseSeconds() default 180;

    /**
     * Whether to automatically delete the message after an <pre>onMessageReceived()</pre> call.
     * Automatic deletion will only happen if <pre>onMessageReceived()</pre> is invoked, no
     * exception is thrown, and the visible duration date of the message as originally received
     * still represents an exclusive lease.
     * autoExtendLease is considered in such a scenario when equating the visibility exclusivity.
     */
    boolean autoDelete() default false;
}
