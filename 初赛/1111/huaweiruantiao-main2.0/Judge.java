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
    public static HashMap<Integer,LinkedList<Car>> carInGarage = new HashMap<>();
    public static List<Answer> answers = new ArrayList<>();
    public static LinkedList<Car> carsCache = new LinkedList<>();
    private static final Logger logger = Logger.getLogger(Main.class);
    public static boolean waiting = false;
    public static int carInRoadCnt=0;
    public static int carWaitCnt=0;
    public static int carArriveCnt=0;
    public static int carAllCnt=0;
    public static void main(String[] args) {
        if (args.length != 4) {
            logger.error("please input args: inputFilePath, resultFilePath");
            return;
        }

        logger.info("Start...");

        String carPath = args[0];
        String roadPath = args[1];
        String crossPath = args[2];
        String answerPath = args[3];
        logger.info("carPath = " + carPath + " roadPath = " + roadPath + " crossPath = " + crossPath
                + " and answerPath = " + answerPath);

        logger.info("start read input files");
        initRead(carPath, roadPath, crossPath, answerPath);
        init();
        start();
        logger.info("End...");
    }
    public static void start(){
        System.out.println("开始调度。。。");
        System.out.println(carMap.size());
        int time=0;
        for(time=0;carArriveCnt!=carMap.size();time++){
            System.out.println("当前时间"+time);
            for(Road r : roads.values()){
                driveAllCarJustOnRoadToEndState(r,true);
            }
            while(carWaitCnt!=0){
//                System.out.println(carWaitCnt);
                if(waiting){
                    System.out.println("Dead lock!!!");
                    for(Cross cro:crossList){
                        if(cro.isWait){
                            System.out.print(cro.getId()+" ");
                        }
                    }
                    System.out.println();
                    System.exit(1);
                }
                waiting=true;
                for(Cross cross : crossList){
//                    System.out.println("调度路口"+cross.getId());
                    cross.isWait=true;
                    driveAllWaitCar(cross);
                }
//                for(Road r : roads.values()){
//                    driveAllCarJustOnRoadToEndState(r,false);
//                }

            }
            driveCarInGarage(time);
            
        }
        System.out.println("总调度时间："+(time-1));
    }
    public static void init() {
        // init car
        Car c;
        LinkedList<Car> list;
        for (Answer ans : answers) {
            c = carMap.get(ans.getCarId());
            c.setPath(ans);
            if((list=carInGarage.get(c.getFrom()))!=null){
                list.add(c);
            }else{
                list = new LinkedList<>();
                list.add(c);
                carInGarage.put(c.getFrom(), list);
            }
        }
        
        for(LinkedList<Car> ls :carInGarage.values()){
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
    public static void driveAllCarJustOnRoadToEndState(Road road,boolean b) {
        List<Channel> fchannels = road.getFchannels();
        for (Channel channel : fchannels) {
            channel.driveCar(b);
        }
        
        if(road.getIsDuplex()){
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
        Car tmpCar;
        Channel firstChannel = null;
        int tmpDir;
        int cnt=0;
        // each roads
        
        while (k<roadsList.size()) {
            road = roadsList.get(k);
            // each channels
            while (true) {
                car = road.getFirst(cross.getId());
                //该道路已经调度完，即没有等待状态
                if (car == null){
                    cnt++;
                    break;
                }
                cnt=0;
                firstChannel = road.getFirstChannel(cross.getId());
                // 到达终点
                if (car.getTo() == cross.getId()) {
                    car.setFlag(Car.ARRIVE);
                    carArriveCnt++;
                    // 从道路删除
                    road.moveOutRoad(cross.getId());
                    firstChannel.driveCar(false);
                    System.out.println("arrvie "+carArriveCnt);
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
                        road.moveOutRoad(cross.getId());
                    } else {
                        // 前车是等待状态
                        if (car.getFlag() == Car.WAIT) {
//                            
                            break;
                        }
                       
                    }
                    // 车道终结态后调度该车道
                    firstChannel.driveCar(false);
                    continue;

                } else if (dir == Cross.LEFT) {
                    int dirRoadId = cross.getRidFromDir(nextRoad.getId(), Cross.STRAIGHT);
                    if(dirRoadId!=-1){
                        tmpCar = roads.get(dirRoadId).getFirst(cross.getId());
                        if (tmpCar != null) {
                            //那一辆车即将到站
                            if(tmpCar.getTo()==cross.getId()){
                                break;// 即将到站的为直行优先级
                            }
                            tmpDir = cross.getTurnDir(tmpCar.getCurRoadId(), tmpCar.getNextRoadId());
                            if (tmpDir == Cross.STRAIGHT){
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

                } else {
                    // 右转
                    // 直行冲突判断
                    int dirRoadId = cross.getRidFromDir(nextRoad.getId(), Cross.STRAIGHT);
                    if(dirRoadId!=-1){
                        tmpCar = roads.get(dirRoadId).getFirst(cross.getId());
                        if (tmpCar != null) {
                            //那一辆车即将到站
                            if(tmpCar.getTo()==cross.getId()){
                                break;
                            }
                            tmpDir = cross.getTurnDir(tmpCar.getCurRoadId(), tmpCar.getNextRoadId());
                            if (tmpDir == Cross.STRAIGHT){
                                break;// 冲突
                            }
                               
                        }
                    }
                    
                    // 左转冲突判断
                    dirRoadId = cross.getRidFromDir(nextRoad.getId(), Cross.LEFT);
                    if(dirRoadId!=-1){
                        tmpCar = roads.get(dirRoadId).getFirst(cross.getId());
                        if (tmpCar != null) {
                            //那一辆车即将到站
                            if(tmpCar.getTo()==cross.getId()){
                                break;// 冲突
                            }
                            tmpDir = cross.getTurnDir(tmpCar.getCurRoadId(), tmpCar.getNextRoadId());
                            if (tmpDir == Cross.LEFT){
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
            if(cnt==roadsList.size()){
                cross.isWait=false;
            }
        }
        
    }
    /* 车库中的车辆上路行驶 */
    public static void driveCarInGarage(int time){
        Car c;
        Road curRoad;
        Channel chan;
        for( Cross cross: crossList){
            LinkedList<Car> carlist = carInGarage.get(cross.getId());
            if(carlist==null) continue;
            while(!carlist.isEmpty()){
                c = carlist.peek();
                
                if(c.getPath().getStartTime()<=time){
                    curRoad = roads.get(c.getCurRoadId());
                    chan = curRoad.getIntoChannels(cross.getId());
                    
                    if(chan==null)
                        break;
                    chan.intoNewCar(c);
                    carlist.poll();
//                    System.out.println(Arrays.toString(chan));
//                    System.out.println(chan.channel);
//                    carInRoadCnt++;
                    carAllCnt++;
                    System.out.println("车"+c.getId()+"开始上路 总数："+carAllCnt+"  当前路上有："+(carAllCnt-carArriveCnt)+" 已到达"+carArriveCnt);
                }else{
                    break;
                }
            }
            
        }   
    }
    public static void initRead(String carPath, String roadPath, String crossPath, String answerPath) {
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
