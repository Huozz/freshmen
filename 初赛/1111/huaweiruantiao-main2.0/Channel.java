package com.huawei;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Channel {
    public Road road;
    public int cid;
    public LinkedList<Car> channel = new LinkedList<>();

    Channel(Road road, int cid) {
        this.cid = cid;
        this.road = road;

    }

    public boolean moveInACar(Channel lastChannel, Car c) {
        Car curTailCar;
        int lastCanMoveDis = lastChannel.road.getLength() - c.getCurRoadDis();
        int nextCanMoveDis = road.getLength();
        int maxSpeed = Math.min(c.getSpeed(), road.getSpeed());
        if(!this.channel.isEmpty()){
            curTailCar = this.channel.getLast();
            nextCanMoveDis = curTailCar.getCurRoadDis() - 1;
            if (maxSpeed - lastCanMoveDis <= 0) {
                // 开到路口
                c.setCurRoadDis(lastChannel.road.getLength());
                c.setFlag(Car.END);
                return false;// dao lu kou
            } else {
                if (maxSpeed - lastCanMoveDis <= nextCanMoveDis) {
                    c.setCurRoadDis(maxSpeed - lastCanMoveDis);
                    c.addPos();
                    c.setFlag(Car.END);
                    channel.add(c);
                    return true;
                }
                if (curTailCar.getFlag() == Car.WAIT) {
                    c.setFlag(Car.WAIT);
                    return false;
                }
                c.setCurRoadDis(nextCanMoveDis);
                c.addPos();
                c.setFlag(Car.END);
                channel.add(c);
                return true;
            }
        }

        if (maxSpeed - lastCanMoveDis <= 0) {
            // 开到路口
            c.setCurRoadDis(lastChannel.road.getLength());
            c.setFlag(Car.END);
            return false;// dao lu kou
        }
        // if(maxSpeed-lastCanMoveDis<=nextCanMoveDis){
        // c.setCurRoadDis(maxSpeed-lastCanMoveDis);
        // c.addPos();
        // c.setFlag(Car.END);
        // return true;
        // }
        // c.setCurRoadDis(nextCanMoveDis);
        // c.addPos();
        // c.setFlag(Car.END);
        c.setCurRoadDis(maxSpeed - lastCanMoveDis);
        c.addPos();
        c.setFlag(Car.END);
        channel.add(c);
        return true;
    }

    public void driveCar(boolean b) {
        boolean newTime = b;
        Car last = null;
        int k = 0;
        if (newTime) {
            while (k < channel.size()) {
                Car c = channel.get(k);
                if (last == null) {
                    int dis = c.getCurRoadDis();
                    int maxSpeed = Math.min(c.getSpeed(), road.getSpeed());
                    if (dis + maxSpeed <= road.getLength()) {
                        c.moveDistance(maxSpeed);
                        c.setFlag(Car.END);
                    } else {
                        c.setFlag(Car.WAIT);
                    }

                } else {
                    int maxSpeed = Math.min(c.getSpeed(), road.getSpeed());
                    int dis = last.getCurRoadDis() - c.getCurRoadDis() - 1;
                    if (dis >= maxSpeed) {
                        c.moveDistance(maxSpeed);
                        c.setFlag(Car.END);
                    } else {
                        if (last.getFlag() == Car.WAIT) {
                            c.setFlag(Car.WAIT);
                        } else {
                            c.moveDistance(dis);
                            c.setFlag(Car.END);
                        }
                    }

                }
                last = c;
                k++;
            }
        } else {
            while (k < channel.size()) {
                Car c = channel.get(k);
                if (last == null) {
                    if (c.getFlag() == Car.WAIT) {
                        int dis = c.getCurRoadDis();
                        int maxSpeed = Math.min(c.getSpeed(), road.getSpeed());
                        if (dis + maxSpeed <= road.getLength()) {
                            c.moveDistance(maxSpeed);
                            c.setFlag(Car.END);
                        } else {
                            c.setFlag(Car.WAIT);
                        }
                    }

                } else {
                    if (c.getFlag() == Car.WAIT) {
                        int maxSpeed = Math.min(c.getSpeed(), road.getSpeed());
                        int dis = last.getCurRoadDis() - c.getCurRoadDis() - 1;
                        if (dis >= maxSpeed) {
                            c.moveDistance(maxSpeed);
                            c.setFlag(Car.END);
                        } else {
                            if (last.getFlag() == Car.WAIT) {
                                c.setFlag(Car.WAIT);
                            } else {
                                c.moveDistance(dis);
                                c.setFlag(Car.END);
                            }
                        }
                    }

                }
                last = c;
                k++;
            }
        }

    }

    public void intoNewCar(Car c) {
        Car lastCar = null;
        int maxSpeed = Math.min(c.getSpeed(), road.getSpeed());
        if (this.channel.isEmpty()) {
            c.setCurRoadDis(maxSpeed);
            c.setFlag(Car.END);
            channel.add(c);
            return;
        } else {
            lastCar = this.channel.getLast();
        }
        if (maxSpeed <= lastCar.getCurRoadDis() - 1) {
            c.setCurRoadDis(maxSpeed);
        } else {
            c.setCurRoadDis(lastCar.getCurRoadDis() - 1);
        }
        c.setFlag(Car.END);
        channel.add(c);
    }
}
