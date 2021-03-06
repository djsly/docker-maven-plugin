package com.alexecollins.docker.model;

public class Id {
    private final String value;

    public Id(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Id id = (Id) o;

        return value.equals(id.value);

    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
