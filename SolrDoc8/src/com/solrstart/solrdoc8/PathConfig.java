package com.solrstart.solrdoc8;

/**
 * Created by arafalov on 4/14/14.
 */
public class PathConfig {
    public String name;
    public String jarSearchRoot;
    public String[] sourceSearchRoots;
    public String[] groupOrder;

    public PathConfig(String name, String jarSearchRoot, String[] sourceSearchRoots, String[] groupOrder)
    {
        this.name = name;
        this.jarSearchRoot = jarSearchRoot;
        this.sourceSearchRoots = sourceSearchRoots;
        this.groupOrder = groupOrder;
    }
}
