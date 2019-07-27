package com.huawei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Road {
    private int id;
    private int length;
    private int speed;
    private int channel;
    private int from;
    private int to;
    private boolean isDuplex;
    private HashMap<Integer,Integer> fblocking = new HashMap<>();
    private HashMap<Integer,Integer> tblocking = new HashMap<>();
    
    
    private List<Channel> fchannels = new ArrayList<>();//forward
    private List<Channel> bchannels =new ArrayList<>();//backward 
    
    private int priority=3; // 3D 2L 1R
    
    private int curFirstChannelId=0;
    
    public boolean flag=false;
    public void init(){
        for(int i=0;i<channel;i++){
            fchannels.add(new Channel(this,i));
        }
        if(isDuplex){
            for(int i=0;i<channel;i++){
                bchannels.add(new Channel(this,i));
            }
        }
        
    }
    
    //取第一优先级的
    public Car getFirst(int start){
        Car c=null;
        Car res=null;
        int maxDis=-1;
        if(start==this.to){
            for(Channel ch : fchannels){
                if(ch.channel.isEmpty()) continue;
                c = ch.channel.getFirst();
                if(c==null){
                    continue;
                }else{
                    if(c.getFlag()==Car.WAIT){
                        if(c.getCurRoadDis()>maxDis){
                            maxDis = c.getCurRoadDis();
                            res=c;
                            curFirstChannelId = ch.cid;
                        }
//                        return c;
                    }
                }
                
            }
        }else{
            for(Channel ch : bchannels){
                if(ch.channel.isEmpty()) continue;
                c = ch.channel.getFirst();
                if(c==null){
                   continue;
                }else{
                    if(c.getFlag()==Car.WAIT){
                        if(c.getCurRoadDis()>maxDis){
                            maxDis = c.getCurRoadDis();
                            res=c;
                            curFirstChannelId = ch.cid;
                        }
                    }
                }
                
            }
        }
        
        return res;
    }
    
    public Channel getIntoChannels(int start){
        Car c;
        Channel channel=null;
        if(start==this.from){
            for(Channel ch : fchannels){
                if(ch.channel.isEmpty())
                    return ch;
                c = ch.channel.getLast();
                if(c.getFlag()==Car.WAIT){
                    return ch;
                }
                if(c.getCurRoadDis()>1){
                    channel = ch;
                    return channel;
                }
            }
        }else{
            for(Channel ch : bchannels){
                if(ch.channel.isEmpty())
                    return ch;
                c = ch.channel.getLast();
                if(c.getFlag()==Car.WAIT){
                    return ch;
                }
                if(c.getCurRoadDis()>1){
                    channel = ch;
                    return channel;
                }
            }
        }
        
        return channel;
    }
    
    
    public void moveOutRoad(int start){
        if(start==this.to){
            fchannels.get(curFirstChannelId).channel.poll();
        }else{
            bchannels.get(curFirstChannelId).channel.poll();
        }
    }
    public Channel getFirstChannel(int start){
        if(start==this.to){
            return fchannels.get(curFirstChannelId);
        }else{
            return bchannels.get(curFirstChannelId);
        }
    }
    
    
    public void addBlocking(int time,int start){
        int key = (time/10)*10;
        if(isDuplex){
            if(start==from){
                int oldValue =  fblocking.getOrDefault(key, 0);
                fblocking.put(key, oldValue+1);
            }else{
                int oldValue =  tblocking.getOrDefault(key, 0);
                tblocking.put(key, oldValue+1);
            }
        }else{
            int oldValue =  fblocking.getOrDefault(key, 0);
            fblocking.put(key, oldValue+1);
        }
        
       
       
    }
    
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<Channel> getFchannels() {
        return fchannels;
    }
    public void setFchannels(List<Channel> fchannels) {
        this.fchannels = fchannels;
    }
    public List<Channel> getBchannels() {
        return bchannels;
    }
    public void setBchannels(List<Channel> bchannels) {
        this.bchannels = bchannels;
    }
    public int getWeigth(){
        return (int)Math.ceil(length/speed);
    }
    public int getWeigth(int carSpeed){
        int sp = Math.min(speed,carSpeed);
        return (int)Math.ceil(length/speed);
    }
    public double getWeigth(int carSpeed,int time){

//        int sp = Math.min(speed,carSpeed);
        return getWeigth(carSpeed,time,from);
        
    }
    public double getWeigth(int carSpeed,int time,int start){
        int key = (time/10)*10;
        int sp = Math.min(speed,carSpeed);
        int totalNum = length*channel;
        int rest;
        double wei =0.0;
        if(isDuplex){
            if(from==start){
                rest = totalNum-fblocking.getOrDefault(key, 0);
            }else{
                rest = totalNum-tblocking.getOrDefault(key, 0);
            }
            
        }else{
            rest = totalNum-fblocking.getOrDefault(key, 0);
        }
        if(rest>8){
            wei = length*1.0/sp;
        }else{
            wei = length*1.0/sp*Math.pow(2, 8-rest);
        }
        
        return wei;
    }

    
    
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }
    public int getSpeed() {
        return speed;
    }
    public void setSpeed(int speed) {
        this.speed = speed;
    }
    public int getChannel() {
        return channel;
    }
    public void setChannel(int channel) {
        this.channel = channel;
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
    public boolean getIsDuplex() {
        return isDuplex;
    }
    public void setIsDuplex(boolean isDuplex) {
        this.isDuplex = isDuplex;
    }
  
    
}
