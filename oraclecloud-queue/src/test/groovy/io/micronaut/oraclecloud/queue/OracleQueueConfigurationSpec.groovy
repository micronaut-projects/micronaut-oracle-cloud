package io.micronaut.oraclecloud.queue

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.oraclecloud.queue.OracleQueueConfiguration.QueueConfig
import spock.lang.Specification

class OracleQueueConfigurationSpec extends Specification {

    private static final String COMPARTMENT_OCID = 'ocid1.compartment.oc1..aaaaaaaaTESTING'
    private static final String NAMESPACE = 'NAME_SPACE'

    void 'valid config'() {
        given:
        int listenerActivationFrequencySeconds = 28

        String[] name = ['q1', 'q2']
        boolean[] enabled = [false, true]
        String[] ocid = ['ocid1.queue.oc1.iad.testingtesting_testQueue1',
                         'ocid1.queue.oc1.iad.testingtesting_testQueue2']
        boolean[] autoChannel = [false, true]

        boolean[] listenerEnabled = [false, true]
        String[] listenerExecutor = ['scheduled', 'custom']
        String[] listenerConcurrency = ['2-2', '3-3']
        int[] listenerActivationMultiple = [2, 22]
        int[] listenerMessageVisibilityExclusivitySeconds = [13, 14]
        boolean[] listenerLongPolling = [false, true]
        int[] listenerLongPollingSeconds = [20, 22]
        int[] listenerMaxMessagesPerActivation = [21, 23]

        when:
        var ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': listenerActivationFrequencySeconds,
                'oci.queue.namespace'                            : NAMESPACE,
                'oci.queue.queues'                               : [
                        [ocid          : ocid[0],
                         name          : name[0],
                         enabled       : enabled[0],
                         'auto-channel': autoChannel[0],
                         listener      : [enabled                                 : listenerEnabled[0],
                                          executor                                : listenerExecutor[0],
                                          concurrency                             : listenerConcurrency[0],
                                          'activation-multiple'                   : listenerActivationMultiple[0],
                                          'message-visibility-exclusivity-seconds': listenerMessageVisibilityExclusivitySeconds[0],
                                          'long-polling'                          : listenerLongPolling[0],
                                          'long-polling-seconds'                  : listenerLongPollingSeconds[0],
                                          'max-messages-per-activation'           : listenerMaxMessagesPerActivation[0]]
                        ],
                        [ocid          : ocid[1],
                         name          : name[1],
                         enabled       : enabled[1],
                         'auto-channel': autoChannel[1],
                         listener      : [enabled                                 : listenerEnabled[1],
                                          executor                                : listenerExecutor[1],
                                          concurrency                             : listenerConcurrency[1],
                                          'activation-multiple'                   : listenerActivationMultiple[1],
                                          'message-visibility-exclusivity-seconds': listenerMessageVisibilityExclusivitySeconds[1],
                                          'long-polling'                          : listenerLongPolling[1],
                                          'long-polling-seconds'                  : listenerLongPollingSeconds[1],
                                          'max-messages-per-activation'           : listenerMaxMessagesPerActivation[1]]
                        ]
                ]])
        var config = ctx.getBean(OracleQueueConfiguration)

        then:
        config.compartmentOcid == COMPARTMENT_OCID
        config.listenerActivationFrequencySeconds == listenerActivationFrequencySeconds
        config.namespace == NAMESPACE

        config.queues.size() == 2

        when:
        List<QueueConfig> queues = config.queues.sort { it.name }

        then:
        queues[0].name == name[0]
        queues[0].enabled == enabled[0]
        queues[0].ocid == ocid[0]
        queues[0].autoChannel == autoChannel[0]
        queues[0].listener.enabled == listenerEnabled[0]
        queues[0].listener.executor == listenerExecutor[0]
        queues[0].listener.concurrency == listenerConcurrency[0]
        queues[0].listener.activationMultiple == listenerActivationMultiple[0]
        queues[0].listener.messageVisibilityExclusivitySeconds == listenerMessageVisibilityExclusivitySeconds[0]
        queues[0].listener.longPolling == listenerLongPolling[0]
        queues[0].listener.longPollingSeconds == listenerLongPollingSeconds[0]
        queues[0].listener.maxMessagesPerActivation == listenerMaxMessagesPerActivation[0]

        queues[1].name == name[1]
        queues[1].enabled == enabled[1]
        queues[1].ocid == ocid[1]
        queues[1].autoChannel == autoChannel[1]
        queues[1].listener.enabled == listenerEnabled[1]
        queues[1].listener.executor == listenerExecutor[1]
        queues[1].listener.concurrency == listenerConcurrency[1]
        queues[1].listener.activationMultiple == listenerActivationMultiple[1]
        queues[1].listener.messageVisibilityExclusivitySeconds == listenerMessageVisibilityExclusivitySeconds[1]
        queues[1].listener.longPolling == listenerLongPolling[1]
        queues[1].listener.longPollingSeconds == listenerLongPollingSeconds[1]
        queues[1].listener.maxMessagesPerActivation == listenerMaxMessagesPerActivation[1]

        cleanup:
        ctx?.close()
    }

    void 'invalid configs'() {
        when:
        var ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': 0,
                'oci.queue.namespace'                            : NAMESPACE
        ])
        ctx.getBean OracleQueueConfiguration

        then:
        var e = thrown(BeanInstantiationException)
        e.message.contains 'listenerActivationFrequencySeconds - must be greater than or equal to 1'

        when:
        ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-initial-delay-seconds': 0,
                'oci.queue.namespace'                            : NAMESPACE
        ])
        ctx.getBean OracleQueueConfiguration

        then:
        e = thrown(BeanInstantiationException)
        e.message.contains 'listenerInitialDelaySeconds - must be greater than or equal to 1'

        when:
        ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': 25,
                'oci.queue.namespace'                            : NAMESPACE,
                'oci.queue.queues'                               : [
                        [ocid          : 'ocid',
                         name          : 'name',
                         enabled       : true,
                         'auto-channel': true,
                         listener      : ['activation-multiple': 0]]
                ]])
        ctx.getBean OracleQueueConfiguration

        then:
        e = thrown(BeanInstantiationException)
        e.message.contains 'activationMultiple - must be greater than or equal to 1'

        when:
        ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': 25,
                'oci.queue.namespace'                            : NAMESPACE,
                'oci.queue.queues'                               : [
                        [ocid          : 'ocid',
                         name          : 'name',
                         enabled       : true,
                         'auto-channel': true,
                         listener      : [
                                 'activation-multiple'                   : 1,
                                 'message-visibility-exclusivity-seconds': -1,
                                 'long-polling-seconds'                  : 5,
                                 'max-messages-per-activation'           : 1]
                        ]
                ]])
        ctx.getBean OracleQueueConfiguration

        then:
        e = thrown(BeanInstantiationException)
        e.message.contains 'messageVisibilityExclusivitySeconds - must be greater than or equal to 0'

        when:
        ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': 25,
                'oci.queue.namespace'                            : NAMESPACE,
                'oci.queue.queues'                               : [
                        [ocid          : 'ocid',
                         name          : 'name',
                         enabled       : true,
                         'auto-channel': true,
                         listener      : ['message-visibility-exclusivity-seconds': 50000]]
                ]])
        ctx.getBean OracleQueueConfiguration

        then:
        e = thrown(BeanInstantiationException)
        e.message.contains 'messageVisibilityExclusivitySeconds - must be less than or equal to 43200'

        when:
        ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': 25,
                'oci.queue.namespace'                            : NAMESPACE,
                'oci.queue.queues'                               : [
                        [ocid          : 'ocid',
                         name          : 'name',
                         enabled       : true,
                         'auto-channel': true,
                         listener      : ['long-polling-seconds': 1]
                        ]
                ]])
        ctx.getBean OracleQueueConfiguration

        then:
        e = thrown(BeanInstantiationException)
        e.message.contains 'longPollingSeconds - must be greater than or equal to 5'

        when:
        ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': 25,
                'oci.queue.namespace'                            : NAMESPACE,
                'oci.queue.queues'                               : [
                        [ocid          : 'ocid',
                         name          : 'name',
                         enabled       : true,
                         'auto-channel': true,
                         listener      : ['max-messages-per-activation': 0]]
                ]])
        ctx.getBean OracleQueueConfiguration

        then:
        e = thrown(BeanInstantiationException)
        e.message.contains 'maxMessagesPerActivation - must be greater than or equal to 1'

        when:
        ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': 25,
                'oci.queue.namespace'                            : NAMESPACE,
                'oci.queue.queues'                               : [
                        [ocid          : 'ocid',
                         name          : 'name',
                         enabled       : true,
                         'auto-channel': true,
                         listener      : ['max-messages-per-activation': 2000]]
                ]])
        ctx.getBean OracleQueueConfiguration

        then:
        e = thrown(BeanInstantiationException)
        e.message.contains 'maxMessagesPerActivation - must be less than or equal to 1000'

        when:
        ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': 25,
                'oci.queue.namespace'                            : NAMESPACE,
                'oci.queue.queues'                               : [
                        [ocid          : 'ocid',
                         name          : 'name',
                         enabled       : true,
                         'auto-channel': true,
                         listener      : [concurrency: 5]]
                ]])
        ctx.getBean OracleQueueConfiguration

        then:
        e = thrown(BeanInstantiationException)
        e.message.contains 'concurrency - must match "\\d+-\\d+"'

        when:
        ctx = ApplicationContext.run([
                'oci.queue.compartment-ocid'                     : COMPARTMENT_OCID,
                'oci.queue.listener-activation-frequency-seconds': 25,
                'oci.queue.namespace'                            : NAMESPACE,
                'oci.queue.queues'                               : [
                        [ocid          : 'ocid',
                         name          : 'name',
                         enabled       : true,
                         'auto-channel': true,
                         listener      : [concurrency: 'a-b']]
                ]])
        ctx.getBean OracleQueueConfiguration

        then:
        e = thrown(BeanInstantiationException)
        e.message.contains 'concurrency - must match "\\d+-\\d+"'

        cleanup:
        ctx?.close()
    }
}
