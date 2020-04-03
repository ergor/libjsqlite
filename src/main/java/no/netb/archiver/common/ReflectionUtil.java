package no.netb.archiver.common;

import no.netb.archiver.annotations.Db;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionUtil {

    public static Set<Field> getAllDbFields(Class<?> modelClass) {
        return getAllDbFields(modelClass, new HashSet<>());
    }

    private static Set<Field> getAllDbFields(Class<?> modelClass, Set<Field> collection) {
        if (modelClass == null) {
            return collection;
        }
        Field[] fields = modelClass.getDeclaredFields();
        collection.addAll(
                Arrays.stream(fields)
                        .filter(f -> f.isAnnotationPresent(Db.class))
                        .collect(Collectors.toSet())
        );
        return getAllDbFields(modelClass.getSuperclass(), collection);
    }
}
