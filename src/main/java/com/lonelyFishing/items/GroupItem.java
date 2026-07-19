package com.lonelyFishing.items;

import com.lonelyFishing.util.WeightedSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 物品组中的一个物品配置项
 */
public class GroupItem implements WeightedSelector.Weighted {

    private final String id;
    private final String source;        // mm / ni
    private final String itemId;        // 物品库节点名
    private final double weight;        // 权重
    private final Double money;         // 金币 (留空为 null)
    private final Integer points;       // 点券 (留空为 null)
    private final Integer exp;          // 经验 (留空为 null)
    private final List<String> commands;
    private final Map<String, Double> variables;  // 钓到时增加的自定义变量基础值 (变量名 -> 基础值)

    public GroupItem(String id, String source, String itemId, double weight,
                     Double money, Integer points, Integer exp, List<String> commands,
                     Map<String, Double> variables) {
        this.id = id;
        this.source = source;
        this.itemId = itemId;
        this.weight = weight;
        this.money = money;
        this.points = points;
        this.exp = exp;
        this.commands = commands == null ? new ArrayList<String>() : commands;
        this.variables = variables == null ? Collections.<String, Double>emptyMap() : variables;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    public String getId() { return id; }
    public String getSource() { return source; }
    public String getItemId() { return itemId; }
    public Double getMoney() { return money; }
    public Integer getPoints() { return points; }
    public Integer getExp() { return exp; }
    public List<String> getCommands() { return commands; }
    public Map<String, Double> getVariables() { return variables; }
}
