package com.liepin.swift.framework.contracts;

import java.util.ArrayList;
import java.util.List;

import com.liepin.swift.framework.contracts.impl.ConfigContract;
import com.liepin.swift.framework.contracts.impl.JarContract;
import com.liepin.swift.framework.contracts.impl.NonParentReferenceContract;
import com.liepin.swift.framework.contracts.impl.SelfReferenceContract;
import com.liepin.swift.framework.contracts.impl.SystemEnvContract;

/**
 * 检查校验上下文管理
 * 
 * @author yuanxl
 *
 */
public final class ContractContext {

    private static final List<IContract> REGISTERS = new ArrayList<IContract>();

    static {
        REGISTERS.add(new SystemEnvContract());
        REGISTERS.add(new JarContract());
        REGISTERS.add(new ConfigContract());
        REGISTERS.add(new SelfReferenceContract());
        REGISTERS.add(new NonParentReferenceContract());
    }

    public static void initialize() {
        for (IContract contract : REGISTERS) {
            contract.review();
        }
    }

}
