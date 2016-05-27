package cn.edu.hit.gpcs.area.core;

import cn.edu.hit.gpcs.area.model.OperationRecord;
import cn.edu.hit.gpcs.area.model.OperationSettings;
import cn.edu.hit.gpcs.utils.DotEnv;

/**
 * 最终面积核算核心
 */
public class FinalAreaCalculator extends Calculator {
    public final static int MAX_WORKWIDTH_INTEGRAL_AREA_RATIO = Integer.parseInt(DotEnv.get("MAX_WORKWIDTH_INTEGRAL_AREA_RATIO"));

    @Override
    public void calc(OperationRecord record, OperationSettings settings) {
        int finalTotalArea;
        float qualifiedRate = 0;
        if (record.getIntegralTotalArea() > 0 && record.getWorkWidthTotalArea() / record.getIntegralTotalArea() <
                MAX_WORKWIDTH_INTEGRAL_AREA_RATIO) {
            qualifiedRate = (float) record.getIntegralQualifiedArea() / record.getIntegralTotalArea();
        } else if (record.getWorkWidthTotalArea() > 0) {
            qualifiedRate = (float) record.getWorkWidthQualifiedArea() / record.getWorkWidthTotalArea();
        }
        /**
         * 最终面积取轨迹等效面积和积分算法面积中较小的一个
         * 除非其中一个为0，或者两者之比大于等于MAX_WORKWIDTH_INTEGRAL_AREA_RATIO
         */
        int minTotalArea = Math.min(record.getWorkWidthTotalArea(), record.getIntegral2TotalArea());
        int maxTotalArea = Math.max(record.getWorkWidthTotalArea(), record.getIntegral2TotalArea());
        if (minTotalArea == 0 || maxTotalArea / minTotalArea >= MAX_WORKWIDTH_INTEGRAL_AREA_RATIO) {
            finalTotalArea = maxTotalArea;
        } else {
            finalTotalArea = minTotalArea;
        }
        // 面积乘上补偿系数
        finalTotalArea = finalTotalArea * settings.getAreaMultiple() / 100;
        // 最终总面积 = min(第二个积分算法总面积, 轨迹幅宽面积)
        record.setFinalTotalArea(finalTotalArea);
        // 最终合格面积 = 前两个算法综合出的合格率 * 最终总面积
        record.setFinalQualifiedArea(Math.round(finalTotalArea * qualifiedRate));
    }
}
