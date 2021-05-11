[when]less than=<
[when]The load average of the system is {operator} {metric_value}=
Number( doubleValue {operator} {metric_value} ) from accumulate
(
    MetricEvent(label == "Load_Average", $value : doubleValue) over window:length(1),
    average($value)
)
