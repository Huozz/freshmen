package com.huawei;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);
    public static List<Car> cars = new ArrayList<>();
    public static HashMap<Integer,Car> carMap = new HashMap<>();
    public static HashMap<Integer, Road> roads = new HashMap<>();
    public static HashMap<Integer, Cross> crosses = new HashMap<>();
    public static HashMap<Integer, LinkedList<Car>> carInGarage = new HashMap<>();
    public static List<Cross> crossList = new ArrayList<>();
    public static boolean waiting = false;
    public static boolean isWait = false;
    public static int carInRoadCnt = 0;
    public static int carWaitCnt = 0;
    public static int carArriveCnt = 0;
    public static int carAllCnt = 0;
    public static int presetAllCnt = 0;
    public static int presetCnt=0;
    public static int presetlimit=0;
    public static int time = 0;
    public static int cctime=50;
    public static int changenum=0;
    //保存某点到某点的最短路径距离
    public static HashMap<Integer,HashMap<Integer,Integer>> shorPathMap = new HashMap<>();
    
    public static void main(String[] args)
    {
        if (args.length != 5) {
//            logger.error("please input args: inputFilePath, resultFilePath");
            return;
        }

        logger.info("Start...");

        String carPath = args[0];
        String roadPath = args[1];
        String crossPath = args[2];
        String presetAnswerPath = args[3];
        String answerPath = args[4];
        //init
        initRead(carPath, roadPath, crossPath,presetAnswerPath);
        init();
        start();
        writeAnswer(answerPath);
    }
    public static void start() {
        System.out.println("开始调度。。。");
        System.out.println(carMap.size());
        int lockCnt=0;
        for (time = 1; carArriveCnt != carMap.size(); time++) {
            for (Road r : roads.values()) {
                // 开始新的一轮标定
                driveJustCurrentRoad(r, true);
            }
            //优先车上路
            driveCarInitList(true);
            lockCnt=0;
            while (carWaitCnt != 0) {
                if (isWait) {
                    waiting = true;
//                    System.out.println("dead lock!!!");
                    lockCnt++;
                    if(lockCnt>100){
                        processDeadLock();
                    }
                    if(lockCnt>1000){
                        System.out.println("Dead Lock!!!");
                        System.exit(1);
                    }
                }
                isWait = true;
                for (Cross cross : crossList) {
                     cross.isWait = true;
                    driveAllWaitCar(cross);
                }

            }
            //所有车上路
            driveCarInitList(false);
            for (Cross cross : crossList) {
                cross.lockDelayTime--;
            }
            if(presetCnt<=0) cctime--; 
        }
        TimeCalc tc = new TimeCalc(time-1, cars);
        tc.calc();
        System.out.println("调度时间：" + (time - 1)+"    "+tc.tSum);
        System.out.println(tc.tPri+"    "+tc.tSumPri);
        System.out.println(tc.tE+"   " + tc.tESum);
        System.out.println("修改数量"+changenum);
    }
    public static void init() {
//      init shortPathMap as A-start 's h(n)
        initShortPathMap();
//         init car to carInGarage
        Car c;
        LinkedList<Car> list;
        for (Car car : cars) {
            if(!car.isPreset())
                car.setRealTime(car.getPlanTime());
            if ((list = carInGarage.get(car.getFrom())) != null) {
                list.add(car);
            } else {
                list = new LinkedList<>();
                list.add(car);
                carInGarage.put(car.getFrom(), list);
            }
        }
        for (LinkedList<Car> ls : carInGarage.values()) {
            Collections.sort(ls);
        }
        Collections.sort(crossList);
        
        System.out.println("cross size:" + crossList.size());
        System.out.println("car size:" + cars.size());
        System.out.println("road size:" + roads.size());
        
    }
    // TODO:driveAllCarJustOnRoadToEndState
    public static void driveJustCurrentRoad(Road road, boolean b) {
        List<Channel> fchannels = road.getFchannels();
        for (Channel channel : fchannels) {
            channel.driveCar(b);
        }
        road.createCarSequeue(road.getTo());
        if (road.getIsDuplex()) {
            List<Channel> bchannels = road.getBchannels();
            for (Channel channel : bchannels) {
                channel.driveCar(b);
            }
            road.createCarSequeue(road.getFrom());
        }
    }
    // TODO:driveAllWaitCar
    public static void driveAllWaitCar(Cross cross) {
        List<Integer> rids = cross.getRids();
        List<Road> roadsList = cross.getRoadList();
        int k = 0;
        Road road;
        Road nextRoad;
        Car car;
        Car tmpCar;
        Channel firstChannel = null;
        int cnt = 0;
        // each roads
        //确定等待状态的下一步
        decideCrossWaitNextRoad(cross);
        while (k < roadsList.size()) {
            road = roadsList.get(k);
            // each channels
            while (true) {
                car = road.getFirst(cross.getId());
                // 该道路已经调度完，即没有等待状态
                if (car == null) {
                    cnt++;
                    break;
                }
                car.waiting=car;
                firstChannel = car.getChannel();
                // 判断是否到达终点
                if (car.getTo() == cross.getId()) {
                    // 不是优先车辆
                    if (!car.isProiority()) {
                        int nextRid = cross.getTidByByStraight(road.getId());
                        if (nextRid != -1) {
                            // 可能冲突，遍历其他路口，查看是否优先车辆进入该路口
                            if ((tmpCar=proiorityCarIntoRoad(cross, road, nextRid, roadsList))!=null) {
                                car.waiting = tmpCar;
                                break;
                            }
                        }
                    }
                    car.setFlag(Car.ARRIVE);
                    car.setArriveTime(time);
                    carArriveCnt++;
                    // 从道路删除
                    firstChannel.channel.poll();
                    firstChannel.driveCar(false);
                    firstChannel.road.createCarSequeue(cross.getId());
                    driveCarInitList(crosses.get(firstChannel.road.getFrom()), firstChannel.road);
                    decideCrossWaitNextRoad(cross);
                    System.out.println("arrvie " + carArriveCnt);
                    continue;
                }
               
                // 不是优先车辆
                if (!car.isProiority()) {
                    // 查看是否优先车辆进入该路口
                    if ((tmpCar=proiorityCarIntoRoad(cross, road, car.getNextRoadId(), roadsList))!=null) {
                        car.waiting = tmpCar;
                        break;
                    }
                }
                nextRoad = roads.get(car.getNextRoadId());
                int dir = cross.getTurnDir(car.getCurRoadId(), car.getNextRoadId());
                if (dir == Cross.STRAIGHT) {
                    // 直行
                    if (moveToNextRoad(cross, road, firstChannel, car, nextRoad)) {
                        continue;
                    } else {
                        break;
                    }
                } else if (dir == Cross.LEFT) {
                    if (turnDirConflict(cross, road, nextRoad, Cross.STRAIGHT, car,car.isProiority())) {
                        break;
                    }
                    // 左转进入
                    if (moveToNextRoad(cross, road, firstChannel, car, nextRoad)) {
                        continue;
                    } else {
                        break;
                    }
                } else {
                    // 右转
                    // 直行冲突判断
                    if (turnDirConflict(cross, road, nextRoad, Cross.STRAIGHT,  car,car.isProiority())) {
                        break;
                    }
                    // 左转冲突判断
                    if (turnDirConflict(cross, road, nextRoad, Cross.LEFT,  car,car.isProiority())) {
                        break;
                    }
                    // 可以右转
                    if (moveToNextRoad(cross, road, firstChannel, car, nextRoad)) {
                        continue;
                    } else {
                        break;
                    }

                }

            }

            k++;
            if (cnt == roadsList.size()) {
                cross.isWait = false;
            }
        }

    }

    /**
     * 进入下一道路,可以到达end状态，放回true
     * 
     * @param cross
     * @param road
     * @param firstChannel
     * @param car
     * @param nextRoad
     * @return
     */
    public static boolean moveToNextRoad(Cross cross, Road road, Channel firstChannel, Car car, Road nextRoad) {
        // 获取可进入的车道
        Channel chan = nextRoad.getIntoChannels(cross.getId());
        if (chan == null) {
            // 全部终态，
            car.setCurRoadDis(firstChannel.road.getLength());
            car.setFlag(Car.END);
            // 车道终结态后调度该车道
            firstChannel.driveCar(false);
            firstChannel.road.createCarSequeue(cross.getId());
            driveCarInitList(crosses.get(firstChannel.road.getFrom()), firstChannel.road);
            decideCrossWaitNextRoad(cross);
            
            return true;
        }
        if (chan.moveInACar(firstChannel, car)) {
            firstChannel.channel.poll();
        } else {
            // 前车是等待状态
            if (car.getFlag() == Car.WAIT) {
                car.waiting = chan.channel.getLast();
                return false;
            }
        }
        // 车道终结态后调度该车道
        firstChannel.driveCar(false);
        firstChannel.road.createCarSequeue(cross.getId());
        driveCarInitList(crosses.get(firstChannel.road.getFrom()), firstChannel.road);
        decideCrossWaitNextRoad(cross);
        return true;
    }
    public static void processDeadLock(){
        for (Cross cross : crossList) {
            //logger.info
            if(cross.isWait){
                List<Road> list = cross.getRoadList();
                int k=0;
                Road road;
                Road nextRoad;
                Car car;
                for(;k<list.size();k++){
                   road=list.get(k);
                   car = road.getFirst(cross.getId());
                   if(car==null) continue;
                   if(car.isPreset()) {
                       if(changenum<presetlimit) {
                           nextRoad = cross.getRelaxedChannel(road.getId());
                           car.replaceLastPath(nextRoad.getId());
                           cross.lockDelayTime=20;
                           changenum++;
                           continue;
                       }
                   }
                   nextRoad = cross.getRelaxedChannel(road.getId());
                   car.replaceLastPath(nextRoad.getId());
                   cross.lockDelayTime=20;
                   continue;
                }
            }
        
        }
    }
    
    /* 车库中的车辆上路行驶 */
    public static void driveCarInitList(boolean proiority) {
        Car c;
        Car lastCar;
        Road nextRoad;
        Channel chan;
        int sum;
        int has;
        for (Cross cross : crossList) {
            LinkedList<Car> carlist = carInGarage.get(cross.getId());
            if (carlist == null)
                continue;
            if(carlist.isEmpty()) continue;
            for(int i=0;i<carlist.size();i++){
                c = carlist.get(i);
                if (proiority && !c.isProiority()) {
                    break;
                }
                if(c.isPreset()){
                    if (c.getRealTime() <= time) {
                        nextRoad = roads.get(c.getCurRoadId());
                        chan = nextRoad.getIntoChannels(cross.getId());
                        if (chan == null)
                            continue;
                        if(!chan.channel.isEmpty()){
                            if ((lastCar = chan.channel.getLast()).getFlag() == Car.WAIT) {
                                int maxSpeed = Math.min(nextRoad.getSpeed(), c.getSpeed());
                                if (maxSpeed >= lastCar.getCurRoadDis())
                                    continue;
                            }
                         
                        }
                        chan.intoNewCar(c);
                        carlist.remove(i);
                        i--;
                        carAllCnt++;
                        presetCnt--;
                        System.out.println("车" + c.getId() + "开始上路 总数：" + carAllCnt + "  当前路上有："
                                + (carAllCnt - carArriveCnt) + " 已到达" + carArriveCnt);
                    } else {
                        if (!c.isProiority())
                            break;
                    }
                }else{
                    if(presetCnt!=0) continue; 
                    if(cctime>0)continue;
                    if(!proiority){
                        if(c.isProiority())continue;
                    }
                    if (c.getPlanTime() <= time) {
                        if(cross.lockDelayTime>0) continue;
                        nextRoad = findNextPath(cross,null,c);
                        chan = nextRoad.getIntoChannels(cross.getId());
                        if (chan == null){
                            continue;
                        }
//                        if(chan.road.getChannel())
                        if(!chan.channel.isEmpty()){
                            if(chan.channel.getLast().getFlag()==Car.WAIT)
                                if(presetCnt==0) 
                                    break;
                                else 
                                    continue;
                        }
                        sum =  nextRoad.getMaxCarNum();
                        has = nextRoad.getCurHaveCarNum(cross.getId());
                        if((cross.getCurCarNum()*1.0/cross.getMaxCarNum())>1.0/8)
                            if(presetCnt==0) 
                                break;
                            else 
                                continue;
                        if(presetCnt==0){
                            if((carAllCnt - carArriveCnt)>3000) break;
                        }else{
                            if((carAllCnt - carArriveCnt)>200) continue;
                        }
                        
                        if(has>sum*0.3){//map2
                            if(presetCnt==0) 
                                break;
                            else 
                                continue;
                        }
                        chan.intoNewCar(c);
                        carlist.remove(i);
                        i--;
                        carAllCnt++;
                        c.addPath(nextRoad.getId());
                        c.setRealTime(time);;
                        System.out.println("车" + c.getId() + "开始上路 总数：" + carAllCnt + "  当前路上有："
                                + (carAllCnt - carArriveCnt) + " 已到达" + carArriveCnt);
                    } else {
                        if (!c.isProiority())
                            break;
                    }
                }
                
            }

        }
        
    }
    public static void driveCarInitList(Cross cross, Road curRoad) {
        Car c;
        Car lastCar;
        Channel chan;
        int maxSpeed;
        Road nextRoad;
        int sum;
        int has;
        LinkedList<Car> carlist = carInGarage.get(cross.getId());
        if (carlist == null)
            return;
        if (carlist.isEmpty())
            return;
        for (int i = 0; i < carlist.size(); i++) {
            c = carlist.get(i);
            if (!c.isProiority()) {
                return;
            }
            if(c.isPreset()){
                if (c.getRealTime() <= time) {
                    if (c.getCurRoadId() != curRoad.getId())
                        continue;
                    chan = curRoad.getIntoChannels(cross.getId());
                    if (chan == null)
                        return;
                    if (!chan.channel.isEmpty()) {
                        if ((lastCar = chan.channel.getLast()).getFlag() == Car.WAIT) {
                            maxSpeed = Math.min(curRoad.getSpeed(), c.getSpeed());
                            if (maxSpeed >= lastCar.getCurRoadDis())
                                continue;
                        }
                    }
                    chan.intoNewCar(c);
                    carlist.remove(i);
                    i--;
                    carAllCnt++;
                    presetCnt--;
                    System.out.println("车" + c.getId() + "开始上路 总数：" + carAllCnt + "  当前路上有：" + (carAllCnt - carArriveCnt)
                            + " 已到达" + carArriveCnt);
                } else {
                    if (!c.isProiority())
                        break;
                }
            }else{
                if(presetCnt>0) continue;
                if(cctime>0)continue;
              if (c.getPlanTime() <= time) {
                  if(cross.lockDelayTime>0) continue;
                  nextRoad = findNextPath(cross,null,c);
                  chan = nextRoad.getIntoChannels(cross.getId());
                  if (chan == null){
                      continue;
                  }
                  if(!chan.channel.isEmpty()){
                      if ((lastCar = chan.channel.getLast()).getFlag() == Car.WAIT) {
//                          maxSpeed = Math.min(curRoad.getSpeed(), c.getSpeed());
//                          if (maxSpeed >= lastCar.getCurRoadDis())
//                              continue;
//                          continue;
                          break;
                      }
                  }
                  sum =  nextRoad.getMaxCarNum();
                  has = nextRoad.getCurHaveCarNum(cross.getId());
                  if((cross.getCurCarNum()*1.0/cross.getMaxCarNum())>1.0/10)
                          break;
                  if(presetCnt==0){
                      if((carAllCnt - carArriveCnt)>3000) break;
                  }else{
                      if((carAllCnt - carArriveCnt)>200) continue;
                  }
                  
                  if(has>sum*0.3){
                      if(presetCnt==0) 
                          break;
                      else 
                          continue;
                  }
                  chan.intoNewCar(c);
                  carlist.remove(i);
                  i--;
                  carAllCnt++;
                  c.addPath(nextRoad.getId());
                  c.setRealTime(time);;
                  System.out.println("车" + c.getId() + "开始上路 总数：" + carAllCnt + "  当前路上有："
                          + (carAllCnt - carArriveCnt) + " 已到达" + carArriveCnt);
              } else {
                  if (!c.isProiority())
                      break;
              }
            }
        }

    }
    public static Road findNextPath(Cross cross,Road curRoad,Car car){
        car.waiting=car;
        int shortValue=0;
        List<Road> roadList = cross.getRoadList();
        LinkedList<Road> priList = new LinkedList<>();
        for(Road r:roadList){
            if(curRoad!=null&&r.getId()==curRoad.getId()) continue;//原来的道路跳过
            if(!r.isAccessibleInto(cross.getId()))continue;
            shortValue = shorPathMap.get(r.getNextCrossId(cross.getId())).get(car.getTo());
            r.mark = r.getLength()+shortValue;
            priList.add(r);
        }
        Collections.sort(priList, new Comparator<Road>(){
            @Override
            public int compare(Road o1, Road o2) {
                return o1.mark-o2.mark;
            }
            
        });
        Channel ch=null;
        Car tmpCar=null;
        //TODO 根据路况选择下一条道路 这里可以优化
        for(Road r : priList){
            ch = r.getIntoChannels(cross.getId());
            if(ch==null) continue;
            if(ch.channel.isEmpty()) return r;
            tmpCar = ch.channel.getLast();
            if(tmpCar.getFlag()==Car.END) return r; 
        }
        int maxCarNum=0;
        Road res=priList.get(0);
        
        for(Road r : priList){
            ch = r.getIntoChannels(cross.getId());
            if(ch==null){
                res=r;
                continue;
            }
            tmpCar = ch.channel.getLast();
            if(tmpCar.getFlag()==Car.WAIT){
                Car cc;
                if((cc=tmpCar.findWaitChain())==null||car.equals(cc)){
                    //成环 死锁
                    System.out.println("----");
//                    processDeadLock();
                    cross.lockDelayTime=20;
                    continue;
                }
            }
//            return r;
        }
//        processDeadLock();
        return res;
    }
    
    // TODO proiorityCarIntoRoad
    public static Car proiorityCarIntoRoad(Cross cross, Road curRoad, int intoRoadId, List<Road> roadsList) {
        Car tmpCar = null;
        for (Road r : roadsList) {
            if (r.getId() == curRoad.getId())
                continue;
            if ((tmpCar = r.getFirst(cross.getId())) != null&&tmpCar.isProiority()) {
                if(tmpCar.getTo()== cross.getId()){
                    int nextRid = cross.getTidByByStraight(r.getId());
                    if(nextRid==intoRoadId){
                        return null;
                    }
                    return tmpCar;
                }
                if (tmpCar.getNextRoadId() == r.getId()) {
                    return tmpCar;
                }
            }
        }
        return null;
    }

    public static void decideCrossWaitNextRoad(Cross cross){
        List<Road> roadlist = cross.getRoadList();
        Car c;
        Channel ch;
        Car tmpCar;
        Road nextRoad=null;
        for(Road r :roadlist){
            if((c = r.getFirst(cross.getId()))!=null){
                if(c.getTo()!=cross.getId()&&c.getNextRoadId()==-1){
                    nextRoad = findNextPath(cross, r, c);
                    c.addPath(nextRoad.getId());
                }
            }
        }
    }
    
    public static boolean turnDirConflict(Cross cross, Road curRoad, Road nextRoad, int dir, Car car,boolean proiority) {
        Car tmpCar = null;
        int tmpDir;
        int dirRoadId = cross.getRidFromDir(nextRoad.getId(), dir);
        if (dirRoadId != -1) {
            tmpCar = roads.get(dirRoadId).getFirst(cross.getId());
            if (tmpCar != null) {
                if (proiority && !tmpCar.isProiority())
                    return false;
                if (tmpCar.getTo() == cross.getId()) {
                 // 即将到站的为直行优先级
                    if(dir == Cross.STRAIGHT) {
                        car.waiting = tmpCar;
                        return true;
                    }
                    return false;
                }
                tmpDir = cross.getTurnDir(tmpCar.getCurRoadId(), tmpCar.getNextRoadId());
                if (tmpDir == dir) {
                    car.waiting = tmpCar;
                    return true;// 冲突
                }
            }
        }
        return false;
    }
    public static void initShortPathMap() {
        for (Cross cross : crossList) {
            findShortBydijkstra(cross.getId());
        }
    }
    public static void findShortBydijkstra(int start) {
        HashSet<Integer> visit = new HashSet<>();
        HashMap<Integer, Integer> dist = new HashMap<>();
        dist.put(start, 0);
        visit.add(start);
        Cross startCross = crosses.get(start);
        List<Integer> srids = startCross.getRids();
        for (int i = 0; i < srids.size(); i++) {
            Road r = roads.get(srids.get(i));
            if (r.getIsDuplex()) {// 双向道路的坑
                dist.put(r.getTo() == start ? r.getFrom() : r.getTo(), r.getLength());
            } else {
                if (r.getTo() != start) {
                    dist.put(r.getTo(), r.getLength());
                }
            }
        }
        while (visit.size() < crosses.size()) {
            int nextCid = findNextShort(dist, visit);
            visit.add(nextCid);
            int to = -1;
            if (nextCid != -1) {
                Cross nextCross = crosses.get(nextCid);
                List<Integer> nextRids = nextCross.getRids();
                for (int i = 0; i < nextRids.size(); i++) {
                    Road r = roads.get(nextRids.get(i));
                    // 双向总有一条路可以
                    if (r.getIsDuplex()) {
                        if (nextCid == r.getTo()) {
                            to = r.getFrom();
                        } else {
                            to = r.getTo();
                        }
                        if (visit.contains(to))
                            continue;
                        Integer twei = dist.get(to);
                        if (twei != null) {
                            if (r.getLength() + dist.get(nextCid) < twei) {
                                twei = r.getLength() + dist.get(nextCid);
                                dist.put(to, twei);
                            }
                        } else {
                            dist.put(to, r.getLength() + dist.get(nextCid));
                        }
                    } else {
                        to = r.getTo();
                        if (visit.contains(r.getTo()))
                            continue;
                        Integer twei = dist.get(r.getTo());
                        if (twei != null) {
                            if (r.getLength() + dist.get(nextCid) < twei) {
                                twei = r.getLength() + dist.get(nextCid);
                                dist.put(r.getTo(), twei);
                            }
                        } else {
                            dist.put(r.getTo(), r.getLength() + dist.get(nextCid));
                        }
                    }

                }
            } else {
                System.out.println("start--" + start);
                System.out.println("dist size:" + dist.size());
                System.out.println("visit size:" + visit.size());
                System.out.println("错误退出");
                System.exit(1);
            }
        }
        shorPathMap.put(start, dist);

    }
    private static int findNextShort(HashMap<Integer, Integer> dist, HashSet<Integer> visit) {
        int cid = -1;
        int minWeight = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> entry : dist.entrySet()) {
            if (!visit.contains(entry.getKey())) {
                if (entry.getValue() < minWeight) {
                    minWeight = entry.getValue();
                    cid = entry.getKey();
                }
            }
        }
        return cid;

    }
    public static void initRead(String carPath, String roadPath, String crossPath,String presetAnswerPath) {
        File carInput = new File(carPath);
        Scanner sc;
        try {
            Pattern p = Pattern.compile("[-]{0,1}\\d+");
            sc = new Scanner(carInput);
            // 读入 car
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.charAt(0) == '#')
                    continue;
                Matcher m = p.matcher(line);
                Car car = new Car();
                if (m.find()) {
                    car.setId(Integer.parseInt(m.group()));
                }
                if (m.find()) {
                    car.setFrom(Integer.parseInt(m.group()));
                }
                if (m.find()) {
                    car.setTo(Integer.parseInt(m.group()));

                }
                if (m.find()) {
                    car.setSpeed(Integer.parseInt(m.group()));

                }
                if (m.find()) {
                    car.setPlanTime(Integer.parseInt(m.group()));

                }
                if (m.find()) {
                    car.setProiority(m.group().equals("1"));

                }
                if (m.find()) {
                    car.setPreset(m.group().equals("1"));

                }
                cars.add(car);
                carMap.put(car.getId(), car);
            }
            sc.close();
            // 读入 road
            sc = new Scanner(new File(roadPath));
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.charAt(0) == '#')
                    continue;
                Matcher m = p.matcher(line);
                Road road = new Road();
                if (m.find()) {
                    road.setId(Integer.parseInt(m.group()));
                }
                if (m.find()) {
                    road.setLength(Integer.parseInt(m.group()));
                }
                if (m.find()) {

                    road.setSpeed(Integer.parseInt(m.group()));
                }
                if (m.find()) {
                    road.setChannel(Integer.parseInt(m.group()));

                }
                if (m.find()) {
                    road.setFrom(Integer.parseInt(m.group()));

                }
                if (m.find()) {
                    road.setTo(Integer.parseInt(m.group()));

                }
                if (m.find()) {
                    road.setIsDuplex(Integer.parseInt(m.group()) == 1);
                }
                roads.put(road.getId(), road);
                road.init();
            }
            sc.close();
            // 读入 cross
            sc = new Scanner(new File(crossPath));
            // System.out.println(crossPath);
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.charAt(0) == '#')
                    continue;
                Matcher m = p.matcher(line);
                Cross cross = new Cross();
                if (m.find()) {
                    cross.setId(Integer.parseInt(m.group()));
                }
                if (m.find()) {

                    cross.setNorth(Integer.parseInt(m.group()));
                }
                if (m.find()) {
                    cross.setEast(Integer.parseInt(m.group()));
                }
                if (m.find()) {
                    cross.setSouth(Integer.parseInt(m.group()));
                }
                if (m.find()) {
                    cross.setWest(Integer.parseInt(m.group()));

                }
                cross.initRids();// 把路口信息放链表 方便遍历
                crosses.put(cross.getId(), cross);
                crossList.add(cross);
            }
            sc.close();
            //读入presetAnswer
            sc = new Scanner(new File(presetAnswerPath));
            int carId =0;
            Car car=null;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.charAt(0) == '#')
                    continue;
                Matcher m = p.matcher(line);
                if (m.find()) {
                    carId = Integer.parseInt(m.group());
                    car = carMap.get(carId);
                }
                if (m.find()) {
                    car.setRealTime(Integer.parseInt(m.group()));
                }
                LinkedList<Integer> list = new LinkedList<>();
                while (m.find()) {
                    list.add(Integer.parseInt(m.group()));
                }
                car.setRealPath(list);
                presetAllCnt++;
            }
            presetCnt=presetAllCnt;
            presetlimit=(int) (presetAllCnt*0.1);
            sc.close();

        } catch (Exception e) {
            System.out.println("读入失败");
            e.printStackTrace();
            System.exit(1);

        }

    }
    public static void writeAnswer(String answerPath) {
        System.out.println("begin to write:" + answerPath);
        try {
            PrintWriter write = new PrintWriter(new File(answerPath));
            for (Car car : cars) {
//                if(!car.isPreset()){
                    write.println(car.toString());
//                }
                
            }
            write.flush();
            write.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}
    
    
