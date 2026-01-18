package com.frame.model;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpSession;



public class Session {
    Map<String, Object> attributes;

    public Session(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Session() {
        attributes = new HashMap<>();
    }

    public Session(HttpSession session) {
        attributes = new HashMap<>();
        if (session!=null) {
            Enumeration<String> attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String attrName = attrNames.nextElement();
                Object attrValue = session.getAttribute(attrName);
                attributes.put(attrName, attrValue);
            }
        }
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    public void clear() {
        attributes.clear();
    }

    public void destroy() {
        attributes.clear();
    }

    public void merge(HttpSession session) {
    if (session != null && attributes != null) {

        Enumeration<String> existingAttributes = session.getAttributeNames();
        List<String> toRemove = new ArrayList<>();
        while (existingAttributes.hasMoreElements()) {
            String attrName = existingAttributes.nextElement();
            toRemove.add(attrName);
        }
        for (String attrName : toRemove) {
            session.removeAttribute(attrName);
        }


        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            session.setAttribute(entry.getKey(), entry.getValue());
        }
    }
}


}
