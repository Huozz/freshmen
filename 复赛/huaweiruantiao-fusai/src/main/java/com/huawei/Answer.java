package com.huawei;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class Answer {
    private int carId;
    private int startTime;
    private LinkedList<Integer> roadIds = new LinkedList<>();
    
    public Answer(){
        
    }
    public Answer(int carId,int startTime){
        this.carId = carId;
        this.startTime = startTime;
    }
    public Answer(int carId,int startTime,LinkedList<Integer> roadIds){
        this.roadIds = roadIds;
        this.carId = carId;
        this.startTime = startTime;
    }
    public void addToHead(int rid){
        roadIds.addFirst(rid);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(carId);
        sb.append(',');
        sb.append(startTime);
        for(int id:roadIds){
            sb.append(","+id);
        }
        sb.append(')');
        return sb.toString();
    }
    
    public int getCarId() {
        return carId;
    }
    public void setCarId(int carId) {
        this.carId = carId;
    }
    public int getStartTime() {
        return startTime;
    }
    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }
    public LinkedList<Integer> getRoadIds() {
        return roadIds;
    }
    public void setRoadIds(LinkedList<Integer> roadIds) {
        this.roadIds = roadIds;
    }
    
}
