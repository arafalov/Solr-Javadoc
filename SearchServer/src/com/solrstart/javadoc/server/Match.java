package com.solrstart.javadoc.server;

import org.apache.solr.client.solrj.beans.Field;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Created by arafalov on 4/18/14.
 */
public class Match {
    @Field private final String id;
    @Field private final String type;
    @Field private final String packageName;
    @Field private final String className;
    @Field private final String sourceClassName;
    @Field private final String methodName;
    @Field private final String description;


    private String htmlDescription;

    public Match(String id, String type, String packageName, String className, String sourceClassName,  String methodName, String description)
    {
        this.id = id;
        this.type = type;
        this.packageName = packageName;
        this.className = className;
        this.sourceClassName = sourceClassName;
        this.methodName = methodName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getSourceClassName() {
        return sourceClassName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescription() {
        return description;
    }

    public String getHtmlDescription() {
        return htmlDescription;
    }

    //TODO: This should be possible to do automatically, since Spring somehow knows how to map!
    public void createHTMLDescription(Map<String, String> overrides)
    {
        String result = replace(description, "packageName", overrides.get("packageName"), getPackageName());
        result = replace(result, "className", overrides.get("className"), getClassName());
        result = replace(result, "sourceClassName", overrides.get("sourceClassName"), getSourceClassName());
        result = replace(result, "methodName", overrides.get("methodName"), getMethodName());
        this.htmlDescription = result;
        System.out.println("htmlDescription: " + htmlDescription);
    }

    /**
     * Replace ${variable} name in pattern with whichever value is provided first
     * @param pattern with variable names in them
     * @param varName the variable name to replace
     * @param bestValue best value (could be null)
     * @param defaultValue default value (potentially could also be null)
     * @return html string with variables substituted
     */
    private String replace(String pattern, String varName, String bestValue, String defaultValue) {
        String replaceValue = (bestValue != null)
                ?bestValue
                :(defaultValue!=null)
                    ?defaultValue
                :null;
        return (replaceValue==null)
                ? pattern
                : pattern.replace("${" + varName + "}", replaceValue);
    }

}
