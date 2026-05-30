package com.betclick.config;

public class DataSourceContextHolder {

    public enum DataSourceType {
        RUNTIME,
        EMPLOYEE
    }

    private static final ThreadLocal<DataSourceType> CONTEXT = ThreadLocal.withInitial(() -> DataSourceType.RUNTIME);

    public static void set(DataSourceType type) {
        CONTEXT.set(type);
    }

    public static DataSourceType get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
