package cn.edu.hit.gpcs.area.core;

import cn.edu.hit.gpcs.area.model.LandBlock;
import cn.edu.hit.gpcs.area.model.OperationRecord;
import cn.edu.hit.gpcs.area.model.OperationSettings;
import cn.edu.hit.gpcs.area.model.Point;
import cn.edu.hit.gpcs.area.util.GeoUtils;
import cn.edu.hit.gpcs.area.util.RouteUtils;
import cn.edu.hit.gpcs.utils.DotEnv;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * 积分面积计算核心
 */
public class IntegralAreaCalculator extends Calculator {
    private static int standardDepth;
    private static double workWid;
//    private static int deviceNo;
//    private static Date date;

    @Override
    public void calc(OperationRecord record, OperationSettings settings) {
        double toArea = 0;
        double quaPercent = 0;
        double toCount = 0;
        double toAveDepth = 0;
        standardDepth = settings.getAverageDepth();
        workWid = record.getWorkWidth() / 100.0;
        if (workWid <= 0)
            workWid = Double.parseDouble(DotEnv.get("SCAN_SQUARE_LENGTH"));
        workWid = 1.5 * workWid;
//        deviceNo = record.getDeviceId();
//        date = record.getDate();

        RouteUtils.RouteBreakJudge judge = new RouteUtils.RouteBreakJudge(settings.getMaxDistance());

        List<Point> points = record.getPoints(settings.getLowestDepth(), settings.getHighestDepth());
        List<List<Point>> routes = RouteUtils.sliceRoute(points, judge);
        if (!routes.isEmpty()) {
            double standLat = 0;
            double standLng = 0;
            loop:for (int i=0;i<routes.size();i++) {
                for (int j = 0; j < routes.get(i).size(); j++) {
                    // 中国经纬度边界
                    if (routes.get(i).get(j).getLatitude() > 4 && routes.get(i).get(j).getLatitude() < 54 &&
                            routes.get(i).get(j).getLongitude() > 72 && routes.get(i).get(j).getLongitude() < 136) {
                        standLat = routes.get(i).get(j).getLatitude();
                        standLng = routes.get(i).get(j).getLongitude();
                        break loop;
                    }
                }
            }
            List<Double> perLat = new ArrayList<Double>();
            List<Double> perLog = new ArrayList<Double>();
            List<Double> totalArea = new ArrayList<Double>();
            List<Double> qualifiedPercent = new ArrayList<Double>();
            List<Double> totalAveDepth = new ArrayList<Double>();
            List<Double> totalCount = new ArrayList<Double>();
            List<String> qq = new ArrayList<String>();
            List<Integer> pointDepth = new ArrayList<Integer>();
            for (List<Point> route : routes) {
                List<Point> newRoute = RouteUtils.interpolate(route);//插点后路段
                for (int i = 0; i < newRoute.size(); i++) {
                    if (Math.abs(newRoute.get(i).getLatitude() - standLat) + Math.abs(newRoute.get(i).getLongitude() - standLng) < 5.0) {
                        perLat.add(newRoute.get(i).getLatitude());
                        perLog.add(newRoute.get(i).getLongitude());
                        pointDepth.add(newRoute.get(i).getDepth());
                    }
                }
                for (int j = 0; j < perLat.size(); j++) {
                    qq.add(Double.toString(perLat.get(j)) + "," + Double.toString(perLog.get(j)));
                }
                perLat.removeAll(perLat);
                perLog.removeAll(perLog);
                if (qq.size() > 3) {
                    double areaPart[] = areaC(qq, workWid, pointDepth);
                    totalArea.add(areaPart[0]);
                    qualifiedPercent.add(areaPart[1]);
                    totalAveDepth.add(areaPart[2]);
                    totalCount.add(areaPart[3]);
                }
                qq.removeAll(qq);
                pointDepth.removeAll(pointDepth);
            }
            for (int i = 0; i < totalArea.size(); i++) {
                toArea = toArea + totalArea.get(i);
                quaPercent = quaPercent + qualifiedPercent.get(i);
                toAveDepth = toAveDepth + totalAveDepth.get(i);
                toCount = toCount + totalCount.get(i);
            }
            toAveDepth = toAveDepth/toCount;
        }
        record.setIntegralTotalArea((int)Math.round(toArea));
        record.setIntegralQualifiedArea((int)Math.round(quaPercent));
        record.setAverageDepth((int) Math.round(toAveDepth));
    }

