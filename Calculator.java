package cn.edu.hit.gpcs.area.core;

import cn.edu.hit.gpcs.area.model.OperationRecord;
import cn.edu.hit.gpcs.area.model.OperationSettings;

/**
 * 面积计算抽象类
 */
public abstract class Calculator {
    /**
     * 计算面积，结果保存到record中
     * @param record 作业记录
     * @param settings 作业参数设定
     */
    public abstract void calc (OperationRecord record, OperationSettings settings);
}
