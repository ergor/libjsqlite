package no.netb.libjsqlite;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReflectionAccess implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(ReflectionAccess.class.getName());

    private final Field field;

    private ReflectionAccess(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    public static ReflectionAccess grant(Field field) {
        return new ReflectionAccess(field);
    }

    @Override
    public void close() {
        field.setAccessible(false);
    }

    public void setFieldValue(Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) { // this should never happen
            LOG.log(Level.SEVERE, "jsqlite: field access threw IllegalAccessException even while under ReflectionAccess", e);
            throw new RuntimeException(e);
        }
    }

    public Object getFieldValue(Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) { // this should never happen
            LOG.log(Level.SEVERE, "jsqlite: field access threw IllegalAccessException even while under ReflectionAccess", e);
            throw new RuntimeException(e);
        }
    }
}
