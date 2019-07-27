package com.huawei;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class Main {
    public static List<Car> cars = new ArrayList<>();
    public static HashMap<Integer,Car> carMap = new HashMap<>();
    public static HashMap<Integer, Road> roads = new HashMap<>();
    public static HashMap<Integer, Cross> crosses = new HashMap<>();
    private static final Logger logger = Logger.getLogger(Main.class);
    public static HashMap<Integer, LinkedList<Car>> carInGarage = new HashMap<>();
    public static List<Cross> crossList = new ArrayList<>();
    public static void main(String[] args)
    {
        if (args.length != 5) {
            logger.error("please input args: inputFilePath, resultFilePath");
            return;
        }

        logger.info("Start...");

        String carPath = args[0];
        String roadPath = args[1];
        String crossPath = args[2];
        String presetAnswerPath = args[3];
        String answerPath = args[4];
        logger.info("carPath = " + carPath + " roadPath = " + roadPath + " crossPath = " + crossPath + " presetAnswerPath = " + presetAnswerPath + " and answerPath = " + answerPath);

        // TODO:read input files
        logger.info("start read input files");
//        initRead(carPath, roadPath, crossPath,presetAnswerPath);
        // TODO: calc

        // TODO: write answer.txt
        logger.info("Start write output file");

        logger.info("End...");
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
                if(!car.isPreset()){
                    write.println(car.toString());
                }
                
            }
            write.flush();
            write.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

 
    
    
}