package no.netb.archiver.annotations;

import no.netb.archiver.models.ModelBase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface Models {
    Class<? extends ModelBase>[] models();
}
