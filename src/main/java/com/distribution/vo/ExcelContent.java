package com.distribution.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjiabin
 */
@Data
public class ExcelContent {

    private List<String> section = new ArrayList<>();

    private List<String> rounding = new ArrayList<>();

    private List<String> rate = new ArrayList<>();

    private List<String> sum = new ArrayList<>();

    private List<String> sumRate = new ArrayList<>();


}
