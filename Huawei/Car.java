package com.huawei;

import java.util.Random;

public class Car implements Comparable<Car>{
    private int id;
    private int from;
    private int to;
    private int speed;
    private int planTime;
    
   
    public int getTime(Road road){
        int sp = Math.min(speed,road.getSpeed());
        return road.getLength()/sp;
        
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getFrom() {
        return from;
    }
    public void setFrom(int from) {
        this.from = from;
    }
    public int getTo() {
        return to;
    }
    public void setTo(int to) {
        this.to = to;
    }
    public int getSpeed() {
        return speed;
    }
    public void setSpeed(int speed) {
        this.speed = speed;
    }
    public int getPlanTime() {
        return planTime;
    }
    public void setPlanTime(int planTime) {
        this.planTime = planTime;
    }
    @Override
    public int compareTo(Car o) {
        //速度快的先调度
        //速度一样的计划时间
        if(speed>o.getSpeed()) return 1;
        else if(speed<o.getSpeed()) return -1;
        else if(planTime<o.getPlanTime()) return 1;
        else if(planTime>o.getPlanTime()) return -1;
        else
//        if(planTime>o.getPlanTime())return 1;
//        else if(planTime<o.getPlanTime()) return -1;
//        else if(speed>o.getSpeed()) return 1;
//        else if(speed<o.getSpeed()) return -1;
//        else
        return 0;
    }
    
    
}
