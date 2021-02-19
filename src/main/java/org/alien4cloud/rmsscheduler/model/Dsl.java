package org.alien4cloud.rmsscheduler.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

@Getter
@Setter
@ToString
@ESObject
@NoArgsConstructor
public class Dsl {

    @Id
    private String id;

    private String content;

    /**
     * Each DSL sentence should have a DSLR validation statement.
     */
    //private String dslr;

}
