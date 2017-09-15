package com.gaozengzhi.flower;


public class MyHome {

    //温度
    private int temperature = 0;

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }


    //湿度
    private int humidity = 0;

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }


    //浇花状态
    private boolean flowerStatus = false;

    public boolean getFlowerStatus() {
        return flowerStatus;
    }

    public void setFlowerStatus(boolean flowerStatus) {
        this.flowerStatus = flowerStatus;
    }

}



