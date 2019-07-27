package com.huawei;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class Judge {
    public static List<Car> cars = new ArrayList<>();
    public static HashMap<Integer, Car> carMap = new HashMap<>();
    public static HashMap<Integer, Road> roads = new HashMap<>();
    public static HashMap<Integer, Cross> crosses = new HashMap<>();
    public static List<Cross> crossList = new ArrayList<>();
    public static HashMap<Integer, LinkedList<Car>> carInGarage = new HashMap<>();
    public static List<Answer> answers = new ArrayList<>();
    public static LinkedList<Car> carsCache = new LinkedList<>();
    private static final Logger logger = Logger.getLogger(Judge.class);
    public static boolean waiting = false;
    public static boolean isWait = false;
    public static int carInRoadCnt = 0;
    public static int carWaitCnt = 0;
    public static int carArriveCnt = 0;
    public static int carAllCnt = 0;
    public static int time = 0;

    public Judge() {

    }

    public static void main(String[] args) {
        if(args.length<5){
            System.out.println("error main args less than 5 ");
            return;
        }
        String carPath = args[0];
        String roadPath = args[1];
        String crossPath = args[2];
        String presetAnswerPath = args[3];
        String answerPath = args[4];
        initRead(carPath, roadPath, crossPath, presetAnswerPath, answerPath);
        init();
        start();
    }

    public static void start() {
        System.out.println("开始调度。。。");
        System.out.println(carMap.size());
        int lockCnt=0;
        for (time = 1; carArriveCnt != carMap.size(); time++) {
            System.out.println("当前时间" + time);
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
                    if(lockCnt>10000){
                        System.out.println("Dead Lock!!!");
                        System.exit(1);
                    }
                }
               
                isWait = true;
                for (Cross cross : crossList) {
                    // System.out.println("调度路口"+cross.getId());
                     cross.isWait = true;
                    driveAllWaitCar(cross);
                }

            }
            //所有车上路
            driveCarInitList(false);
        }
        TimeCalc tc = new TimeCalc(time-1, cars);
        tc.calc();
        System.out.println("调度时间：" + (time - 1)+"    "+tc.tSum);
        System.out.println(tc.tPri+"    "+tc.tSumPri);
        System.out.println(tc.tE+"   " + tc.tESum);
        
    }

    public static void init() {
        // init car
        Car c;
        LinkedList<Car> list;
        for (Answer ans : answers) {
            c = carMap.get(ans.getCarId());
            c.setRealTime(ans.getStartTime());
            c.setRealPath(ans.getRoadIds());
            if ((list = carInGarage.get(c.getFrom())) != null) {
                list.add(c);
            } else {
                list = new LinkedList<>();
                list.add(c);
                carInGarage.put(c.getFrom(), list);
            }
        }

        for (LinkedList<Car> ls : carInGarage.values()) {
            Collections.sort(ls);
        }
        Collections.sort(crossList);

    }

    /**
     * 调整所有道路上在道路上的车辆，让道路上车辆前进，只要不出路口且可以到达终止状态的车辆
     * 分别标记出来等待的车辆（要出路口的车辆，或者因为要出路口的车辆阻挡而不能前进的车辆）
     * 和终止状态的车辆（在该车道内可以经过这一次调度可以行驶其最大可行驶距离的车辆）
     * 
     * @param road
     */
    // TODO:driveAllCarJustOnRoadToEndState
    public static void driveJustCurrentRoad(Road road, boolean b) {
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

    // TODO:driveAllWaitCar
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
        Channel firstChannel = null;
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
                firstChannel = road.getFirstChannel(cross.getId());
                // 判断是否到达终点
                if (car.getTo() == cross.getId()) {
                    // 不是优先车辆
                    if (!car.isProiority()) {
                        int nextRid = cross.getTidByByStraight(road.getId());
                        if (nextRid != -1) {
                            // 可能冲突，遍历其他路口，查看是否优先车辆进入该路口
                            if (proiorityCarIntoRoad(cross, road, nextRid, roadsList)) {
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
                    driveCarInitList(true);
                    System.out.println("arrvie " + carArriveCnt);
                    continue;
                }
                // 不是优先车辆
                if (!car.isProiority()) {
                    // 查看是否优先车辆进入该路口
                    if (proiorityCarIntoRoad(cross, road, car.getNextRoadId(), roadsList)) {
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
                    if (turnDirConflict(cross, road, nextRoad, Cross.STRAIGHT, car.isProiority())) {
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
                    if (turnDirConflict(cross, road, nextRoad, Cross.STRAIGHT, car.isProiority())) {
                        break;
                    }
                    // 左转冲突判断
                    if (turnDirConflict(cross, road, nextRoad, Cross.LEFT, car.isProiority())) {
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
            driveCarInitList(true);
            return true;
        }
        if (chan.moveInACar(firstChannel, car)) {
            firstChannel.channel.poll();
        } else {
            // 前车是等待状态
            if (car.getFlag() == Car.WAIT) {
                return false;
            }
        }
        // 车道终结态后调度该车道
        firstChannel.driveCar(false);
        driveCarInitList(true);
        return true;
    }

    /* 车库中的车辆上路行驶 */
    public static void driveCarInitList(boolean proiority) {
        Car c;
        Road curRoad;
        Channel chan;
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
                if (c.getRealTime() <= time) {
                    curRoad = roads.get(c.getCurRoadId());
                    chan = curRoad.getIntoChannels(cross.getId());
                    if (chan == null)
                        continue;
                    if(!chan.channel.isEmpty()){
                        if(chan.channel.getLast().getFlag()==Car.WAIT)
                            continue;
                    }
                    chan.intoNewCar(c);
                    carlist.remove(i);
                    i--;
                    carAllCnt++;
                    System.out.println("车" + c.getId() + "开始上路 总数：" + carAllCnt + "  当前路上有："
                            + (carAllCnt - carArriveCnt) + " 已到达" + carArriveCnt);
                } else {
                    if (!c.isProiority())
                        break;
                }
            }

        }
    }

    // TODO proiorityCarIntoRoad
    public static boolean proiorityCarIntoRoad(Cross cross, Road curRoad, int intoRoadId, List<Road> roadsList) {
        Car tmpCar = null;
        for (Road r : roadsList) {
            if (r.getId() == curRoad.getId())
                continue;
            if ((tmpCar = r.getFirst(cross.getId())) != null&&tmpCar.isProiority()) {
                if(tmpCar.getTo()== cross.getId()){
                    int nextRid = cross.getTidByByStraight(r.getId());
                    return nextRid==intoRoadId;
                }
                if (tmpCar.getNextRoadId() == r.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean turnDirConflict(Cross cross, Road curRoad, Road nextRoad, int dir, boolean proiority) {
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
                    int nextRid = cross.getTidByByStraight(curRoad.getId());
                    return nextRid==nextRoad.getId();
                }
                tmpDir = cross.getTurnDir(tmpCar.getCurRoadId(), tmpCar.getNextRoadId());
                if (tmpDir == dir) {
                    return true;// 冲突
                }
            }
        }
        return false;
    }

    public static void initRead(String carPath, String roadPath, String crossPath, String presetAnswerPath,
            String answerPath) {
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
            // 读入presetAnswer
            sc = new Scanner(new File(answerPath));// answerPath
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.charAt(0) == '#')
                    continue;
                Matcher m = p.matcher(line);
                Answer path = new Answer();
                if (m.find()) {
                    path.setCarId(Integer.parseInt(m.group()));
                }
                if (m.find()) {

                    path.setStartTime(Integer.parseInt(m.group()));
                }
                LinkedList<Integer> list = new LinkedList<>();
                while (m.find()) {
                    list.add(Integer.parseInt(m.group()));
                }
                path.setRoadIds(list);
//                answers.add(path);
            }
            sc.close();
            // 读入answer
            sc = new Scanner(new File(answerPath));// answerPath
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.charAt(0) == '#')
                    continue;
                Matcher m = p.matcher(line);
                Answer path = new Answer();
                if (m.find()) {
                    path.setCarId(Integer.parseInt(m.group()));
                }
                if (m.find()) {

                    path.setStartTime(Integer.parseInt(m.group()));
                }
                LinkedList<Integer> list = new LinkedList<>();
                while (m.find()) {
                    list.add(Integer.parseInt(m.group()));
                }
                path.setRoadIds(list);
                answers.add(path);
            }
            sc.close();

        } catch (Exception e) {
            System.out.println("读入失败");
            e.printStackTrace();
            System.exit(1);

        }

    }
}
