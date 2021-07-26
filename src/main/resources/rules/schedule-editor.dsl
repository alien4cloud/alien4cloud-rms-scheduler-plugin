#/ result usage
# Here you can enrich the builtin DSL in order to provide your own DSL sentences
# See https://docs.jboss.org/drools/release/7.49.0.Final/drools-docs/html_single/index.html#_domain_specific_languages

# Here are commented french translations for builtin sentences
#[when]J'ai une mesure "{metric_label}" récente=MetricEvent(label == "{metric_label}")
#[when]Le dernier "{metric_label}" connu est {operator} à {metric_value}=
#Number( doubleValue {operator} {metric_value} ) from accumulate
#(
#    MetricEvent(label == "{metric_label}", $value : doubleValue) over window:length(1),
#    average($value)
#)
