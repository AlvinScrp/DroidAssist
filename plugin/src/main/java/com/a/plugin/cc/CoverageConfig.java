package com.a.plugin.cc;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class CoverageConfig {

    public static Set<String> include = new HashSet<>();
    public static Set<String> exclude = new HashSet<>();

    static {
        include.add("com.webuy.*");
        include.add(".*MainActivity");

        String[] excludeArray = {".*databinding.*Binding",
                ".*databinding.*BindingImpl",
                ".*.R",
                ".*.R\\$.*",
                ".*BuildConfig.*",
                ".*Manifest.*",
                ".*DataBinderMapperImpl",
                ".*DataBindingTriggerClass",
                ".*ViewInjector.*",
                ".*ViewBinder.*",
                ".*BuildConfig.*",
                ".*BR",
                ".*Manifest*.*",
                ".*_Factory.*",
                ".*_Provide.*Factory.*"};
        for (String s : excludeArray) {
            exclude.add(s);
        }
    }

    public static boolean matches(String className) {
      boolean isInclude =   include.stream().anyMatch(s -> Pattern.matches(s,className));
      boolean isExclude = exclude.stream().anyMatch(s -> Pattern.matches(s,className));
      boolean match = isInclude &&  !isExclude;
      return  match;
    }

}
