package com.liepin.swift.framework.limit.rules;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.liepin.swift.framework.limit.LimitContext;
import com.liepin.swift.framework.limit.config.controll.LimitControll;

@SuppressWarnings("rawtypes")
public class Bridge {

    public static volatile List<RuleProcessor> processors = new ArrayList<RuleProcessor>();

    public static List<RuleProcessor> lookup(LimitContext limitContext) {
        if (processors.isEmpty()) {
            return null;
        }
        return processors.stream().filter(t -> t.contain(limitContext)).findFirst().isPresent() ? processors : null;
    }

    @SuppressWarnings("unchecked")
    public static synchronized void refresh(List<LimitControll> configs) {
        if (processors.isEmpty()) {
            // 初始化
            if (configs.size() > 0) {
                // 第一次加载
                List<RuleProcessor> list = RuleProcessor.build();
                list.forEach(t -> {
                    // configs.forEach(c -> {
                    // t.updateRuleConfig(c);
                    // });
                    configs.stream().filter(c -> match(t, c)).forEach(t::updateRuleConfig);
                });
                processors = list;
            }
        } else {
            // 动态更新
            if (configs.size() > 0) {
                // 修改
                processors.forEach(t -> {
                    // configs.forEach(c -> {
                    // t.updateRuleConfig(c);
                    // });
                    configs.stream().filter(c -> match(t, c)).forEach(t::updateRuleConfig);
                });
            } else {
                // 删除
                List<RuleProcessor> temp = processors;
                processors = new ArrayList<>();
                temp.forEach(t -> t.clear());
            }
        }
    }

    public static boolean match(RuleProcessor ruleProcessor, LimitControll limitControll) {
        Type type = ((ParameterizedType) ruleProcessor.getClass().getGenericInterfaces()[0])
                .getActualTypeArguments()[0];
        return type == limitControll.getClass();
    }

}
