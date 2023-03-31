package com.liepin.swift.framework.limit.rules.processor.fallback2s;

import java.lang.reflect.Method;

import com.liepin.swift.framework.limit.BlockException;

public class FallbackHandler {

    private final Object instance;
    private final Method method;

    public FallbackHandler(Object instance, Method method) {
        this.instance = instance;
        this.method = method;
    }

    public Object handle(BlockException e, Object[] args) throws Exception {
        Object[] newArgs = new Object[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[newArgs.length - 1] = e;
        return method.invoke(instance, newArgs);
    }

    @Override
    public String toString() {
        return instance.getClass().getName() + "@" + method.getName();
    }

}
