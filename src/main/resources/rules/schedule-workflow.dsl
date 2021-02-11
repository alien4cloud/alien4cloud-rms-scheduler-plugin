[then]Log "{message}"=System.out.println("{message}");
[then][]Schedule a trigger with id "{ruleId}" for environment "{environmentId}" deployment "{deploymentId}" running "{workflowName}" using a TTL of {ttlDuration} {ttlField}=insert(new RuleTrigger("{ruleId}", "{environmentId}", "{deploymentId}", "{workflowName}", Calendar.MINUTE, 1));
[when][]A scheduled non expired trigger exists with id {ruleId}=$tickTocker: TickTocker(); r: RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.SCHEDULED, expirationTime > $tickTocker.now)
[when][]A trigger in error exists with id {ruleId}=r: RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.ERROR)
[when][]No running execution with id {ruleId}=not RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.RUNNING)
[then]Fire the trigger=r.setStatus(RuleTriggerStatus.TRIGGERED);update(r);
[then]Reschedule the trigger=r.setStatus(RuleTriggerStatus.SCHEDULED);update(r);
[when][]I've got a recent value for metric "{metric_label}"=MetricEvent(label == "{metric_label}")
[when][]Last known metric "{metric_label}" is {operator} {metric_value}=
Number( doubleValue {operator} {metric_value} ) from accumulate
(
    MetricEvent(label == "{metric_label}", $value : value) over window:length(1),
    average($value)
)
