package org.dizitart.no2.objects.data;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Anindya Chatterjee.
 */
@Getter
@Setter
public class WithCustomConstructor {
    private String name;
    private long number;

    public WithCustomConstructor(String name, long number) {
        this.name = name;
        this.number = number;
    }
}
