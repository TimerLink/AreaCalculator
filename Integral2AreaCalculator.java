package cn.edu.hit.gpcs.area.core;

import cn.edu.hit.gpcs.area.model.LandBlock;
import cn.edu.hit.gpcs.area.model.OperationRecord;
import cn.edu.hit.gpcs.area.model.OperationSettings;
import cn.edu.hit.gpcs.area.model.Point;
import cn.edu.hit.gpcs.area.util.GeoUtils;
import cn.edu.hit.gpcs.area.util.RouteUtils;
import cn.edu.hit.gpcs.utils.DotEnv;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/4/14.
 */
public class Integral2AreaCalculator extends Calculator {
    private static int standardDepth;
    private static double workWid;
    private static int deviceNo;
    private static Date date;
    public void calc(OperationRecord record, OperationSettings settings) {
        double toArea = 0;
        double quaArea = 0;
        double toCount = 0;
        double toAveDepth = 0;
        standardDepth = settings.getAverageDepth();
        workWid = record.getWorkWidth() / 100.0;
        if (workWid <= 0)
            workWid = Double.parseDouble(DotEnv.get("SCAN_SQUARE_LENGTH"));
        deviceNo = record.getDeviceId();
        date = record.getDate();

        RouteUtils.RouteBreakJudge judge = new RouteUtils.RouteBreakJudge(settings.getMaxDistance());

        List<Point> points = record.getPoints(settings.getLowestDepth(), settings.getHighestDepth());
        List<List<Point>> routes = RouteUtils.sliceRoute(points, judge);
        if (!routes.isEmpty()) {
            double standLat = 0;
            double standLng = 0;
            loop:for (int i=0;i<routes.size();i++) {
                for (int j = 0; j < routes.get(i).size(); j++) {
                    if (routes.get(i).get(j).getLatitude() > 4 && routes.get(i).get(j).getLatitude() < 54&&routes.get(i).get(j).getLongitude() > 72 && routes.get(i).get(j).getLongitude() < 136) {
                        standLat = routes.get(i).get(j).getLatitude();
                        standLng = routes.get(i).get(j).getLongitude();
                        break loop;
                    }
                }
            }
            List<Double> perLat = new ArrayList<Double>();
            List<Double> perLog = new ArrayList<Double>();
            List<Double> totalArea = new ArrayList<Double>();
            List<Double> qualifyArea = new ArrayList<Double>();
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
                    double areaPart[] = areaC(qq, pointDepth);
                    totalArea.add(areaPart[0]);
                    qualifyArea.add(areaPart[1]);
                }
                qq.removeAll(qq);
                pointDepth.removeAll(pointDepth);
            }
            for (int i = 0; i < totalArea.size(); i++) {
                toArea = toArea + totalArea.get(i);
                quaArea = quaArea + qualifyArea.get(i);
            }
        }
        record.setIntegral2TotalArea((int) toArea);
        record.setIntegral2QualifiedArea((int) quaArea);
    }

    public static double[] areaC(List<String> dataList1,List<Integer> pointDepth) {
        String pp[];
        //用两个数组分别存储转换坐标后的横纵坐标
        double heng[] = new double[dataList1.size()];
        double zong[] = new double[dataList1.size()];
        int depth[] = new int[dataList1.size()];
        double temp[];
        pp = dataList1.get(0).split(",");
        temp = GeoUtils.transform(pp);
        heng[0] = temp[0];
        zong[0] = temp[1];
        for (int i = 1; i < dataList1.size(); i++) {
            pp = dataList1.get(i).split(",");
            temp = GeoUtils.transform( pp);
            heng[i] = temp[0];
            zong[i] = temp[1];
            depth[i] = pointDepth.get(i);
        }
        double countArea[] = countArea(heng,zong,depth);
        return countArea;
    }

    /*新函数,给出一组横纵坐标计算面积*/

    public static double[] countArea(double heng[],double zong[],int depth[]){
        double areaTo[] = {0,0};
        boolean judge;//判断是否是内部点
        boolean judgeLast = false;//判断上一个点是否是内部点
        for (int j=2;j<heng.length;j++) {
            judge = false;
            double areaPart[] = transformNew(heng[j-2], zong[j-2], heng[j-1], zong[j-1], heng[j], zong[j],depth[j]);
            double areaLast = areaPart[4];
            double areaQua = areaPart[0];
            for (int i = j-1; i > 0; i--) {
                areaPart = transformNew(heng[i-1], zong[i-1], heng[i], zong[i], heng[j], zong[j],depth[j]);
                if (areaPart[4]==0){
                    judge = true;
                    judgeLast = true;
                    break;
                }
            }
            if (!judge){
                if (!judgeLast) {//上一个点也是外部点
                    areaTo[0] = areaTo[0] + areaLast;
                    areaTo[1] = areaTo[1] + areaQua;
                }
                else {//上一个点是内部点
                    double areaIsolated[] = areaIsolated(j,heng,zong,depth[j]);
                    areaTo[0] = areaTo[0] + areaIsolated[0];
                    areaTo[1] = areaTo[1] + areaIsolated[1];
                }
                judgeLast = false;
            }
        }
        areaTo[0] = areaTo[0] + workWid*(Math.sqrt(Math.pow((heng[0] - heng[1]), 2) + Math.pow((zong[0] - zong[1]), 2)));
        return areaTo;
    }

    /*计算孤立点的面积*/
    public static double[] areaIsolated(int j,double heng[],double zong[],int depth){
        double distanceMin = Math.sqrt(Math.pow((heng[j] - heng[j-1]), 2) + Math.pow((zong[j] - zong[j-1]), 2));
        int numberMin = j-1;
        for (int i=j-2;i>=0;i--){
            double distance = Math.sqrt(Math.pow((heng[j] - heng[i]), 2) + Math.pow((zong[j] - zong[i]), 2));
            if (distanceMin>distance) {
                distanceMin = distance;
                numberMin = i;
            }
        }
        double area[] = new double[2];
        area[0] = workWid*(Math.sqrt(Math.pow((heng[j] - heng[numberMin]), 2) + Math.pow((zong[j] - zong[numberMin]), 2)));
        if (depth>standardDepth){
            area[1] = area[0];
        }
        else {
            area[1] = 0;
        }
        return area;
    }
    /**/
    public static double[] transformNew(double x1,double y1,double x2,double y2,double x,double y,int depth){
        //判断y1==y2否
        double newXY[] = new double[5];
        double dx = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        double dy = workWid / 2;
        double dxNew = Math.sqrt(Math.pow((x - x2), 2) + Math.pow((y - y2), 2));
        double x0 = x1;
        double y0 = y1;
        double sint = (y2-y1)/dx;
        double cost = (x2-x1)/dx;
        double xNew;
        double yNew;
        xNew = (y - y0) * sint + (x - x0) * cost;
        yNew = (y - y0) * cost - sint * (x - x0);
        if (xNew <= dx && xNew >= 0 && yNew >= -0.9*dy && yNew <= 0.9*dy) {
            newXY[0] = 0;
            newXY[1] = y1;
            newXY[2] = x2;
            newXY[3] = y2;
            newXY[4] = 0;//面积
        } else {
            if (depth>standardDepth) {
                newXY[0] = dxNew * workWid;
            }
            else {
                newXY[0] = 0;
            }
            newXY[1] = y2;
            newXY[2] = x;
            newXY[3] = y;
            newXY[4] = dxNew * workWid;//面积
        }
        return newXY;
    }
    /*
     * 经纬度一转四
     */
    public static void transformQua(double x,double y,LandBlock landEve){
        double x1 = x - workWid/200;
        double x3 = x - workWid/200;
        double x2 = x + workWid/200;
        double x4 = x + workWid/200;
        double y1 = y - workWid/200;
        double y3 = y - workWid/200;
        double y2 = y + workWid/200;
        double y4 = y + workWid/200;
        double LB1[] = GeoUtils.transformBack(x1,y1);
        double LB2[] = GeoUtils.transformBack(x2,y2);
        double LB3[] = GeoUtils.transformBack(x3,y3);
        double LB4[] = GeoUtils.transformBack(x4,y4);
        double Longi[] = new double[]{LB1[0],LB2[0],LB3[0],LB4[0]};
        double Latit[] = new double[]{LB1[1],LB2[1],LB3[1],LB4[1]};
        double east = 0;
        double west = 1000;
        double north = 0;
        double south = 1000;
        for (int i=0;i<4;i++){
            if (Longi[i]>east){
                east = Longi[i];
            }
            if (Longi[i]<west){
                west = Longi[i];
            }
        }
        for (int i=0;i<4;i++){
            if (Latit[i]>north){
                north = Latit[i];
            }
            if (Latit[i]<south){
                south = Latit[i];
            }
        }
        landEve.setBoundary(east,south,west,north);
    }
}
