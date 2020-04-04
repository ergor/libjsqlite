package no.netb.libjsqlite.annotations;

import no.netb.libjsqlite.BaseModel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in a class as a foreign key.
 * Used in addition to {@link Db} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Fk {

    Class<?extends BaseModel> value();
}
