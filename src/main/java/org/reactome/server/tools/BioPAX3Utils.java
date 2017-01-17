package org.reactome.server.tools;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Pathway;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.domain.model.Event;


import java.io.*;
import java.util.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class TypeCounter {
    private String mName;
    private Integer mCount;

    TypeCounter(String name) {
        mName = name;
        mCount = 0;
    }

    String getName() { return mName; }

    Integer getCount() { return mCount; }

    void incrementCount() {
        mCount++;
    }
}


class BioPAX3Utils {

    private static ArrayList<TypeCounter> count = new ArrayList<TypeCounter>();

    static String getTypeCount(String name) {
        if (count.size() == 0) {
            count.add(new TypeCounter("Pathway"));
        }
        String id = "";
        for (TypeCounter tc: count) {
            if (tc.getName().equals(name)) {
                tc.incrementCount();
                id = getID(name + tc.getCount());
                break;
            }
        }
        if (id.length() == 0){
            TypeCounter tc1 = new TypeCounter(name);
            tc1.incrementCount();
            count.add(tc1);
            id = getID(name + tc1.getCount());
        }
        return id;
    }

    static void clearCounterArray() {
        if (count.size() > 0) {
            count.clear();
        }
    }

    static String getID(String id){
        return (WriteBioPAX3.xmlBase + id);
    }

    static <T extends org.biopax.paxtools.model.BioPAXElement> T getObjectFromSet(Set<T> setObjects, String id) {
        if (setObjects == null || setObjects.size() == 0) {
            return null;
        }
        else if (id == null || id.length() == 0) {
            return null;
        }
        T p = null;
        Boolean found = false;
        for (T obj : setObjects) {
            if (obj.getRDFId().equals(getID(id))) {
                found = true;
                p = obj;
                break;
            }
        }
        if (found){
            return p;
        }
        else {
            return null;
        }
    }

    static <T extends org.biopax.paxtools.model.level3.Named> T getObjectFromSetByName(Set<T> setObjects, String name) {
        if (setObjects == null || setObjects.size() == 0) {
            return null;
        }
        else if (name == null || name.length() == 0) {
            return null;
        }
        T p = null;
        Boolean found = false;
        Set<String> names = new TreeSet<String>();
        names.add(name);

        for (T obj : setObjects) {
            if (obj.getName().equals(names)) {
                found = true;
                p = obj;
                break;
            }
        }
        if (found){
            return p;
        }
        else {
            return null;
        }
    }

    static <T extends org.biopax.paxtools.model.BioPAXElement> Boolean contains(Set<T> setObjects, String id) {
        if (setObjects == null || setObjects.size() == 0) {
            return false;
        }
        else if (id == null || id.length() == 0) {
            return false;
        }
        Boolean containsObj = false;
        for (T obj : setObjects) {
            if (obj.getRDFId().equals(getID(id))) {
                containsObj = true;
                break;
            }
        }
        return  containsObj;
    }

    static <T extends org.biopax.paxtools.model.level3.Named> Boolean containsName(Set<T> setObjects, String name) {
        if (setObjects == null || setObjects.size() == 0) {
            return false;
        }
        else if (name == null || name.length() == 0) {
            return false;
        }
        Boolean containsObj = false;
        Set<String> names = new TreeSet<String>();
        names.add(name);
        for (T obj : setObjects) {
            if (obj.getName().equals(names)) {
                containsObj = true;
                break;
            }
        }
        return  containsObj;
    }

}
