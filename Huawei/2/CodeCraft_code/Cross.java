package com.huawei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cross implements Comparable<Cross>{
    public static final int NORTH = 0;
    public static final int EAST = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;
    public static final int  STRAIGHT = 3;
    public static final int  LEFT= 2;
    public static final int  RIGHT= 1;
    private int id;
    private int north;
    private int east;
    private int south;
    private int west;
    private List<Integer> rids = new ArrayList<>();
    
    public boolean isWait = false;
    
    public int lockDelayTime = 0;
    
    public List<Integer> getRids(){
        return rids;
    }
    public void initRids(){
        if(north!=-1) rids.add(north);
        if(east!=-1) rids.add(east);
        if(south!=-1) rids.add(south);
        if(west!=-1) rids.add(west);
        Collections.sort(rids);
    }
    public Road getRelaxedChannel(int roadId){
        Road road;
        Car car;
        Channel channel;
        Road res=null;
        int cnums = -1;
        for(int rid:rids){
            if(rid==roadId) continue;
            road = Main.roads.get(rid);
            channel = road.getIntoChannels(id);
            if((channel==null)){
                if(cnums==-1){
                    res = road;
                    continue;
                }
                
            }else{
                if(channel.channel.isEmpty()){
                    cnums=9999;
                    return road;
                }else{
                    if(channel.channel.size()>cnums){
                        res = road;
                        cnums = channel.channel.size();
                    }
                    
                }
            }
           
        }
       
        if(res==null) {
            System.err.println("error");
            System.exit(1);
        }
        return res;
    }
    
    public int getDirection(int rid){
        if(north==rid) return NORTH;
        if(east==rid) return EAST;
        if(south==rid) return SOUTH;
        if(west==rid) return WEST;
        System.err.println("Direction error");
        System.exit(1);
        return -1;
    }
    
    /**
     * 
     * @param fid
     * @param tid
     * @return turn 3D 2L 1R 
     */
    public int getTurnDir(int fid,int tid){
        int f = getDirection(fid);
        int t =getDirection(tid);
//        if(t==NORTH && t==SOUTH) return STRAIGHT;
//        if(t==NORTH && t==EAST) return LEFT;
//        if(t==NORTH && t==WEST) return RIGHT;
        if(f==NORTH){
            if(t==SOUTH) return STRAIGHT;
            if(t==EAST) return LEFT;
            if(t==WEST) return RIGHT;
        }
        
        
//        if(t==EAST && t==WEST) return STRAIGHT;
//        if(t==EAST && t==SOUTH) return LEFT;
//        if(t==EAST && t==NORTH) return RIGHT;
        if(f==EAST){
            if(t==WEST) return STRAIGHT;
            if(t==SOUTH) return LEFT;
            if(t==NORTH) return RIGHT;
        }
        
        
        
//        if(t==SOUTH && t==NORTH) return STRAIGHT;
//        if(t==SOUTH && t==WEST) return LEFT;
//        if(t==SOUTH && t==EAST) return RIGHT;
        if(f==SOUTH){
            if(t==NORTH) return STRAIGHT;
            if(t==WEST) return LEFT;
            if(t==EAST) return RIGHT;
        }
        
//        
//        if(t==WEST && t==EAST) return STRAIGHT;
//        if(t==WEST && t==NORTH) return LEFT;
//        if(t==WEST && t==SOUTH) return RIGHT;
        if(f==WEST){
            if(t==EAST) return STRAIGHT;
            if(t==NORTH) return LEFT;
            if(t==SOUTH) return RIGHT;
        }
        System.err.println("error fid:"+fid+"     to tid:"+tid);
        System.exit(1);
        return STRAIGHT;
    }
    public int getRidFromDir(int toid,int dir){
        int to = getDirection(toid);
        
        if(to==NORTH){
            if(dir==STRAIGHT)return south;
            if(dir==LEFT)return west;
            if(dir==RIGHT)return east;
        }
        if(to==EAST){
            if(dir==STRAIGHT)return west;
            if(dir==LEFT)return north;
            if(dir==RIGHT)return south;
        } 
        if(to==SOUTH){
            if(dir==STRAIGHT)return north;
            if(dir==LEFT)return east;
            if(dir==RIGHT)return west;
        } 
        
        if(to==WEST){
            if(dir==STRAIGHT)return east;
            if(dir==LEFT)return south;
            if(dir==RIGHT)return north;
        } 
        System.err.println("error dir: to"+to);
        System.exit(1);
        return 0;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    
    public int getNorth() {
        return north;
    }
    public void setNorth(int north) {
        this.north = north;
    }
    public int getEast() {
        return east;
    }
    public void setEast(int east) {
        this.east = east;
    }
    public int getSouth() {
        return south;
    }
    public void setSouth(int south) {
        this.south = south;
    }
    public int getWest() {
        return west;
    }
    public void setWest(int west) {
        this.west = west;
    }
    @Override
    public int compareTo(Cross o) {
        // TODO Auto-generated method stub
        return this.id-o.id;
    }
    
}
