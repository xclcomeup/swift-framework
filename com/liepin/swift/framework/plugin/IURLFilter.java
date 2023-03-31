package com.liepin.swift.framework.plugin;

import java.net.URL;

public interface IURLFilter extends IScanFilter {

    boolean test(URL url);
    
    String path();
    
    String suffix();

}