    public static double[] areaC(List<String> dataList1,double arc,List<Integer> pointDepth) {
        String pp[];
        //用两个数组分别存储转换坐标后的横纵坐标
        double heng[] = new double[dataList1.size()];
        double zong[] = new double[dataList1.size()];
        int depth[] = new int[dataList1.size()];
        int eastMost = 0;
        int westMost = 0;
        int northMost = 0;
        int southMost = 0;
        double temp[];
        pp = dataList1.get(0).split(",");
        temp = GeoUtils.transform(pp);
        heng[0] = temp[0];
        zong[0] = temp[1];
        //存进数组
        for (int i = 1; i < dataList1.size(); i++) {
            pp = dataList1.get(i).split(",");
            temp = GeoUtils.transform( pp);
            heng[i] = temp[0];
            zong[i] = temp[1];
            depth[i] = pointDepth.get(i);
            //寻找四个最远点
            eastMost = heng[i]>heng[eastMost]?i:eastMost;
            westMost = heng[i]<heng[westMost]?i:westMost;
            northMost = zong[i]>zong[northMost]?i:northMost;
            southMost = zong[i]<zong[southMost]?i:southMost;
        }
        double east = heng[eastMost];//最远坐标
        double west = heng[westMost];
        double north = zong[northMost];
        double south = zong[southMost];
        //确定小正方形个数
        List<Integer> cellDepth = numCount(east,west,south,north,arc,heng,zong,heng.length,depth);
        double area[] = new double[4];
        area[0] = arc*arc*cellDepth.size();///666.666667单位亩
        int qualifiedNum = 0;
        int totalDepth = 0;
        for (Integer cell:cellDepth){
            totalDepth = totalDepth+cell;
            if (cell>=standardDepth){
                qualifiedNum++;
            }
        }
        area[1] = arc*arc*qualifiedNum;
        area[2] = totalDepth;
        area[3] = cellDepth.size();
        return area;
    }

    /*
     * 由最点  遍历所有点 蒙特卡洛
     */
    public static List<Integer> numCount(double east,double west,double south,double north,double arc,double heng[],double zong[],int len,int depth[]) {
        int i = 0;
        int flag;
        double edgeX = west;
        double edgeY = north;//每个正方形的左上角的横纵坐标
        String pp[] = {"1000","1000"};//经纬度为1000,1000  出现过的点置为此点对应的坐标
        double zero[] = GeoUtils.transform(pp);
        int totalDepth;
        int depthCount;
//        DeviceInfo deviceInfo = new DeviceInfo(deviceNo).load();
        List<Integer> cellDepth = new ArrayList<>();
        while(edgeX<=east&&edgeX>=west&&edgeY<=north&&edgeY>=south)//按行扫描矩形区域所有单元格
        {
            flag = 0;
            totalDepth = 0;
            depthCount = 0;
            while(i<len){//扫描所有点，判断是否在小格中
                if(heng[i]==zero[0]&&zong[i]==zero[1])
                {i++;
                    continue;}//是清除点，跳过
                else
                if(isIn( edgeX, edgeY, heng[i], zong[i], arc)==1)//点在该小格中
                {
                    flag = 1;
                    heng[i] = zero[0];
                    zong[i] = zero[1];//标记清除
                    totalDepth = totalDepth+depth[i];
                    depthCount++;
                }
                i++;
            }
            i = 0;
            if(flag==1) {//单元格中有点
//                LandBlock sLand = new LandBlock(deviceNo, date);
                totalDepth = totalDepth/depthCount;
                cellDepth.add(totalDepth);
//                transformQua(edgeX,edgeY,sLand);
//                sLand.setCountyId(deviceInfo.getCountyId());
//                sLand.commit();
            }
            if(edgeX+arc<east) {
                edgeX = edgeX+arc;//在同一行下次扫面的小格左上角坐标
            }
            else {
                edgeX = west;//换行
                edgeY = edgeY-arc;
            }
        }
        return cellDepth;
    }
    public static int isIn(double edgeX,double edgeY,double pointX,double pointY,double arc)//左上角的顶点坐标 以及待判定的点
    {
        if(pointX<=(edgeX+arc)&&pointX>=edgeX&&pointY<=edgeY&&pointY>=edgeY-arc)
        {return 1;}
        else
            return 0;
    }

    /**
     * 导出文件
     * @param file
     * @param dataList
     * @return
     */
    public static boolean exportCsv(File file, List<String> dataList) {
        boolean isSucess = false;
        FileOutputStream out = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        try {
            out = new FileOutputStream(file);
            osw = new OutputStreamWriter(out);
            bw = new BufferedWriter(osw);
            if (dataList != null && !dataList.isEmpty()) {
                for (String data : dataList) {
                    bw.append(data).append("\r\n");
                }
            }
            isSucess = true;
        } catch (Exception e) {
            isSucess = false;
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                    bw = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (osw != null) {
                try {
                    osw.close();
                    osw = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return isSucess;
    }
    /**
     * 导入文件
     *
     * @param file csv文件(路径+文件)
     * @return
     */
    public static List<String> importCsv(File file) {
        List<String> dataList = new ArrayList<String>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = br.readLine()) != null) {
                dataList.add(line);
            }
        } catch (Exception e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                    br = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return dataList;
    }
}