package com.lonelyFishing.config;

/**
 * 自定义变量配置: 通过 PAPI 占位符读取当前值, 通过指令增加。
 * 用于 "钓到鱼时增加某变量" 的奖励机制 (如等级、声望等)。
 *
 * 配置示例 (config.yml):
 *   custom-variables:
 *     ll_level:
 *       placeholder: "%ll_level-默认%"                    # 读取当前值的 PAPI 占位符
 *       add-command: "ll add %player_name% {amount} 默认"  # 增加变量的指令模板
 *       message: "&a+{amount} 等级 &7(当前: &e{current}&7)" # 增加后发送给玩家的提示
 */
public class CustomVariable {

    private final String name;            // 变量名 (config.yml 中的 key, 用于物品组/鱼竿引用)
    private final String placeholder;     // 读取当前值的 PAPI 占位符 (如 %ll_level-默认%)
    private final String addCommand;      // 增加变量的指令模板 (含 {amount} 和 %player_name%)
    private final String message;         // 增加后发送给玩家的提示 (含 {amount}/{base}/{multiplier}/{current}/{player})

    public CustomVariable(String name, String placeholder, String addCommand, String message) {
        this.name = name;
        this.placeholder = placeholder == null ? "" : placeholder;
        this.addCommand = addCommand == null ? "" : addCommand;
        this.message = message == null ? "" : message;
    }

    public String getName() { return name; }
    public String getPlaceholder() { return placeholder; }
    public String getAddCommand() { return addCommand; }
    public String getMessage() { return message; }
}
