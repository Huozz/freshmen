package com.huawei;

import java.util.LinkedList;

public class Utils {
    
    public static <T>void copyLinkedList(LinkedList<T> dest,LinkedList<T> src){
        for(T t : src){
            dest.add(t);
        }
    }
}
