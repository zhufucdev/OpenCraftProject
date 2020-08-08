package ss

import java.lang.reflect.Method

trait ResultConstructor<T, R> {
    abstract R construct()
    void merge(T other) {
        other.properties.forEach { String n, v ->
            if (v != null && v !instanceof Method && n != 'class')
                this[n] = v
        }
    }
}