package com.liepin.swift.framework.rpc.compile;

import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * 删除接口适配器
 * 
 * @author yuanxl
 * 
 */
public class MethodDeleteClassAdapter extends ClassAdapter {

    /**
     * 需要保留的方法编号集合
     */
    private final Set<Integer> methodNumbers;

    private int number = 0;

    public MethodDeleteClassAdapter(final Set<Integer> methodNumbers, ClassVisitor cv) {
        super(cv);
        this.methodNumbers = methodNumbers;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
            final String[] exceptions) {
        if (!methodNumbers.contains(++number)) {
            return null;
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

}
