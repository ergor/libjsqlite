package no.netb.libjsqlite;

import no.netb.libjsqlite.annotations.Db;
import no.netb.libjsqlite.annotations.Fk;
import no.netb.libjsqlite.annotations.Pk;

import java.lang.reflect.Field;

import static no.netb.libjsqlite.TypeMapping.AbstractType;

public class Column {

    private final Field field;

    public Column(Field field) {
        this.field = field;
    }

    public Field getField() {
        return field;
    }

    public void setField(Object modelInstance, Object value) throws IllegalAccessException {
        this.field.set(modelInstance, value);
    }

    private String formatColumnName(String name) {
        return String.format("\"%s\"", name);
    }

    public String getColumnNameForQuery() {
        return formatColumnName(field.getName());
    }

    public String getFkNameForQuery() {
        return formatColumnName(field.getAnnotation(Fk.class).model().getSimpleName());
    }

    /**
     * @throws IllegalStateException If no mapping for type found
     */
    public AbstractType getAbstractTypeOrFail() {
        return TypeMapping.toAbstractType(field)
                .orElseThrow(() -> new IllegalStateException("Column.java: field types exhausted. No mapping for: " + field.getType().getName()));
    }

    public String getValueForQuery(Object modelInstance) {
        AbstractType abstractType =  getAbstractTypeOrFail();

        String valueForSql;
        try (ReflectionAccess access = ReflectionAccess.grant(this.field)) {
            valueForSql = abstractType.mapValueForQuery(access.getFieldValue(modelInstance));
        }

        return valueForSql;
    }

    public void setFieldAccesible(boolean isAccesible) {
        field.setAccessible(isAccesible);
    }

    public boolean isPrimaryKey() {
        return field.isAnnotationPresent(Pk.class);
    }

    public boolean isForeignKey() {
        return field.isAnnotationPresent(Fk.class);
    }

    public boolean isNullable() {
        return field.getAnnotation(Db.class).nullable();
    }
}
