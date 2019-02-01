package org.terasology.module.sandbox;

import java.security.Permission;
import java.util.function.Predicate;

public class PredicatePermissionSet implements PermissionProvider {

    private final Predicate<Class<?>> predicate;

    public PredicatePermissionSet(Predicate<Class<?>> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean isPermitted(Class<?> type) {
        return predicate.test(type);
    }

    @Override
    public boolean isPermitted(Permission permission, Class<?> context) {
        return false;
    }
}
