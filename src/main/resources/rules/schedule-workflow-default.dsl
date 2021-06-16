#/ result usage
# Default condition statements DSL
[when]I've got a recent value for metric "{metric_label}"=MetricEvent(label == "{metric_label}")
[when]Last known metric "{metric_label}" is {operator} {metric_value}=
Number( doubleValue {operator} {metric_value} ) from accumulate
(
    MetricEvent(label == "{metric_label}", $value : doubleValue) over window:length(1),
    average($value)
)
[when]Average value for metric "{metric_label}" during last {window_time} is {operator} {metric_value}=
Number( doubleValue > {metric_value} ) from accumulate
(
    MetricEvent(label == "{metric_label}", $value : doubleValue) over window:time({window_time}),
    average($value)
)
