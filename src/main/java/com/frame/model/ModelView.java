package com.frame.model;

import java.util.HashMap;
import java.util.Map;

public class ModelView {

    private String view;
    private Map<String , Object> attributes;
    public ModelView(){
        this.attributes = new HashMap<String , Object>();
    }
    public ModelView(String view){
        this.attributes = new HashMap<String , Object>();
        this.view = view;
    }
    public ModelView(String view , Map<String , Object> attributes){
        this.view=view;
        if(attributes==null){
            this.attributes=new HashMap<String , Object>();
        } else {
            this.attributes=attributes;
        }
    }

    public String getView(){
        return view;
    }

    public void setView(String view){
        this.view=view;
    }
    public void addAttribute(String key,Object value){
        attributes.put(key, value);
    }
    public Object getAttribute(String key){
        return attributes.get(key);
    }
    public Map<String ,Object> getAttributes(){
        return attributes;
    }
    public void setAttibutes(Map<String ,Object > attributes){
        if (attributes == null) {
            attributes = new HashMap<String , Object>();
        }else {
            this.attributes=attributes;
        }
    }

}
