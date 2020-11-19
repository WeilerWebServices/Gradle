package org.gradle.playframework.tools.internal.reflection;

/**
 * Thrown when a requested property cannot be found.
 */
public class NoSuchPropertyException extends RuntimeException {
    public NoSuchPropertyException(String message) {
        super(message);
    }
}
