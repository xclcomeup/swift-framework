package com.liepin.swift.framework.rpc.compile;

import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

/**
 * 方法注解适配器
 * 
 * @author yuanxl
 * 
 */
public class AnnotationMethodAdapter extends MethodAdapter {

    private int number;
    private final Set<Integer> methodNumbers;// 保留的方法编号集合

    public AnnotationMethodAdapter(final Set<Integer> methodNumbers, int number, MethodVisitor mv) {
        super(mv);
        this.number = number;
        this.methodNumbers = methodNumbers;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        // 包含指定注解的方法名
        if ("Lcom/liepin/swift/core/annotation/SwiftInterface;".equals(desc)) {
            methodNumbers.add(number);
        }
        return super.visitAnnotation(desc, visible);
    }

}
