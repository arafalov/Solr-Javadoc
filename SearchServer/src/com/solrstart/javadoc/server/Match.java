package com.solrstart.javadoc.server;

import org.apache.solr.client.solrj.beans.Field;

import java.util.Map;

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
    @Field private final String comment;
    @Field private final String module;

    private final String packageAsPath;

    private String htmlDescription;
    private String urlTarget;

    public Match(String id, String type, String packageName, String className, String sourceClassName,  String methodName, String description, String comment, String module)
    {
        this.id = id;
        this.type = type;
        this.packageName = packageName;
        this.className = className;
        this.sourceClassName = sourceClassName;
        this.methodName = methodName;
        this.description = description;
        this.comment = comment;
        this.module = module;

        this.packageAsPath = packageName.replaceAll("\\.", "/");
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

    public String getModule() {
        return module;
    }

    public String getComment() {
        return comment;
    }

    public String getHtmlDescription() {
        return htmlDescription;
    }

    public String getUrlTarget() {
        return urlTarget;
    }

    //TODO: This should be possible to do automatically, since Spring somehow knows how to map!
    public void createHTMLDescription(Map<String, String> overrides)
    {
        String result = replace(description, "packageName", overrides.get("packageName"), getPackageName());
        result = replace(result, "className", overrides.get("className"), getClassName());
        result = replace(result, "sourceClassName", overrides.get("sourceClassName"), getSourceClassName());
        result = replace(result, "methodName", overrides.get("methodName"), getMethodName());
        this.htmlDescription = result;
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

    public void lockTheURL(String base) {
        switch(type) {
            case "package":
                //e.g. http://localhost:8983/javadoc/solr-core/index.html?org/apache/solr/package-summary.html
                urlTarget = String.format("%s/%s/index.html?%s/package-summary.html",
                        base,
                        module,
                        packageAsPath);
                break;
            case "class":
            case "method":
                //e.g. http://localhost:8983/javadoc/solr-core/index.html?org/apache/solr/SolrLogFormatter.html
                //Can't do method signature anchor link yet
                urlTarget = String.format("%s/%s/index.html?%s/%s.html",
                        base,
                        module,
                        packageAsPath,
                        className);
                break;
            case "inherit":
                //Same as class, but use sourceClassName
                urlTarget = String.format("%s/%s/index.html?%s/%s.html",
                        base,
                        module,
                        packageAsPath,
                        sourceClassName);
                break;
            default:
                urlTarget = base +"?UNKNOWN"; //should not happen, but I am sure it will happen
                break;
        }
    }


}
