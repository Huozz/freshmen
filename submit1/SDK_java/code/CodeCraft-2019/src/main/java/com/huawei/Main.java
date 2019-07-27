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
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class Main {

    public static List<Car> cars = new ArrayList<>();
    public static HashMap<Integer, Car> carMap = new HashMap<>();
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
    public static int time = 0;
    
    public static void main(String[] args) {

        if (args.length != 5) {
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
        String presetAnswerPath = args[3];
        String answerPath = args[4];
        logger.info("carPath = " + carPath + " roadPath = " + roadPath + " crossPath = " + crossPath
                + " and presetanswerPath = " + presetAnswerPath + " answerpath"+ answerPath);

        logger.info("start read input files");
        initRead(carPath, roadPath, crossPath, presetAnswerPath);
        init();
        initShortPath();
        start();

        logger.info("Start write output file");
         writeAnswer(answerPath);

        logger.info("End...");
    }

    public static void start() {
    	
        System.out.println(carMap.size());
        int lockCnt=0;
        for (time = 0; carArriveCnt != carMap.size(); time++) {
            
            for (Road r : roads.values()) {
                
            	driveAllCarJustOnRoadToEndState(r, true);
            }
            
            driveCarInGarage(true);
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
                    
                     cross.isWait = true;
                    driveAllWaitCar(cross);
                }

            }
            
            driveCarInGarage(false);
        }
        TimeCalc tc = new TimeCalc(time-1, cars);
        tc.calc();
       
        System.out.println(tc.tPri+"    "+tc.tSumPri);
        System.out.println(tc.tE+"   " + tc.tESum);
        
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
//        System.out.println(carInGarage.size());

    }

    public static void initShortPath() {
        for (Cross cross : crossList) {
            findShortBydijkstra(cross.getId());
        }
        // //outPut shortPath
//         for(Entry<String, LinkedList<Integer>> entry : shortPath.entrySet()){
//         System.out.println(entry.getKey());
//         System.out.println(entry.toString());
//         logger.info(entry.toString());
//         }
    }

    /**
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
                
                if (car == null) {
                    cnt++;
                    break;
                }
                cnt = 0;
                firstChannel = road.getFirstChannel(cross.getId());
                
                if (car.getTo() == cross.getId()) {
//                    car.setFlag(Car.ARRIVE);
//                    carArriveCnt++;
//                   
//                    road.moveOutRoad(cross.getId());
//                    firstChannel.driveCar(false);
//                    System.out.println("arrvie " + carArriveCnt);
//                    car.addPath(road.getId());
//                    continue;
                	if (!car.isProiority()) {
                        int nextRid = cross.getTidByByStraight(road.getId());
                        if (nextRid != -1) {
                            
                            if (proiorityCarIntoRoad(cross, road, nextRid, roadsList)) {
                                break;
                            }
                        }
                    }
                    car.setFlag(Car.ARRIVE);
                    car.setArriveTime(time);
                    carArriveCnt++;
                    car.addPath(road.getId());
                    
                    firstChannel.channel.poll();
                    firstChannel.driveCar(false);
//                    driveCarInitList(true);
                    Cross tracebackCross;
                	if(cross.getId()==road.getFrom()) {
                 	   tracebackCross=crosses.get(road.getTo());
                    }else { 
                 	   tracebackCross=crosses.get(road.getFrom());
                    }
                    drivePartCarInitList(tracebackCross,road);
                    System.out.println("arrvie " + carArriveCnt);
                    continue;
                }
                if (!car.isProiority()) {
                    
                    if (proiorityCarIntoRoad(cross, road, car.getNextRoadId(car.isPreset()), roadsList)) {
                        break;
                    }
                }
                if (waiting && cross.isWait) {
                    
                    Road newRoad = cross.getRelaxedChannel(road.getId());
                    Channel ch = newRoad.getIntoChannels(cross.getId());
                    if (ch != null) {
                        if (ch.moveInACar(firstChannel, car)) {
                        	firstChannel.channel.poll();
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
                            
                            if (car.getFlag() == Car.WAIT) {
                                break;
                            }
                            
                        }
                    } 
                    
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
                  
                    firstChannel.driveCar(false);
                    continue;   
                }

                nextRoad = roads.get(car.getNextRoadId(car.isPreset()));
                int dir = cross.getTurnDir(car.getCurRoadId(car.isPreset()), car.getNextRoadId(car.isPreset()));
                if (dir == Cross.STRAIGHT) {
                    
                    if (moveToNextRoad(cross, road, firstChannel, car, nextRoad)) {
                        continue;
                    } else {
                        break;
                    }
                } else if (dir == Cross.LEFT) {
                    if (turnDirConflict(cross, road, nextRoad, Cross.STRAIGHT, car.isProiority())) {
                        break;
                    }
                    
                    if (moveToNextRoad(cross, road, firstChannel, car, nextRoad)) {
                        continue;
                    } else {
                        break;
                    }
                } else {
                    
                    if (turnDirConflict(cross, road, nextRoad, Cross.STRAIGHT, car.isProiority())) {
                        break;
                    }
                    
                    if (turnDirConflict(cross, road, nextRoad, Cross.LEFT, car.isProiority())) {
                        break;
                    }
                   
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
    

    
    public static void driveCarInGarage(boolean priority) {
        Car c;
        Road curRoad;
        Channel chan;
        int sum;
        int has;
        for (Cross cross : crossList) {
            LinkedList<Car> carlist = carInGarage.get(cross.getId());
            if(cross.lockDelayTime-->0) continue;
            if(carlist.isEmpty()) continue;
            if (carlist == null)
                continue;
            for(int i=0;i<carlist.size();i++){
                c = carlist.get(i);
                if (priority && !c.isProiority()) {
                    break;
                }
                if (c.getPlanTime() <= time && (carAllCnt - carArriveCnt)<1000) {
                    curRoad = roads.get(c.getCurRoadId(c.isPreset()));
                    chan = curRoad.getIntoChannels(cross.getId());

                    if (chan == null)
                       continue;
                    if(!chan.channel.isEmpty()){
                        if(chan.channel.getLast().getFlag()==Car.WAIT)
                            continue;
                    }
                    sum =  curRoad.getMaxCarNum();
                    has = curRoad.getCurHaveCarNum(cross.getId());
                    if(has>sum*0.75){
                        break;
                    }
                    c.setPlanTime(time);
                    chan.intoNewCar(c);
                    carlist.remove(i);
                    i--;
                    carAllCnt++;
                    
                } else {
                    if(!c.isProiority())
                    	break;
                }
            }

        }
    }
    public static void drivePartCarInitList(Cross tracebackCross,Road clearRoad) {
        
    	Car c;
    	Channel chan;
    	Road curRoad;
    	int sum;
        int has;
    	LinkedList<Car> carlist = carInGarage.get(tracebackCross);
        if (carlist == null)
            return;
        if(carlist.isEmpty()) return;
        for(int i=0;i<carlist.size();i++){
            c = carlist.get(i);
            if (!c.isProiority()) {
                break;
            }
            if (c.getPlanTime() <= time && (carAllCnt - carArriveCnt)<3000) { { 
            	curRoad = roads.get(c.getCurRoadId(c.isPreset())); 
	            if(curRoad.getId()==clearRoad.getId()) {          
	                chan = curRoad.getIntoChannels(tracebackCross.getId());
	                if (chan == null)
	                    continue;
	                if(!chan.channel.isEmpty()){
	                    if(chan.channel.getLast().getFlag()==Car.WAIT)
	                        continue;
	                }
	                sum =  curRoad.getMaxCarNum();
                    has = curRoad.getCurHaveCarNum(tracebackCross.getId());
                    if(has>sum*0.75){
                        break;
                    }
                    c.setPlanTime(time);
	                chan.intoNewCar(c);
	                carlist.remove(i);
	                i--;
	                
            	}
	            else continue;
        }
    }
    }
    }
    
    public static boolean moveToNextRoad(Cross cross, Road road, Channel firstChannel, Car car, Road nextRoad) {
        
    	Cross tracebackCross;
    	if(cross.getId()==road.getFrom()) {
     	   tracebackCross=crosses.get(road.getTo());
        }else { 
     	   tracebackCross=crosses.get(road.getFrom());
        }
        Channel chan = nextRoad.getIntoChannels(cross.getId());
        if (chan == null) {
            
            car.setCurRoadDis(firstChannel.road.getLength());
            car.setFlag(Car.END);
            
            firstChannel.driveCar(false);
//            driveCarInitList(true);
           
           drivePartCarInitList(tracebackCross,road);
            return true;
        }
        if (chan.moveInACar(firstChannel, car)) {
        	car.addPath(firstChannel.road.getId());
        	firstChannel.channel.poll();
        } else {
            
            if (car.getFlag() == Car.WAIT) {
                return false;
            }
        }
        
        firstChannel.driveCar(false);
//      driveCarInitList(true);
        drivePartCarInitList(tracebackCross,road);
        return true;
    }
    
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
                if (tmpCar.getNextRoadId(tmpCar.isPreset()) == r.getId()) {
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
           
                    int nextRid = cross.getTidByByStraight(curRoad.getId());
                    return nextRid==nextRoad.getId();
                }
                tmpDir = cross.getTurnDir(tmpCar.getCurRoadId(tmpCar.isPreset()), tmpCar.getNextRoadId(tmpCar.isPreset()));
                if (tmpDir == dir) {
                    return true;
                }
            }
        }
        return false;
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
            if (r.getIsDuplex()) {
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
                        break;// 
                } else {
                    last = r.getTo();
                    r = roads.get(path.get(r.getTo()));
                    if (r == null)
                        break;// 
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
            if(r.getIsDuplex()){//
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
                
                System.exit(1);
            }
//            System.out.println(visit.size());
            if(visit.contains(end)){
                LinkedList<Integer> newPath = new LinkedList<>();
                Road r = roads.get(path.get(end));
                int last=end;
                while(last!=start){
                    newPath.addFirst(r.getId());
                    if(r.getTo()==last){
                        last = r.getFrom();
                        r = roads.get(path.get(r.getFrom()));
                        if(r==null)break;
                        
                    }else{
                        last = r.getTo();
                        r = roads.get(path.get(r.getTo()));
                        if(r==null)break;
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


    public static void initRead(String carPath, String roadPath, String crossPath, String presetAnswerPath) {
        File carInput = new File(carPath);
        Scanner sc;
        try {
            Pattern p = Pattern.compile("[-]{0,1}\\d+");
            sc = new Scanner(carInput);
            // sc.nextLine();
            
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
                cross.initRids();
                crosses.put(cross.getId(), cross);
                crossList.add(cross);
            }
            sc.close();
            
        
            sc = new Scanner(new File(presetAnswerPath));
            int carId =0;
            Car car=null;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.charAt(0) == '#')
                    continue;
                Matcher m = p.matcher(line);
                Answer path = new Answer();
                
                if (m.find()) {
                    carId = Integer.parseInt(m.group());
                    car = carMap.get(carId);
                }
                if (m.find()) {
                    car.setPlanTime(Integer.parseInt(m.group()));
                }
                LinkedList<Integer> list = new LinkedList<>();
                while (m.find()) {
                    list.add(Integer.parseInt(m.group()));
                }
                car.setRealPath(list);
            }
            sc.close();

        } catch (Exception e) {
            
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