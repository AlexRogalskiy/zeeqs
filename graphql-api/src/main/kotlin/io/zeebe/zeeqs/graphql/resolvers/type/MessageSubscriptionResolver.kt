package io.zeebe.zeeqs.data.resolvers

import graphql.kickstart.tools.GraphQLResolver
import io.zeebe.zeeqs.data.entity.*
import io.zeebe.zeeqs.data.repository.ElementInstanceRepository
import io.zeebe.zeeqs.data.repository.MessageCorrelationRepository
import io.zeebe.zeeqs.data.repository.ProcessInstanceRepository
import io.zeebe.zeeqs.data.repository.ProcessRepository
import io.zeebe.zeeqs.graphql.resolvers.type.BpmnElement
import io.zeebe.zeeqs.graphql.resolvers.type.ResolverExtension
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class MessageSubscriptionResolver(
        val processInstanceRepository: ProcessInstanceRepository,
        val elementInstanceRepository: ElementInstanceRepository,
        val processRepository: ProcessRepository,
        val messageCorrelationRepository: MessageCorrelationRepository
) : GraphQLResolver<MessageSubscription> {

    fun timestamp(messageSubscription: MessageSubscription, zoneId: String): String? {
        return messageSubscription.timestamp.let { ResolverExtension.timestampToString(it, zoneId) }
    }

    fun processInstance(messageSubscription: MessageSubscription): ProcessInstance? {
        return messageSubscription.processInstanceKey?.let { processInstanceRepository.findByIdOrNull(it) }
    }

    fun elementInstance(messageSubscription: MessageSubscription): ElementInstance? {
        return messageSubscription.elementInstanceKey?.let { elementInstanceRepository.findByIdOrNull(it) }
    }

    fun process(messageSubscription: MessageSubscription): Process? {
        return messageSubscription.processDefinitionKey?.let { processRepository.findByIdOrNull(it) }
                ?: messageSubscription.processInstanceKey?.let {
                    processInstanceRepository.findByIdOrNull(it)
                            ?.processDefinitionKey.let { processRepository.findByIdOrNull(it) }
                }
    }

    fun messageCorrelations(messageSubscription: MessageSubscription): List<MessageCorrelation> {
        return messageSubscription.elementInstanceKey
                ?.let {
                    messageCorrelationRepository.findByMessageNameAndElementInstanceKey(
                            messageName = messageSubscription.messageName,
                            elementInstanceKey = it)
                }
                ?: messageSubscription.processDefinitionKey?.let {
                    messageCorrelationRepository.findByMessageNameAndProcessDefinitionKey(
                            messageName = messageSubscription.messageName,
                            processDefinitionKey = it
                    )
                }
                ?: emptyList()
    }

    fun element(messageSubscription: MessageSubscription): BpmnElement? {
        val processDefinitionKeyOfSubscription = messageSubscription.processDefinitionKey
                ?: processInstanceRepository
                        .findByIdOrNull(messageSubscription.processInstanceKey)
                        ?.processDefinitionKey

        return processDefinitionKeyOfSubscription?.let { processDefinitionKey ->
            messageSubscription.elementId?.let { elementId ->
                BpmnElement(
                        processDefinitionKey = processDefinitionKey,
                        elementId = elementId
                )
            }
        }
    }

}