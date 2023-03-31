package com.liepin.swift.framework.rpc.compile;

import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * 类方法收集适配器
 * 
 * @author yuanxl
 * 
 */
public class MethodCollectClassAdapter extends ClassAdapter {

    private final Set<Integer> methodNumbers;

    private int num = 0;

    public MethodCollectClassAdapter(final Set<Integer> methodNumbers, ClassVisitor cv) {
        super(cv);
        this.methodNumbers = methodNumbers;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
            final String[] exceptions) {
        MethodVisitor visitMethod = super.visitMethod(access, name, desc, signature, exceptions);
        AnnotationMethodAdapter annotationAdapter = new AnnotationMethodAdapter(methodNumbers, ++num, visitMethod);
        return annotationAdapter;
    }

}
