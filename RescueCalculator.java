package cn.edu.hit.gpcs.area.core;

import cn.edu.hit.gpcs.area.model.OperationRecord;
import cn.edu.hit.gpcs.area.model.OperationSettings;
import cn.edu.hit.gpcs.area.model.Point;
import cn.edu.hit.gpcs.area.util.GeoUtils;
import cn.edu.hit.gpcs.utils.DotEnv;

import java.util.List;

/**
 * 修复等效幅宽计算面积（给定幅宽计算总路径*幅宽=面积）
 */
public class RescueCalculator extends Calculator {
    private int workWidth;

    /**
     * @param width 工作幅宽
     */
    public void setWorkWidth(int width) {
        workWidth = width;
    }

    @Override
    public void calc(OperationRecord record, OperationSettings settings) {
        List<Point> points = record.getPoints();
        double totalArea = 0;
        Point lastPoint = null;
        for (Point point : points) {
            if (lastPoint != null) {
                double distance = 0;
                long deltaTime = Math.abs(point.getGpsTime().getTime() - lastPoint.getGpsTime().getTime()) / 1000;
                // 未定位点按照每秒${MAKE_UP_DISTANCE}的距离进行补偿
                if (point.getLatitude() == 0 || point.getLongitude() == 0) {
                    if (deltaTime <= settings.getMaxTimeout()) {
                        distance = Float.parseFloat(DotEnv.get("MAKE_UP_DISTANCE")) * deltaTime;
                    }
                } else {
                    distance = GeoUtils.distance(lastPoint, point);
                    if (distance > settings.getMaxDistance() || deltaTime > settings.getMaxTimeout()) {
                        distance = 0;
                    }
                }
                totalArea += distance * workWidth;
            }
            lastPoint = point;
        }
        int result = (int) Math.round(totalArea / 100);
        // 将结果保存到record中
        record.setWorkWidthQualifiedArea(result);
        record.setWorkWidthTotalArea(result);
        record.setFinalQualifiedArea(result);
        record.setFinalTotalArea(result);
    }
}
