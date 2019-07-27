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
    
    public static List<Car> cars=new ArrayList<>();
    public static HashMap<Integer,Road> roads=new HashMap<>();
    public static HashMap<Integer,Cross> crosses=new HashMap<>();
    public static List<Answer> answers=new ArrayList<>();
    public static LinkedList<Car> carsCache=new LinkedList<>();
    private static final Logger logger = Logger.getLogger(Main.class);
    public static final int PRETIME = 10;
    public static final int PRENUM = 200;
    public static void main(String[] args)
    {
        if (args.length != 4) {
            logger.error("please input args: inputFilePath, resultFilePath");
            return;
        }

        logger.info("Start...");

        String carPath = args[0];
        String roadPath = args[1];
        String crossPath = args[2];
        String answerPath = args[3];
        logger.info("carPath = " + carPath + " roadPath = " + roadPath + " crossPath = " + crossPath + " and answerPath = " + answerPath);

        // TODO:read input files
        logger.info("start read input files");
        initRead(carPath,roadPath,crossPath);
        // TODO: calc
        Collections.sort(cars);
//        for(Car car : cars){
//            findShortBydijkstra(car);
//        }
        int k=-PRETIME;
        Car c;
        for(int i=0;i<cars.size();i++){
           if(i%PRENUM==0){
               k+=PRETIME;
           }
           c= cars.get(i);
           c.setPlanTime(c.getPlanTime()+k);
           findShortBydijkstra(c,false);
        }
        while(!carsCache.isEmpty()){
            c = carsCache.pop();
            c.setPlanTime(c.getPlanTime()+5);
            findShortBydijkstra(c,true);
        }
        
        // TODO: write answer.txt
        logger.info("Start write output file");
        writeAnswer(answerPath);
        
        
        logger.info("End...");
    }
    public static void findShortBydijkstra(Car car,boolean isRpeat){
        HashSet<Integer> visit = new HashSet<>();
        HashMap<Integer,Double> dist = new HashMap<>();
        HashMap<Integer,Integer> path = new HashMap<>();
        int start = car.getFrom();
        visit.add(start);
        Cross startCross = crosses.get(start);
        List<Integer> srids = startCross.getRids();
        int end = car.getTo();
        for(int i=0;i<srids.size();i++){
            Road r = roads.get(srids.get(i));
            if(r.getIsDuplex()){//双向道路的坑
                dist.put(r.getTo()==start?r.getFrom():r.getTo(), r.getWeigth(car.getSpeed(),car.getPlanTime(),start));
                path.put(r.getTo()==start?r.getFrom():r.getTo(), r.getId());
            }else{
                if(r.getTo()!=start){
                    dist.put(r.getTo(), r.getWeigth(car.getSpeed(),car.getPlanTime(),start));
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
                            if(r.getWeigth(car.getSpeed(),car.getPlanTime(),nextCid)+dist.get(nextCid)<twei){
                                twei = r.getWeigth(car.getSpeed(),car.getPlanTime(),nextCid)+dist.get(nextCid);
                                dist.put(to, twei);
                                path.put(to,r.getId());
                            }
                        }else{
                            dist.put(to, r.getWeigth(car.getSpeed(),car.getPlanTime(),nextCid)+dist.get(nextCid));
                            path.put(to,r.getId());
                        }
                        //path.put(r.getFrom(),r.getId());
                    }else {
                        to=r.getTo();
                        if(visit.contains(r.getTo())) continue;
                        Double twei = dist.get(r.getTo());
                        if(twei!=null){
                            if(r.getWeigth(car.getSpeed())+dist.get(nextCid)<twei){
                                twei = r.getWeigth(car.getSpeed(),car.getPlanTime(),nextCid)+dist.get(nextCid);
                                dist.put(r.getTo(), twei);
                                path.put(r.getTo(),r.getId());
                            }
                        }else{
                            dist.put(r.getTo(), r.getWeigth(car.getSpeed(),car.getPlanTime())+dist.get(nextCid));
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
                if(dist.get(end)>1024){
                    carsCache.push(car);;
                    return ;
                }
                System.out.println("已经找到最短路径"); 
                Answer ans = new Answer();
                ans.setCarId(car.getId());
                ans.setStartTime(car.getPlanTime());
                Road r = roads.get(path.get(end));
                int last=end;//双向道路的坑
                while(last!=start){
                    ans.addToHead(r.getId());
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
                
                answers.add(ans);
                setBlock(car,ans.getRoadIds(),start);
                System.out.println(ans.toString());
                break;
            }
        }
        
    }
    private static int findNextShort(HashMap<Integer,Double> dist,HashSet<Integer> visit){
        int cid=-1;
        double minWeight=Double.MAX_VALUE;
        for(Map.Entry<Integer, Double> entry: dist.entrySet()){
            if(!visit.contains(entry.getKey())){
                if(entry.getValue()<minWeight){
                    minWeight=entry.getValue();
                    cid = entry.getKey();
                }
            }
        }
        return cid;
        
    }
    public static void setBlock(Car car,LinkedList<Integer> path,int start){
  
        int totalTime=car.getPlanTime();
        for(Integer x:path){
            Road r = roads.get(x);
            totalTime+=car.getTime(r)+5;
            r.addBlocking(totalTime,start);
            start = (start == r.getFrom()?r.getTo():r.getFrom());
        }
//        System.out.println(totalTime-car.getPlanTime());
        
    }
    public static void initRead(String carPath,String roadPath,String crossPath){
        File carInput = new File(carPath);
        Scanner sc;
        try {
            Pattern p=Pattern.compile("[-]{0,1}\\d+"); 
            sc = new Scanner(carInput);
//            sc.nextLine();
            //读入 
            while(sc.hasNextLine()){
                String line = sc.nextLine();
                if(line.charAt(0)=='#') continue;
                Matcher m = p.matcher(line);
                Car car = new Car();
                if(m.find()){
                    car.setId(Integer.parseInt(m.group()));
                }
                if(m.find()){
                    car.setFrom(Integer.parseInt(m.group()));
                }
                if(m.find()){
                    car.setTo(Integer.parseInt(m.group()));
                    
                }
                if(m.find()){
                    car.setSpeed(Integer.parseInt(m.group()));
                    
                }
                if(m.find()){
                  car.setPlanTime(Integer.parseInt(m.group()));
                    
                }
                cars.add(car);
            }
            sc.close();
          //读入 road
            sc = new Scanner(new File(roadPath));
            while(sc.hasNextLine()){
                String line = sc.nextLine();
                if(line.charAt(0)=='#') continue;
                Matcher m = p.matcher(line);
                Road road = new Road();
                if(m.find()){
                    road.setId(Integer.parseInt(m.group()));
                }
                if(m.find()){
                    road.setLength(Integer.parseInt(m.group()));
                }
                if(m.find()){
                    
                    road.setSpeed(Integer.parseInt(m.group()));
                }
                if(m.find()){
                    road.setChannel(Integer.parseInt(m.group()));
                    
                }
                if(m.find()){
                  road.setFrom(Integer.parseInt(m.group()));
                    
                }
                if(m.find()){
                    road.setTo(Integer.parseInt(m.group()));
                      
                  }
                if(m.find()){
                    road.setIsDuplex(Integer.parseInt(m.group())==1);
                  }
                roads.put(road.getId(),road);
            }
            sc.close();
            //读入 cross
            sc = new Scanner(new File(crossPath));
//            System.out.println(crossPath);
            while(sc.hasNextLine()){
                String line = sc.nextLine();
                if(line.charAt(0)=='#') continue;
                Matcher m = p.matcher(line);
                Cross cross = new Cross();
                if(m.find()){
                    cross.setId(Integer.parseInt(m.group()));
                }
                if(m.find()){
                    
                    cross.setNorth(Integer.parseInt(m.group()));
                }
                if(m.find()){
                    cross.setEast(Integer.parseInt(m.group()));
                }
                if(m.find()){
                    cross.setSouth(Integer.parseInt(m.group()));
                }
                if(m.find()){
                    cross.setWest(Integer.parseInt(m.group()));
                    
                }
                cross.initRids();//把路口信息放链表 方便遍历
                crosses.put(cross.getId(),cross);
            }
            sc.close();
        } catch (Exception e) {
            System.out.println("读入失败");
            e.printStackTrace();
            System.exit(1);
           
        }
        
    }
    
    public static void writeAnswer(String answerPath){
        System.out.println("begin to write:"+answerPath);
        System.out.println(answers.size());
        try {
            PrintWriter write = new PrintWriter(new File(answerPath));
            for(Answer ans : answers){
                write.println(ans.toString());
            }
            write.flush();
            write.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
}