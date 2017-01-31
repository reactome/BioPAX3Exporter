package org.reactome.server.tools;


import java.util.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

/**
 * Class to keep track of the different IDS used in biopax
 * e.g. Pathway1 Publication1 etc.
 */
class TypeCounter {
    private final String mName;
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

    /**
     * Function to return the next ID to use for a given class name
     *
     * @param name the name of the BioPax class
     *
     * @return the full qualified ID of the next object of the given class
     */
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

    /**
     * Function to ensure counter is cleared with each new run of WriteBioPAX3
     */
    static void clearCounterArray() {
        if (count.size() > 0) {
            count.clear();
        }
    }

    /**
     * Function to return full ID based on the xml base registered
     *
     * @param id the unique part of the ID to append
     *
     * @return fully qualified ID
     */
    static String getID(String id){
        return (WriteBioPAX3.xmlBase + id);
    }

    /**
     * Generic function to retrieve an object from a set based on its ID
     *
     * paxtools uses Sets to store the individual types of element added to the
     * model. This is a generic function based on the BioPAXElement class - all
     * of which have an ID
     *
     * @param setObjects the set of objects to query
     * @param id the unqualified id to match
     *
     * @return the object from the set that matches the fully qualified ID created
     * from the id argument passed OR null if no such object exists
     */
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

    /**
     * Generic function to retrieve an object from a set based on its Set of names
     *
     * paxtools uses Sets to store the individual types of element added to the
     * model. This is a generic function based on the Named class - all
     * of which have a Set<String> names
     *
     * @param setObjects the set of objects to query
     * @param name String representing a name to match
     *
     * @return the object from the set that matches the name argument passed
     * OR null if no such object exists
     */
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

    /**
     * Function to determine whether a set contains an element with the given ID
     *
     * @param setObjects the set of objects to query
     * @param id the unqualified id to match
     *
     * @return true if the Set contains an element matching the id given; false otherwise
     */
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

    /**
     * Function to determine whether a set contains an element with the given name
     *
     * @param setObjects the set of objects to query
     * @param name String representing a name to match
     *
     * @return true if the Set contains an element matching the name given; false otherwise
     */
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
