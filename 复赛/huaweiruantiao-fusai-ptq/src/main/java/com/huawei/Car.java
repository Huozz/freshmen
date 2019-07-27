package com.huawei;

import java.util.LinkedList;

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
    private int realTime;
    private int arriveTime;
    private boolean proiority;
    private boolean preset;
    public Car waiting;
    private LinkedList<Integer> shortPath = new LinkedList<>();
    private LinkedList<Integer> realPath = new LinkedList<>();
    
    private int curRoadDis;//在当前已经道路行驶的距离
   
    private int flag=BEGIN;
    
    private int curPos=0; // 当前的位置（道路）
    public Car(){
        waiting=this;
        curPos = 0;
    }
    public void setArriveTime(int arriveTime){
        this.arriveTime = arriveTime;
    }
    public int getArriveTime(){
        return this.arriveTime;
    }
    public void setRealPath(LinkedList<Integer> path){
        this.realPath = path;
    }
    public void moveDistance(int dis){
        curRoadDis+=dis;
    }
    public void addPos(){
        curPos++;
    }
    public void setPos(int x){
        curPos = x;
    }
    public void setShortPath(LinkedList<Integer> path){
        this.shortPath = path;
    }
    public void addPath(int roadId){
        realPath.add(roadId);
    }
    
    public int getTime(Road road){
        int sp = Math.min(speed,road.getSpeed());
        return road.getLength()/sp;
        
    }
    
    
    public int getRealTime() {
        return realTime;
    }
    public void setRealTime(int realTime) {
        this.realTime = realTime;
    }
    public boolean isProiority() {
        return proiority;
    }
    public void setProiority(boolean proiority) {
        this.proiority = proiority;
    }
    public boolean isPreset() {
        return preset;
    }
    public void setPreset(boolean preset) {
        this.preset = preset;
    }
    public int getCurRoadId(){
        return realPath.get(curPos);
    }
    public int getNextRoadId(){
        return realPath.get(curPos+1);
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
                Judge.isWait=false;
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
    public LinkedList<Integer> getPath() {
        return realPath;
    }
    public Car findWaitChain(){
        if(waiting==this){
            return this;
        }
        return waiting.findWaitChain();
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(id);
        sb.append(',');
        sb.append(planTime);
        for(int id:realPath){
            sb.append(","+id);
        }
        sb.append(')');
        return sb.toString();
    }
    
    @Override
    public int compareTo(Car o) {
        //速度快的先调度
        //速度一样的计划时间
//        if(speed>o.getSpeed()) return 1;
//        else if(speed<o.getSpeed()) return -1;
//        else if(id>o.getId()) return 1;
//        else if(id<o.getId()) return -1;
//        else
//            return 0;
        //按时间调度

//        if(o.path!=null){
//            if(this.path.getStartTime()>o.path.getStartTime())return 1;
//            else if(this.path.getStartTime()<o.path.getStartTime()) return -1;
//            else if(id>o.getId()) return 1;
//            else if(id<o.getId()) return -1;
//            else
//            return 0;
//        }
//        if(MainOld.isMap2){
//            if(planTime>o.getPlanTime())return 1;
//            else if(planTime<o.getPlanTime()) return -1;
//            else if(id>o.getId()) return 1;
//            else if(id<o.getId()) return -1;
//            else
//            return 0;
//        }else{
//          if(speed>o.getSpeed()) return 1;
//          else if(speed<o.getSpeed()) return -1;
//          else if(id>o.getId()) return 1;
//          else if(id<o.getId()) return -1;
//          else
//              return 0;
//        }
      if(proiority==true&&o.isProiority()==false){
          return -1; 
      }else if(proiority==true&&o.isProiority()==true){
          if(realTime>o.getRealTime())return 1;
          else if(realTime<o.getRealTime()) return -1;
          else if(id>o.getId()) return 1;
          else if(id<o.getId()) return -1;
          else
          return 0;
      }else if(proiority==false&&o.isProiority()==false){
          if(realTime>o.getRealTime())return 1;
          else if(realTime<o.getRealTime()) return -1;
          else if(id>o.getId()) return 1;
          else if(id<o.getId()) return -1;
          else
          return 0;
      }else{
          return 1;
      }
      
    }
    
    
    
}
