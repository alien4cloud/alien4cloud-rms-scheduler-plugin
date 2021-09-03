#/ result usage
# Used by builtin rules
[then]Log "{message}"=System.out.println("{message}");
[then]Schedule a trigger with id "{ruleId}" for environment "{environmentId}" deployment "{deploymentId}" running "{workflowName}" using a TTL of {ttlDuration} and a max of {maxRun} having {conditionsCount} conditions=insert(new RuleTrigger("{ruleId}", "{environmentId}", "{deploymentId}", "{workflowName}", {ttlDuration}, {maxRun}, {conditionsCount}));
[when]A scheduled non expired trigger exists with id {ruleId}=$tickTocker: TickTocker(); r: RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.SCHEDULED, scheduleTime <= $tickTocker.now, expirationDelay == 0 || expirationTime > $tickTocker.now)
[when]A trigger exists with id {ruleId}=$tickTocker: TickTocker(); rt: RuleTrigger(ruleId == "{ruleId}", scheduleTime <= $tickTocker.now, expirationDelay == 0 || expirationTime > $tickTocker.now)
[when]A scheduled no window trigger exists with id {ruleId}=$tickTocker: TickTocker(); r: RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.SCHEDULED, scheduleTime <= $tickTocker.now, expirationDelay == 0)
[when]A expired trigger exists with id {ruleId}=$tickTocker: TickTocker(); $r: RuleTrigger(ruleId == "{ruleId}", expirationDelay > 0 && $tickTocker.now > expirationTime)
[when]A running action exists for current trigger=$a: TimelineAction(triggerId == $r.id, state == TimelineActionState.RUNNING)
[when]A trigger with window in {triggerStatus} exists with id {ruleId}=r: RuleTrigger(ruleId == "{ruleId}", status == RuleTriggerStatus.TRIGGERED, expirationDelay > 0); a: TimelineAction(triggerId == r.id, state == TimelineActionState.{triggerStatus})
[when]A trigger heartbeat without remaining conditions exists for rule "{ruleId}"=$t: TickTocker(); h: RuleTriggerHeartbeat(ruleId == "{ruleId}", id == $t.now.time, remainingConditionsCount == 0)
[when]A trigger heartbeat exists for rule "{ruleId}"=$tickTocker: TickTocker(); h: RuleTriggerHeartbeat(ruleId == "{ruleId}", id == $tickTocker.now.time)
[when]No running execution with id {ruleId}=not TimelineAction(ruleId == "{ruleId}", state == TimelineActionState.RUNNING)
[when]No failed condition event exists=not ConditionEvent(triggerId == h.getTriggerId(), heartbeatId == h.getId(), passed == false)
[when]No condition event for condition #{conditionIdx} exists=not ConditionEvent(triggerId == h.getTriggerId(), heartbeatId == h.getId(), conditionIdx == {conditionIdx})
[when]We detect several condition events for condition #{conditionIdx}=
$conditionEvents : List( size > 0 )
    from collect( ConditionEvent( triggerId == h.getTriggerId(), conditionIdx == {conditionIdx} ) )
[when]Condition event passed changed detected=eval($conditionEvents.size() == 1 || ((ConditionEvent)$conditionEvents.get($conditionEvents.size()-1)).isPassed() != ((ConditionEvent)$conditionEvents.get($conditionEvents.size()-2)).isPassed())
[then]Decrement the trigger remaining conditions=h.decrementRemainingConditions();update(h);
[then]Insert a new trigger heartbeat=insert(new RuleTriggerHeartbeat($tickTocker.getNow().getTime(), r.getId(), r.getRuleId(), r.getDeploymentId(), r.getConditionsCount()));
[then]Fire a condition event for condition #{conditionIdx} with value {value}=insert(new ConditionEvent(h.getTriggerId(), h.getId(), {conditionIdx}, {value}));
[then]Fire an event for condition #{conditionIdx}=insert(new TimelineRuleConditionEvent(h.getDeploymentId(), h.getRuleId(), (((ConditionEvent)$conditionEvents.get($conditionEvents.size()-1)).isPassed()), {conditionIdx}-1));
[then]Timeout the action=$a.setState(TimelineActionState.TIMEOUT);update($a);
[then]Fire the trigger=r.activate();update(r);
[then]Delete the trigger=delete(r);
[then]Reschedule the trigger in {delay}=r.reschedule({delay});update(r);
#[then]Iterate over condition events=System.out.println($conditionEvents);
#[then]Log condition event passed changed=System.out.println("Condition event " + ((ConditionEvent)$conditionEvents.get($conditionEvents.size()-1)).getConditionIdx() + " passed is now " + ((ConditionEvent)$conditionEvents.get($conditionEvents.size()-1)).isPassed());
[then]Fire a fake condition event=insert(new ConditionEvent(((ConditionEvent)$conditionEvents.get($conditionEvents.size()-1)).getTriggerId(), ((ConditionEvent)$conditionEvents.get($conditionEvents.size()-1)).getHeartbeatId() + 1, ((ConditionEvent)$conditionEvents.get($conditionEvents.size()-1)).getConditionIdx(), ((ConditionEvent)$conditionEvents.get($conditionEvents.size()-1)).isPassed()));
