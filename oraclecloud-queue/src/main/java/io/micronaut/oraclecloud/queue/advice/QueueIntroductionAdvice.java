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
package io.micronaut.oraclecloud.queue.advice;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.oraclecloud.queue.GenericQueue;
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration;
import io.micronaut.oraclecloud.queue.annotation.Queue;
import io.micronaut.oraclecloud.queue.service.QueueService;
import jakarta.inject.Singleton;

/**
 * Introduction advice for {@link Queue}.
 */
@Singleton
@InterceptorBean(Queue.class)
public class QueueIntroductionAdvice extends AbstractQueueIntroductionAdvice<Queue> {

    QueueIntroductionAdvice(QueueService service,
                            OracleQueueConfiguration config) {
        super(service, config, Queue.class);
    }

    @Override
    protected boolean isSupportedDeclaringType(Class<?> type) {
        return type.equals(GenericQueue.class);
    }
}
