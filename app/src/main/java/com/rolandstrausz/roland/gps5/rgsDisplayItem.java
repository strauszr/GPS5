package com.rolandstrausz.roland.gps5;

import android.media.Image;

/**
 * Created by Roland on 23.05.2015.
 */
public class rgsDisplayItem
{
    public String unit;
    public int iconId;
    public String initialString;
    public String inputType;
    public int id;
    public String text;
    public double timeStamp;

    public rgsDisplayItem(String inputIn, String initialIn, String unitIn,int icId)
    {
        initialString=initialIn;
        unit=unitIn;
        inputType=inputIn;
        text=initialIn;
        iconId=icId;
    }

    public String getunit(){
        return unit;
    }

    public String getInputtype(){
        return inputType;
    }

    public int getId(){
        return id;
    }

    public String getText(){
        return text;
    }

    public void setinitialString(String txt){
        initialString=txt;
    }

    public void setunit(String txt){
        unit=txt;
    }

    public void setInputtype (String txt){
        inputType=txt;
    }
    public void setId(int nr){
        id=nr;
    }
    public void setText(String txt){
        text=txt;
        timeStamp=System.currentTimeMillis();
    }




}