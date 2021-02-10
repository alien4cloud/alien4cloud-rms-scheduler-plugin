
[when][]J'ai une mesure "{metric_label}" récente=MetricEvent(label == "{metric_label}")

[when][]J'ai une mesure "{metric_label}" récente=MetricEvent(label == "{metric_label}")

[when][]Le dernier "{metric_label}" connu est {operator} à {metric_value}=
Number( doubleValue {operator} {metric_value} ) from accumulate
(
    MetricEvent(label == "{metric_label}", $value : value) over window:length(1),
    average($value)
)

[when][]La charge moyenne du cluster K8S est {operator} à {metric_value}=
    (
        MetricEvent(label == "K8S_Load_Average")
    )
    and
    (
        Number( doubleValue {operator} {metric_value} ) from accumulate
            (
                MetricEvent(label == "K8S_Load_Average", $value : value) over window:length(1),
                average($value)
            )
    )

[then]Log "{message}"=System.out.println("{message}");

[then][]Schedule a trigger with id "{ruleId}" for environment "{environmentId}" deployment "{deploymentId}" running "{workflowName}" using a TTL of {ttlDuration} {ttlField}=insert(new RuleTrigger("{ruleId}", "{environmentId}", "{deploymentId}", "{workflowName}", Calendar.MINUTE, 1));
[when][]A scheduled non expired trigger exists with id {ruleId}=$tickTocker: TickTocker(); r: RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.SCHEDULED, expirationTime > $tickTocker.now)
[when][]A trigger in error exists with id {ruleId}=r: RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.ERROR)
[when][]No running execution with id {ruleId}=not RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.RUNNING)
[then]Fire the trigger=r.setStatus(RuleTriggerStatus.TRIGGERED);update(r);
[then]Reschedule the trigger=r.setStatus(RuleTriggerStatus.SCHEDULED);update(r);
