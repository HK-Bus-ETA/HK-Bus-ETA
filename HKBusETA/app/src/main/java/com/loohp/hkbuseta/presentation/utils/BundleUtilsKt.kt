package com.loohp.hkbuseta.presentation.utils

import android.os.Bundle


fun Bundle.isEqualTo(other: Bundle?): Boolean {
    if (other == null) {
        return false
    }

    if (this.size() != other.size()) {
        return false
    }

    if (!this.keySet().containsAll(other.keySet())) {
        return false
    }

    for (key in this.keySet()) {
        val valueOne = this.get(key)
        val valueTwo = other.get(key)
        if (valueOne is Bundle && valueTwo is Bundle) {
            if (!valueOne.isEqualTo(valueTwo)) {
                return false
            }
        } else if (valueOne != valueTwo) {
            return false
        }
    }

    return true
}