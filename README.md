# alien4cloud-rms-scheduler-plugin
A rule based scheduler embedding drools.

A runtime policy of `RMSScheduleWorkflowPolicy` type is defined in the embeded [csar](src/main/resources/csar/tosca.yml).


# Config

This plugin can be tuned by setting following properties in A4C YAML configuration 
(prefix is `alien4cloud-rms-scheduler-plugin`) :

Property name | default | description 
------------ | -------------| -------------
metricEventTtl | 10m | The TTL for events : you should set this config regarding to the frequency with witch you expect to receive events in your loopback.
heartbeatPeriod | 60 | In seconds, the period between 2 rule fires. A heartbeat will fire all rules on each session (1 session per active deployments).

For tests purposes you should reduce `heartbeatPeriod` in order to make the system more reactive.

```
alien4cloud-rms-scheduler-plugin:
  metricEventTtl: 10m
  heartbeatPeriod: 1
```

# How-to

Add the **RMS Scheduler Modifier** to your location at **post-policy-match** phase.
Create a topology using simple mocks and add one or several **RMSScheduleWorkflowPolicy** to your topology.

The following policy config example will trigger the run workflow (you need a run workflow !) each hour at 0 and 30 minutes with a TTL of 10 minutes (if workflow fails, it will be retried during 10 minutes) :

Property name | value
------------ | -------------
cron_expression | 0 0/30 * * * ?
timer_type | cron
workflow_name | run
duration | 10m
retry_on_error | true

This other example will trigger run workflow after a 10 minutes delay (after the deployment, or system start), if conditions are satisfied (with temporal window of 2 minutes) :

Property name | value
------------ | -------------
cron_expression | 10m
timer_type | int
workflow_name | run
duration | 2m
conditions[0] | I've got a recent value for metric "ES_Disk_Free"
conditions[1] | Last known metric "ES_Disk_Free" is >= 10000

Condition are :
- **ES_Disk_Free** metrics exists in the system
- **ES_Disk_Free >= 10000** (let's say it's Go so 10 To)

You can publish an event of type **ES_Disk_Free** using the following REST query :
```
curl -X PUT http://localhost:8088/rest/rmsscheduler/events/publish/ES_Disk_Free/20000
```

Events are timestamped and have a TTL of 5m (TODO: make plugin configurable)

Another example condition could also be `Average value for metric "ES_Disk_Free" during last 10m is > 10000`.

## Loops

In the example policy configuration below, if conditions are met, the policy will loop during 3m, with a delay between each triggers of 45s. 
 :

```
timer_type: cron
cron_expression: "0 0/5 * * * ?"
duration: 3m
loop: true
reschedule_delay: 45s
only_one_running: true
conditions: 
 - "Average value for metric \"ES_Disk_Free\" during last 10m is > 10000"
workflow_name: run
```

# 


# TODO

- DSL editor
- Manage deployment update
- Manage downtime ? 
- Manage workflow inputs

## Ideas

Ability to cancel a workflow when it's expiration occurs ?
How-to retry after a given period in case of error ?
Number of retry ? -1 = infinite

Should be possible to launch a workflow during the time window even if SUCESS

reschedule_delay
    int expression
    the delay beetween reschedules (after error or if looping)
loop
    boolean
    if true will loop the action during time window
