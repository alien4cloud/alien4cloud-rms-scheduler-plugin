#/ result usage
[then]Log "{message}"=System.out.println("{message}");
[then]Schedule a trigger with id "{ruleId}" for environment "{environmentId}" deployment "{deploymentId}" running "{workflowName}" using a TTL of {ttlDuration}=insert(new RuleTrigger("{ruleId}", "{environmentId}", "{deploymentId}", "{workflowName}", "{ttlDuration}"));
[when]A scheduled non expired trigger exists with id {ruleId}=$tickTocker: TickTocker(); r: RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.SCHEDULED, scheduleTime <= $tickTocker.now, expirationTime > $tickTocker.now)
[when]A trigger in {triggerStatus} exists with id {ruleId}=r: RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.{triggerStatus})
[when]No running execution with id {ruleId}=not RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.RUNNING)
[when]No scheduled trigger exists with id {ruleId}=not RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.SCHEDULED)
[then]Fire the trigger=r.setStatus(RuleTriggerStatus.TRIGGERED);update(r);
[then]Reschedule the trigger in {rescheduleDelay}=r.reschedule("{rescheduleDelay}");update(r);
[when]I've got a recent value for metric "{metric_label}"=MetricEvent(label == "{metric_label}")
[when]Last known metric "{metric_label}" is {operator} {metric_value}=
Number( doubleValue {operator} {metric_value} ) from accumulate
(
    MetricEvent(label == "{metric_label}", $value : value) over window:length(1),
    average($value)
)
[when]Average value for metric "{metric_label}" during last {window_time} is {operator} {metric_value}=
Number( doubleValue > {metric_value} ) from accumulate
(
    MetricEvent(label == "{metric_label}", $value : value) over window:time({window_time}),
    average($value)
)
