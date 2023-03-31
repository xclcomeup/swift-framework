package com.liepin.swift.framework.limit.rules.chain;

import com.liepin.swift.framework.limit.BlockException;

public abstract class ProcessorContext implements RuleProcessorChain {

    protected BlockException blockException;
    protected Object fallbackObject;

    public BlockException getBlockException() {
        return blockException;
    }

    public Object getFallbackObject() {
        return fallbackObject;
    }

    public void setBlockException(BlockException blockException) {
        this.blockException = blockException;
    }

    public void setObject(Object object) {
        this.fallbackObject = object;
        this.blockException = null;
    }

    public void releaseBlockException() throws BlockException {
        if (blockException != null) {
            throw blockException;
        }
    }

}
