package com.huawei;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
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

    public static List<Car> cars = new ArrayList<>();
    public static HashMap<Integer, Road> roads = new HashMap<>();
    public static HashMap<Integer, Cross> crosses = new HashMap<>();
    private static final Logger logger = Logger.getLogger(Main.class);
    public static HashMap<Integer, LinkedList<Car>> carInGarage = new HashMap<>();
    public static List<Cross> crossList = new ArrayList<>();
    public static HashMap<String, LinkedList<Integer>> shortPath = new HashMap<>();
    public static boolean waiting = false;
    public static boolean isWait = false;
    public static int carInRoadCnt = 0;
    public static int carWaitCnt = 0;
    public static int carArriveCnt = 0;
    public static int carAllCnt = 0;
    public static int lockDelayTime = 30;
    
    public static void main(String[] args) {

        if (args.length != 4) {
            logger.error("please input args: inputFilePath, resultFilePath");
            return;
        }
//        System.out.println(20>30*0.75);
//        return ;
        logger.info("Start...");
//        System.exit(1);
        String carPath = args[0];
        String roadPath = args[1];
        String crossPath = args[2];
        String answerPath = args[3];
        logger.info("carPath = " + carPath + " roadPath = " + roadPath + " crossPath = " + crossPath
                + " and answerPath = " + answerPath);

        logger.info("start read input files");
        initRead(carPath, roadPath, crossPath);
        init();
        initShortPath();
        start();

        logger.info("Start write output file");
         writeAnswer(answerPath);

        logger.info("End...");
    }

    public static void start() {
        System.out.println("开始调度。。。");
        int time = 0;
        for (time = 0; carArriveCnt != cars.size(); time++) {
            System.out.println("当前时间" + time);
            for (Road r : roads.values()) {
                driveAllCarJustOnRoadToEndState(r, true);
            }
            while (carWaitCnt != 0) {
                if (isWait) {
                    waiting = true;
//                    waitLockTime = 20;
//                    System.out.println("dead lock!!!");
                    for(Cross cro:crossList){
                        if(cro.isWait){
                            cro.lockDelayTime=lockDelayTime;
                        }
                    }
//                    System.out.println("Dead lock!!!");
                }
                isWait = true;
                for (Cross cross : crossList) {
                    cross.isWait = true;
                    driveAllWaitCar(cross);
                }
            }
            driveCarInGarage(time);
        }
        System.out.println("总调度时间：" + (time - 1));
    }

    public static void init() {
        // init All shortPath
        initShortPath();
        // init car to carInGarage
        Car c;
        LinkedList<Car> list;
        for (Car car : cars) {
            car.setShortPath(shortPath.get(car.getFrom() + "-" + car.getTo()));
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

    public static void initShortPath() {
        for (Cross cross : crossList) {
            findShortBydijkstra(cross.getId());
        }
        // //outPut shortPath
        // for(Map.Entry<String, List<Integer> >entry : shortPath.entrySet()){
        // System.out.println(entry.getKey());
        // System.out.println(entry.toString());
        // logger.info(entry.toString());
        // }
    }

    /**
     * 调整所有道路上在道路上的车辆，让道路上车辆前进，只要不出路口且可以到达终止状态的车辆
     * 分别标记出来等待的车辆（要出路口的车辆，或者因为要出路口的车辆阻挡而不能前进的车辆）
     * 和终止状态的车辆（在该车道内可以经过这一次调度可以行驶其最大可行驶距离的车辆）
     * 
     * @param road
     */
    public static void driveAllCarJustOnRoadToEndState(Road road, boolean b) {
        List<Channel> fchannels = road.getFchannels();
        for (Channel channel : fchannels) {
            channel.driveCar(b);
        }

        if (road.getIsDuplex()) {
            List<Channel> bchannels = road.getBchannels();
            for (Channel channel : bchannels) {
                channel.driveCar(b);
            }
        }
    }

    public static void driveAllWaitCar(Cross cross) {
        List<Integer> rids = cross.getRids();
        List<Road> roadsList = new ArrayList<>();
        for (int id : rids) {
            roadsList.add(roads.get(id));
        }
        int k = 0;
        Road road;
        Road nextRoad;
        Car car;
        Car tmpCar;
        Channel firstChannel = null;
        int tmpDir;
        int cnt = 0;
        // each roads

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
                cnt = 0;
                firstChannel = road.getFirstChannel(cross.getId());
                // 到达终点
                if (car.getTo() == cross.getId()) {
                    car.setFlag(Car.ARRIVE);
                    carArriveCnt++;
                    // 从道路删除
                    road.moveOutRoad(cross.getId());
                    firstChannel.driveCar(false);
                    System.out.println("arrvie " + carArriveCnt);
                    car.addPath(road.getId());
                    continue;
                }
                if (waiting && cross.isWait) {
                    // 死锁
                    Road newRoad = cross.getRelaxedChannel(road.getId());
                    Channel ch = newRoad.getIntoChannels(cross.getId());
                    if (ch != null) {
                        if (ch.moveInACar(firstChannel, car)) {
                            road.moveOutRoad(cross.getId());
                            int newStart = newRoad.getFrom() == cross.getId() ? newRoad.getTo() : newRoad.getFrom();
                            LinkedList<Integer> list = new LinkedList<>();
                            if(newStart== car.getTo())break;
                            Utils.copyLinkedList(list, shortPath.get(newStart + "-" + car.getTo()));
                            if(list.get(0)==newRoad.getId()){
                                list = findShortBydijkstra(newStart,car.getTo(),new int[]{newRoad.getId()});
                            }
                            list.addFirst(newRoad.getId());
                            car.setPos(0);
                            car.setShortPath(list);
                            car.addPath(firstChannel.road.getId());
                            firstChannel.driveCar(false);
                            continue;
                        } else {
                            // 如果前车是等待状态
                            if (car.getFlag() == Car.WAIT) {
                                break;
                            }
                            //没过，开到路口
                        }
                    } 
                    //全部终态，没过，开到路口
                    car.setCurRoadDis(road.getLength());
                    car.setFlag(Car.END);
                    int newStart = newRoad.getFrom() == cross.getId() ? newRoad.getTo() : newRoad.getFrom();
                    LinkedList<Integer> list = new LinkedList<>();
                    if(newStart== car.getTo())break;
                    Utils.copyLinkedList(list, shortPath.get(newStart + "-" + car.getTo()));
                    if(list.get(0)==newRoad.getId()){
                        list = findShortBydijkstra(newStart,car.getTo(),new int[]{newRoad.getId(),road.getId()});
                    }
                    list.addFirst(newRoad.getId());
                    list.addFirst(road.getId());
                    car.setPos(0);
                    car.setShortPath(list);
                    // 车道终结态后调度该车道
                    firstChannel.driveCar(false);
                    continue;   
                }

                nextRoad = roads.get(car.getNextRoadId());
                int dir = cross.getTurnDir(car.getCurRoadId(), car.getNextRoadId());
                if (dir == Cross.STRAIGHT) {
                    // 直行
                    // 获取可进入的车道
                    Channel chan = nextRoad.getIntoChannels(cross.getId());
                    if (chan == null) {
                        // 全部终态，且没有位置
                        car.setCurRoadDis(firstChannel.road.getLength());
                        car.setFlag(Car.END);
                        // 车道终结态后调度该车道
                        firstChannel.driveCar(false);
                        continue;
                    }
                    if (chan.moveInACar(firstChannel, car)) {
                        car.addPath(firstChannel.road.getId());
                        road.moveOutRoad(cross.getId());
                    } else {
                        // 前车是等待状态
                        if (car.getFlag() == Car.WAIT) {
                            break;
                        }

                    }
                    // 车道终结态后调度该车道
                    firstChannel.driveCar(false);
                    continue;

                } else if (dir == Cross.LEFT) {
                    int dirRoadId = cross.getRidFromDir(nextRoad.getId(), Cross.STRAIGHT);
                    if (dirRoadId != -1) {
                        tmpCar = roads.get(dirRoadId).getFirst(cross.getId());
                        if (tmpCar != null) {
                            // 那一辆车即将到站
                            if (tmpCar.getTo() == cross.getId()) {
                                break;// 即将到站的为直行优先级
                            }
                            tmpDir = cross.getTurnDir(tmpCar.getCurRoadId(), tmpCar.getNextRoadId());
                            if (tmpDir == Cross.STRAIGHT) {
                                break;// 冲突
                            }

                        }
                    }

                    // 左转进入
                    // 获取可进入的车道
                    Channel chan = nextRoad.getIntoChannels(cross.getId());
                    if (chan == null) {
                        // 全部终态，
                        car.setCurRoadDis(firstChannel.road.getLength());
                        car.setFlag(Car.END);
                        // 车道终结态后调度该车道
                        firstChannel.driveCar(false);
                        continue;
                    }
                    if (chan.moveInACar(firstChannel, car)) {
                        car.addPath(firstChannel.road.getId());
                        road.moveOutRoad(cross.getId());
                    } else {
                        // 前车是等待状态
                        if (car.getFlag() == Car.WAIT) {
//                            road.moveOutRoad(cross.getId());
                            break;
                        }

                    }
                    // 车道终结态后调度该车道
                    firstChannel.driveCar(false);
                    continue;

                } else {
                    // 右转
                    // 直行冲突判断
                    int dirRoadId = cross.getRidFromDir(nextRoad.getId(), Cross.STRAIGHT);
                    if (dirRoadId != -1) {
                        tmpCar = roads.get(dirRoadId).getFirst(cross.getId());
                        if (tmpCar != null) {
                            // 那一辆车即将到站
                            if (tmpCar.getTo() == cross.getId()) {
                                break;
                            }
                            tmpDir = cross.getTurnDir(tmpCar.getCurRoadId(), tmpCar.getNextRoadId());
                            if (tmpDir == Cross.STRAIGHT) {
                                break;// 冲突
                            }

                        }
                    }

                    // 左转冲突判断
                    dirRoadId = cross.getRidFromDir(nextRoad.getId(), Cross.LEFT);
                    if (dirRoadId != -1) {
                        tmpCar = roads.get(dirRoadId).getFirst(cross.getId());
                        if (tmpCar != null) {
                            // 那一辆车即将到站
                            if (tmpCar.getTo() == cross.getId()) {
                                break;// 冲突
                            }
                            tmpDir = cross.getTurnDir(tmpCar.getCurRoadId(), tmpCar.getNextRoadId());
                            if (tmpDir == Cross.LEFT) {
                                break;// 冲突
                            }

                        }
                    }

                    // 右转
                    // 获取可进入的车道
                    Channel chan = nextRoad.getIntoChannels(cross.getId());
                    if (chan == null) {
                        // 全部终态，
                        car.setCurRoadDis(firstChannel.road.getLength());
                        car.setFlag(Car.END);
                        // 车道终结态后调度该车道
                        firstChannel.driveCar(false);
                        continue;
                    }
                    if (chan.moveInACar(firstChannel, car)) {
                        car.addPath(firstChannel.road.getId());
                        road.moveOutRoad(cross.getId());
                    } else {
                        // 前车是等待状态
                        if (car.getFlag() == Car.WAIT) {
                            break;
                        }
                    }
                    // 车道终结态后调度该车道
                    firstChannel.driveCar(false);
                    continue;
                }

            }
            k++;
            if (cnt == roadsList.size()) {
                cross.isWait = false;
            }
        }

    }

    /* 车库中的车辆上路行驶 */
    public static void driveCarInGarage(int time) {
        Car c;
        Road curRoad;
        Channel chan;
        int sum;
        int has;
        for (Cross cross : crossList) {
            LinkedList<Car> carlist = carInGarage.get(cross.getId());
            if(cross.lockDelayTime-->0) continue;
            if (carlist == null)
                continue;
            while (!carlist.isEmpty()) {
                c = carlist.peek();

                if (c.getPlanTime() <= time && (carAllCnt - carArriveCnt)<5800) {
                    curRoad = roads.get(c.getCurRoadId());
                    chan = curRoad.getIntoChannels(cross.getId());

                    if (chan == null)
                        break;
                    if (chan.road.getChannel()>1&&chan.cid == chan.road.getChannel() - 1) {
                        // 只剩一个车道不上 ，这里可以调
                        break;
                    }
                    sum =  curRoad.getMaxCarNum();
                    has = curRoad.getCurHaveCarNum(cross.getId());
                    if(has>sum*0.18){
                        break;
                    }
                    c.setPlanTime(time);
                    chan.intoNewCar(c);
                    carlist.poll();
                    carAllCnt++;
                    System.out.println("车" + c.getId() + "开始上路 总数：" + carAllCnt + "  当前路上有："
                            + (carAllCnt - carArriveCnt) + " 已到达" + carArriveCnt+" 未上路" + (cars.size()-carAllCnt));
                } else {
                    break;
                }
            }

        }
    }

    public static void findShortBydijkstra(int start) {
        HashSet<Integer> visit = new HashSet<>();
        HashMap<Integer, Double> dist = new HashMap<>();
        HashMap<Integer, Integer> path = new HashMap<>();
        visit.add(start);
        Cross startCross = crosses.get(start);
        List<Integer> srids = startCross.getRids();
        for (int i = 0; i < srids.size(); i++) {
            Road r = roads.get(srids.get(i));
            if (r.getIsDuplex()) {// 双向道路的坑
                dist.put(r.getTo() == start ? r.getFrom() : r.getTo(), r.getWeigth());
                path.put(r.getTo() == start ? r.getFrom() : r.getTo(), r.getId());
            } else {
                if (r.getTo() != start) {
                    dist.put(r.getTo(), r.getWeigth());
                    path.put(r.getTo(), r.getId());
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
                        Double twei = dist.get(to);
                        if (twei != null) {
                            if (r.getWeigth() + dist.get(nextCid) < twei) {
                                twei = r.getWeigth() + dist.get(nextCid);
                                dist.put(to, twei);
                                path.put(to, r.getId());
                            }
                        } else {
                            dist.put(to, r.getWeigth() + dist.get(nextCid));
                            path.put(to, r.getId());
                        }
                    } else {
                        to = r.getTo();
                        if (visit.contains(r.getTo()))
                            continue;
                        Double twei = dist.get(r.getTo());
                        if (twei != null) {
                            if (r.getWeigth() + dist.get(nextCid) < twei) {
                                twei = r.getWeigth() + dist.get(nextCid);
                                dist.put(r.getTo(), twei);
                                path.put(r.getTo(), r.getId());
                            }
                        } else {
                            dist.put(r.getTo(), r.getWeigth() + dist.get(nextCid));
                            path.put(r.getTo(), r.getId());
                        }
                    }

                }
            } else {
                System.out.println("start--" + start);
                // System.out.println("end---"+end);
                System.out.println("dist size:" + dist.size());
                System.out.println("visit size:" + visit.size());
                System.out.println("错误退出");
                System.exit(1);
            }
        }
        for (Cross c2 : crossList) {
            if (start == c2.getId())
                continue;
            String key = start + "-" + c2.getId();
            Road r = roads.get(path.get(c2.getId()));
            int last = c2.getId();//
            LinkedList<Integer> spath = new LinkedList<>();
            while (last != start) {
                String tkey = start + "-" + last;
                if (shortPath.containsKey(tkey)) {
                    spath.addAll(0, shortPath.get(tkey));
                    break;
                }
                spath.addFirst(r.getId());
                if (r.getTo() == last) {
                    last = r.getFrom();
                    r = roads.get(path.get(r.getFrom()));
                    if (r == null)
                        break;// 为空 可以结束了。
                } else {
                    last = r.getTo();
                    r = roads.get(path.get(r.getTo()));
                    if (r == null)
                        break;// 为空 可以结束了。
                }

            }
            shortPath.put(key, spath);
        }

    }

    public static LinkedList<Integer> findShortBydijkstra(int start,int end,int []delrids){
        for(int delrid :delrids){
            roads.get(delrid).block=true;
        }
        
        HashSet<Integer> visit = new HashSet<>();
        HashMap<Integer,Double> dist = new HashMap<>();
        HashMap<Integer,Integer> path = new HashMap<>();
        visit.add(start);
        Cross startCross = crosses.get(start);
        List<Integer> srids = startCross.getRids();
        for(int i=0;i<srids.size();i++){
            Road r = roads.get(srids.get(i));
            if(r.getIsDuplex()){//双向道路的坑
                dist.put(r.getTo()==start?r.getFrom():r.getTo(), r.getWeigth());
                path.put(r.getTo()==start?r.getFrom():r.getTo(), r.getId());
            }else{
                if(r.getTo()!=start){
                    dist.put(r.getTo(), r.getWeigth());
                    path.put(r.getTo(), r.getId());
                }     
            }
            
//            path.put(r.getFrom(),r.getId());
        }
        while(visit.size()<crosses.size()){
            int nextCid = findNextShort(dist,visit);
            visit.add(nextCid);
//            System.out.println("add visit:"+nextCid);
//            logger.info("add visit:"+nextCid);
            int to=-1;
            if(nextCid!=-1){
                Cross nextCross = crosses.get(nextCid);
                List<Integer> nextRids = nextCross.getRids();
                for(int i=0;i<nextRids.size();i++){
                    Road r = roads.get(nextRids.get(i));
                    //双向总有一条路可以
                    if(r.getIsDuplex()){
                        if(nextCid==r.getTo()){
                            to = r.getFrom();
                        }else{
                            to = r.getTo();
                        }
                        if(visit.contains(to)) continue;
                        Double twei = dist.get(to);
                        if(twei!=null){
                            if(r.getWeigth()+dist.get(nextCid)<twei){
                                twei = r.getWeigth()+dist.get(nextCid);
                                dist.put(to, twei);
                                path.put(to,r.getId());
                            }
                        }else{
                            dist.put(to, r.getWeigth());
                            path.put(to,r.getId());
                        }
                        //path.put(r.getFrom(),r.getId());
                    }else {
                        to=r.getTo();
                        if(visit.contains(r.getTo())) continue;
                        Double twei = dist.get(r.getTo());
                        if(twei!=null){
                            if(r.getWeigth()<twei){
                                twei = r.getWeigth()+dist.get(nextCid);
                                dist.put(r.getTo(), twei);
                                path.put(r.getTo(),r.getId());
                            }
                        }else{
                            dist.put(r.getTo(), r.getWeigth()+dist.get(nextCid));
                            path.put(r.getTo(),r.getId());
                        }
//                        path.put(r.getTo(),r.getId());   
                    }
                    
                }
            }else{
                System.out.println("start--"+start);
                System.out.println("end---"+end);
                System.out.println("dist size:"+dist.size());
                System.out.println("visit size:"+visit.size());
                System.out.println("错误退出");
                System.exit(1);
            }
//            System.out.println(visit.size());
            if(visit.contains(end)){
                LinkedList<Integer> newPath = new LinkedList<>();
                Road r = roads.get(path.get(end));
                int last=end;//双向道路的坑
                while(last!=start){
                    newPath.addFirst(r.getId());
                    if(r.getTo()==last){
                        last = r.getFrom();
                        r = roads.get(path.get(r.getFrom()));
                        if(r==null)break;//为空 可以结束了。
                        
                    }else{
                        last = r.getTo();
                        r = roads.get(path.get(r.getTo()));
                        if(r==null)break;//为空 可以结束了。
                    }
                    
                }
                for(int delrid :delrids){
                    roads.get(delrid).block=false;
                }
                return newPath;
            }
        }
        System.out.println("findShortBydijkstra error");
        System.exit(-1);
        return null;
    }
        
    
    private static int findNextShort(HashMap<Integer, Double> dist, HashSet<Integer> visit) {
        int cid = -1;
        double minWeight = Double.MAX_VALUE;
        for (Map.Entry<Integer, Double> entry : dist.entrySet()) {
            if (!visit.contains(entry.getKey())) {
                if (entry.getValue() < minWeight) {
                    minWeight = entry.getValue();
                    cid = entry.getKey();
                }
            }
        }
        return cid;

    }


    public static void initRead(String carPath, String roadPath, String crossPath) {
        File carInput = new File(carPath);
        Scanner sc;
        try {
            Pattern p = Pattern.compile("[-]{0,1}\\d+");
            sc = new Scanner(carInput);
            // sc.nextLine();
            // 读入
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
                cars.add(car);
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
                write.println(car.toString());
            }
            write.flush();
            write.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}