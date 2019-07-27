package com.huawei;

import java.util.ArrayList;
import java.util.List;

public class Car implements Comparable<Car>{
    public static final int BEGIN = 0;
    public static final int WAIT = 1;
    public static final int END = 2;
    public static final int ARRIVE = 3;
    private int id;
    private int from;
    private int to;
    private int speed;
    private int planTime;
    public Car waiting;
    private Answer path;
    
    private int curRoadDis;//在当前已经道路行驶的距离
   
    private int flag=BEGIN;
    
    private int curPos=0; // 当前的位置（道路）
    public Car(){
        waiting=this;
    }
    public void moveDistance(int dis){
        curRoadDis+=dis;
    }
    public void addPos(){
        curPos++;
    }
    public int getTime(Road road){
        int sp = Math.min(speed,road.getSpeed());
        return road.getLength()/sp;
        
    }
    public int getCurRoadId(){
        return path.getRoadIds().get(curPos);
    }
    public int getNextRoadId(){
        return path.getRoadIds().get(curPos+1);
    }
    public void setFlag(int i){
        if(i==WAIT){
            if(this.flag!=WAIT) {
               // System.out.println(id +"  "+ this.flag+" ----> Wait ");
                Judge.carWaitCnt++;
            }
        }else{
            if(this.flag==WAIT){
               // System.out.println(id +"  "+ this.flag+" ---->  " + i);
                Judge.carWaitCnt--;
                Judge.waiting=false;
            }
            waiting=this;
        }
        this.flag = i;
    }
    public int getFlag(){
        return this.flag;
    }
    public int getCurRoadDis() {
        return curRoadDis;
    }
    public void setCurRoadDis(int dis) {
        this.curRoadDis = dis;
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
    public Answer getPath() {
        return path;
    }
    public void setPath(Answer path) {
        this.path = path;
    }
    public Car findWaitChain(){
        if(waiting==this){
            return this;
        }
        return waiting.findWaitChain();
    }
    public void addToWaitChain(Car c){
        waiting = c.findWaitChain();
    }
//    public void outputWaitChain(){
//        System.out.print(this.id+" ");
//    }
    public void setWaitSeft(){
        waiting = this;
    }
    @Override
    public int compareTo(Car o) {
        //速度快的先调度
        //速度一样的计划时间
//        if(speed>o.getSpeed()) return 1;
//        else if(speed<o.getSpeed()) return -1;
//        else if(planTime<o.getPlanTime()) return 1;
//        else if(planTime>o.getPlanTime()) return -1;
//        else
        //按时间调度
        if(o.path!=null){
            if(this.path.getStartTime()>o.path.getStartTime())return 1;
            else if(this.path.getStartTime()<o.path.getStartTime()) return -1;
            else if(id>o.getId()) return 1;
            else if(id<o.getId()) return -1;
            else
            return 0;
        }
        if(planTime>o.getPlanTime())return 1;
        else if(planTime<o.getPlanTime()) return -1;
        else if(id>o.getId()) return 1;
        else if(id<o.getId()) return -1;
        else
        return 0;
    }
    
    
}
