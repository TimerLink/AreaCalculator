package cn.edu.hit.gpcs.area.core;

import cn.edu.hit.gpcs.area.model.OperationRecord;
import cn.edu.hit.gpcs.area.model.OperationSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * 面积计算组，用于运行一组计算逻辑
 */
public class CalculatorGroup extends Calculator {
    private List<Calculator> calculators;

    public CalculatorGroup() {
        calculators = new ArrayList<Calculator>();
    }

    public CalculatorGroup(Calculator calculator) {
        this();
        calculators.add(calculator);
    }

    public CalculatorGroup(Calculator calculator1, Calculator calculator2) {
        this(calculator1);
        calculators.add(calculator2);
    }

    public CalculatorGroup(Calculator calculator1, Calculator calculator2, Calculator calculator3) {
        this(calculator1, calculator2);
        calculators.add(calculator3);
    }

    public CalculatorGroup(Calculator calculator1, Calculator calculator2, Calculator calculator3, Calculator calculator4) {
        this(calculator1, calculator2, calculator3);
        calculators.add(calculator4);
    }

    public CalculatorGroup(List<Calculator> calculatorList) {
        this();
        calculators.addAll(calculatorList);
    }

    @Override
    public void calc(OperationRecord record, OperationSettings settings) {
        for (Calculator calculator : calculators) {
            calculator.calc(record, settings);
        }
    }
}
