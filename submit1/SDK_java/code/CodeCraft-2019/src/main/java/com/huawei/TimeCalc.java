package com.huawei;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimeCalc {
    private List<Car> cars;
    public int time;
    public int tSum;
    public int tPri;
    public int tSumPri;
    public int priCnt=0;
    public int tE=0;
    public int tESum=0;
    TimeCalc(int time,List<Car> cars){
        this.time = time;
        this.cars = cars;
    }
    
    public void calc(){
        int minPriTime=99999;
        int maxPriTime=-1;
        int maxStartTime = -1;
        int minStartTime = 999999;
        int priMaxStartTime = -1;
        int priMinStartTime = 999999;
        tSum=0;
        tSumPri = 0;
        Set<Integer> allstart = new HashSet<>();
        Set<Integer> pristart = new HashSet<>();
        Set<Integer> allend = new HashSet<>();
        Set<Integer> priend = new HashSet<>();
        int maxSpeed=0;
        int priMaxSpeed=0;
        int minSpeed=99999;
        int priminSpeed=99999;
        
        for(Car c : cars){
            allstart.add(c.getFrom());
            allend.add(c.getTo());
            maxSpeed = Math.max(maxSpeed, c.getSpeed());
            minSpeed = Math.max(minSpeed, c.getSpeed());
            maxStartTime = Math.max(maxStartTime, c.getPlanTime());
            minStartTime = Math.min(minStartTime, c.getPlanTime());
            tSum+=(c.getArriveTime() - c.getPlanTime());
            if(c.isProiority()){
                pristart.add(c.getFrom());
                priend.add(c.getTo());
                priMaxSpeed = Math.max(priMaxSpeed, c.getSpeed());
                priminSpeed = Math.min(priminSpeed, c.getSpeed());
                priMaxStartTime = Math.max(priMaxStartTime, c.getPlanTime());
                priMinStartTime = Math.min(priMinStartTime, c.getPlanTime());
                tSumPri+= (c.getArriveTime() - c.getPlanTime());
                minPriTime = Math.min(minPriTime, c.getPlanTime());
                maxPriTime = Math.max(maxPriTime, c.getArriveTime());
                priCnt++;
            }
        }
        tPri = maxPriTime - minPriTime; 
        double a = (cars.size()*1.0/priCnt)*0.05+
                (maxSpeed*1.0/minSpeed)/(priMaxSpeed*1.0/priminSpeed)*0.2375+
                (maxStartTime*1.0/minStartTime)/(priMaxStartTime*1.0/priMinStartTime)*0.2375+
                allstart.size()*1.0/pristart.size()*0.2375+
                allend.size()*1.0/priend.size()*0.2375;
        double b = (cars.size()*1.0/priCnt)*0.8+
                (maxSpeed*1.0/minSpeed)/(priMaxSpeed*1.0/priminSpeed)*0.05+
                (maxStartTime*1.0/minStartTime)/(priMaxStartTime*1.0/priMinStartTime)*0.05+
                allstart.size()*1.0/pristart.size()*0.05+
                allend.size()*1.0/priend.size()*0.05;
        
       tE = (int)Math.round(a*tPri)+time;
       tESum = (int)Math.round(b*tSumPri)+tSum;
        
    }
    
    
    
}
