package cn.edu.hit.gpcs.area.core;

import cn.edu.hit.gpcs.area.model.OperationRecord;
import cn.edu.hit.gpcs.area.model.OperationSettings;
import cn.edu.hit.gpcs.area.model.Point;
import cn.edu.hit.gpcs.area.util.GeoUtils;
import cn.edu.hit.gpcs.utils.DotEnv;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * 轨迹等效面积计算核心
 */
public class WorkWidthAreaCalculator extends Calculator {
    public void calc (OperationRecord record, final OperationSettings settings) {
        // 筛选出深度在合理范围内且有幅宽的点集
        List<Point> points = Lists.newArrayList(
                Collections2.filter(record.getPoints(), new Predicate<Point>() {
                    @Override
                    public boolean apply(Point p) {
                        return p.getDepth() >= settings.getLowestDepth() &&
                                p.getDepth() <= settings.getHighestDepth() &&
                                p.getWorkWidth() > 0;
                    }
                })
        );
        double qualifiedArea = 0;
        double unqualifiedArea = 0;
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
                if (point.getDepth() >= settings.getAverageDepth()) {
                    qualifiedArea += distance * point.getWorkWidth();
                } else {
                    unqualifiedArea += distance * point.getWorkWidth();
                }
            }
            lastPoint = point;
        }
        // 将结果保存到record中
        record.setWorkWidthQualifiedArea((int) Math.round(qualifiedArea / 100));
        record.setWorkWidthTotalArea((int) Math.round((qualifiedArea + unqualifiedArea) / 100));
    }

}
