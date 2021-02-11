# alien4cloud-rms-scheduler-plugin
A rule based scheduler embedding drools.

# How-to

Add the **RMS Scheduler Modifier** to your location at **post-policy-match** phase.
Create a topology using simple mocks and add one or several **RMSScheduleWorkflowPolicy** to your topology.

The following policy config example will trigger the run workflow (you need a run workflow !) each hour at 0 and 30 minutes with a TTL of 10 minutes (if workflow fails, it will be retried during 10 minutes) :

Property name | value
------------ | -------------
cron_expression | 0 0/30 * * * ?
timer_type | cron
workflow_name | run
ttl | 10
ttl_unit | MINUTE
retry_on_error | true

This other example will trigger run workflow after a 10 minutes delay (after the deployment, or system start), if conditions are satisfied (with temporal window of 2 minutes) :

Property name | value
------------ | -------------
cron_expression | 10m
timer_type | int
workflow_name | run
ttl | 2
ttl_unit | MINUTE
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

# TODO

- Plugin configuration (metric event TTL, heart beat period ...)
- DSL editor
- Manage deployment update
- Manage downtime ? 
- Manage workflow inputs

## Ideas

Ability to cancel a workflow when it's expiration occurs ?
How-to retry after a given period in case of error ?
Number of retry ? -1 = infinite

