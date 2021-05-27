package com.distribution.util;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author QingKe
 * @date 2021-03-10 09:38
 **/
@Data
@Accessors(chain = true)
public class ArrayCalculation {

    private BigDecimal length;
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal sum;
    private BigDecimal average;
    private BigDecimal median;
    private BigDecimal variance;
    private BigDecimal standardDeviation;

    public ArrayCalculation(List<BigDecimal> sources) {
        List<BigDecimal> list = sources.stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (list.size() == 0) {
            return;
        }

        this.length = new BigDecimal(list.size());

        BigDecimal min = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;

        boolean isFirst = true;
        for (BigDecimal val : list) {
            if (isFirst) {
                max = val;
                min = val;
            } else {
                max = max.compareTo(val) > 0 ? max : val;
                min = min.compareTo(val) < 0 ? min : val;
            }
            sum = sum.add(val);
            isFirst = false;
        }

        this.min = min;
        this.max = max;
        this.sum = sum;

        this.average = sum.divide(this.length, 2, RoundingMode.HALF_UP);

        this.median =
                list.size() == 1 ? list.get(0) :
                        (list.size() % 2 == 0 ?
                                (list.get(list.size() / 2 - 1).add((list.get(list.size() / 2)))).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP)
                                : list.get((list.size() - 1) / 2)
                        );

        BigDecimal x = BigDecimal.ZERO;
        for (BigDecimal val : list) {
            x = x.add(val.subtract(this.average).multiply(val.subtract(this.average)));
        }

        this.variance = x.divide(this.length, 2, RoundingMode.HALF_UP);
        this.standardDeviation = BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(2, RoundingMode.HALF_UP);
    }

    public static ArrayCalculation build(List<Long> sources) {
        return new ArrayCalculation(sources.stream().map(BigDecimal::valueOf).collect(Collectors.toList()));
    }

}
