package com.pipeforge.execution.dto;

import com.pipeforge.execution.entity.TriggerType;

/**
 * Optional body when triggering a pipeline. {@code triggerType} defaults to
 * {@code MANUAL} when omitted.
 */
public record TriggerExecutionRequest(TriggerType triggerType) {
}
