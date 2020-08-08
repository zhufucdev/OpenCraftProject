package ss

import java.lang.reflect.Method

trait Constructor <T> {
    abstract void apply()
    abstract void unapply()
    void merge(T other) {
        other.properties.forEach { String n, v ->
            if (v != null && v !instanceof Method && n != 'class')
                this[n] = v
        }
    }
}