package com.solrstart.javadoc.server;

import org.apache.solr.client.solrj.beans.Field;

/**
 * Created by arafalov on 4/18/14.
 */
public class Match {
    @Field private final String id;
    @Field private final String packageName;
    @Field private final String className;
    @Field private final String methodName;
    @Field private final String description;

    public Match(String id, String packageName, String className, String methodName, String description)
    {
        this.id = id;
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescription() {
        return description;
    }

}
