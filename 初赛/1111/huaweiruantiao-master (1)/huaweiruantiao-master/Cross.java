package com.huawei;

import java.util.ArrayList;
import java.util.List;

public class Cross {
    private int id;
    private int north;
    private int east;
    private int south;
    private int west;
    private List<Integer> rids = new ArrayList<>();
    
    public List<Integer> getRids(){
        return rids;
    }
    public void initRids(){
        if(north!=-1) rids.add(north);
        if(east!=-1) rids.add(east);
        if(south!=-1) rids.add(south);
        if(west!=-1) rids.add(west);
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
    
}
