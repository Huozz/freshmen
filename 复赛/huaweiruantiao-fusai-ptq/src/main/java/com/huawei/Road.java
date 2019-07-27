package com.huawei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Road implements Comparable<Road> {
    private int id;
    private int length;
    private int speed;
    private int channel;
    private int from;
    private int to;
    private boolean isDuplex;

    private List<Channel> fchannels = new ArrayList<>();// forward
    private List<Channel> bchannels = new ArrayList<>();// backward

    public boolean block = false;

    private int fcurFirstChannelId = 0;
    private int bcurFirstChannelId = 0;

    public boolean flag = false;

    public void init() {
        for (int i = 0; i < channel; i++) {
            fchannels.add(new Channel(this, i));
        }
        if (isDuplex) {
            for (int i = 0; i < channel; i++) {
                bchannels.add(new Channel(this, i));
            }
        }

    }

    public int getMaxCarNum() {
        return this.channel * this.length;
    }

    public int getCurHaveCarNum(int start) {
        int cnt = 0;
        if (start == from) {
            for (Channel x : fchannels) {
                cnt += x.channel.size();
            }
        } else {
            for (Channel x : bchannels) {
                cnt += x.channel.size();
            }
        }
        return cnt;
    }

    // 取第一优先级的
    public Car getFirst(int start) {
        Car c = null;
        Car res = null;
        int maxDis = -1;
        boolean proiority = false;
        if (start == this.to) {
            for (Channel ch : fchannels) {
                if (ch.channel.isEmpty())
                    continue;
                c = ch.channel.getFirst();
                if (c == null) {
                    continue;
                } else {
                    if (c.getFlag() == Car.WAIT) {
                        // 当前还没有优先车辆
                        if (!proiority) {
                            // 设置优先车
                            if (c.isProiority()) {
                                maxDis = c.getCurRoadDis();
                                res = c;
                                fcurFirstChannelId = ch.cid;
                                proiority = true;
                            } else {
                                // 否则判断距离
                                if (c.getCurRoadDis() > maxDis) {
                                    maxDis = c.getCurRoadDis();
                                    res = c;
                                    fcurFirstChannelId = ch.cid;
                                }
                            }

                        } else {
                            // 只选择优先车
                            if (c.isProiority()) {
                                if (c.getCurRoadDis() > maxDis) {
                                    maxDis = c.getCurRoadDis();
                                    res = c;
                                    fcurFirstChannelId = ch.cid;
                                }

                            }
                        }

                    }
                }

            }
        } else {
            for (Channel ch : bchannels) {
                if (ch.channel.isEmpty())
                    continue;
                c = ch.channel.getFirst();
                if (c == null) {
                    continue;
                } else {
                    if (c.getFlag() == Car.WAIT) {
                        // 当前还没有优先车辆
                        if (!proiority) {
                            // 设置优先车
                            if (c.isProiority()) {
                                maxDis = c.getCurRoadDis();
                                res = c;
                                bcurFirstChannelId = ch.cid;
                                proiority = true;
                            } else {
                                // 否则判断距离
                                if (c.getCurRoadDis() > maxDis) {
                                    maxDis = c.getCurRoadDis();
                                    res = c;
                                    bcurFirstChannelId = ch.cid;
                                }
                            }

                        } else {
                            // 只选择优先车
                            if (c.isProiority()) {
                                if (c.getCurRoadDis() > maxDis) {
                                    maxDis = c.getCurRoadDis();
                                    res = c;
                                    bcurFirstChannelId = ch.cid;
                                }

                            }
                        }

                    }
                }

            }
        }

        return res;
    }

    public Channel getIntoChannels(int start) {
        Car c;
        Channel channel = null;
        if (start == this.from) {
            for (Channel ch : fchannels) {
                if (ch.channel.isEmpty())
                    return ch;
                c = ch.channel.getLast();
                if (c.getFlag() == Car.WAIT) {
                    return ch;
                }
                if (c.getCurRoadDis() > 1) {
                    channel = ch;
                    return channel;
                }
            }
        } else {
            for (Channel ch : bchannels) {
                if (ch.channel.isEmpty())
                    return ch;
                c = ch.channel.getLast();
                if (c.getFlag() == Car.WAIT) {
                    return ch;
                }
                if (c.getCurRoadDis() > 1) {
                    channel = ch;
                    return channel;
                }
            }
        }

        return channel;
    }

    // public void moveOutRoad(int start){
    // if(start==this.to){
    // fchannels.get(curFirstChannelId).channel.poll();
    // }else{
    // bchannels.get(curFirstChannelId).channel.poll();
    // }
    // }
    // 与getFirst连用
    public Channel getFirstChannel(int start) {
        if (start == this.to) {
            return fchannels.get(fcurFirstChannelId);
        } else {
            return bchannels.get(bcurFirstChannelId);
        }
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

    public double getWeigth() {
        if (block)
            return 999999;
        return (length * 1.0 / speed) * Math.pow((97.0 / 100), getMaxCarNum()) * 10;// map2
    }

    public double getWeigth(int start) {
        if (block)
            return 999999;
        return (length * 1.0 / speed) * 10 * (getCurHaveCarNum(start) * 1.0 / getMaxCarNum());
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

    @Override
    public int compareTo(Road o) {
        // TODO Auto-generated method stub
        return id - o.id;
    }

}
