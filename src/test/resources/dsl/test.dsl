[when]J'ai une mesure "{metric_label}" récente=MetricEvent(label == "{metric_label}")
[when]Blabla=MetricEvent(label == "")
[when]less than=<
[when]The load average of the system is {operator} {metric_value}=
Number( doubleValue {operator} {metric_value} ) from accumulate
(
    MetricEvent(label == "Load_Average", $value : value) over window:length(1),
    average($value)
)
