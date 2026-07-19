package com.lonelyFishing;

import com.lonelyFishing.config.ConfigManager;
import com.lonelyFishing.config.CustomVariableManager;
import com.lonelyFishing.config.ItemGroupManager;
import com.lonelyFishing.config.LoreBonusManager;
import com.lonelyFishing.config.RodManager;
import com.lonelyFishing.fishing.FishingListener;
import com.lonelyFishing.fishing.RewardManager;
import com.lonelyFishing.hooks.EconomyHook;
import com.lonelyFishing.hooks.MythicMobsHook;
import com.lonelyFishing.hooks.NeigeItemsHook;
import com.lonelyFishing.hooks.PlaceholderAPIHook;
import com.lonelyFishing.hooks.PointsHook;
import com.lonelyFishing.items.ItemProvider;
import com.lonelyFishing.security.Sec;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class LonelyFishing extends JavaPlugin implements CommandExecutor, TabCompleter {

    private ConfigManager configManager;
    private ItemGroupManager groupManager;
    private RodManager rodManager;
    private ItemProvider itemProvider;
    private RewardManager rewardManager;
    private LoreBonusManager loreBonusManager;
    private CustomVariableManager customVariableManager;
    private FishingListener listener;
    // 软依赖 hook (全部反射对接, 缺失也不报错), 存为字段以便热重载时重新探测
    private MythicMobsHook mmHook;
    private NeigeItemsHook niHook;
    private EconomyHook ecoHook;
    private PointsHook ppHook;
    private PlaceholderAPIHook papiHook;

    @Override
    public void onEnable() {
        // 启动绿色提示 (作者名以加密形式存储, 运行时解密)
        printBanner(true);

        // 配置
        configManager = new ConfigManager(this);
        configManager.reload();

        // 软依赖 hook (全部反射对接, 缺失也不报错)
        mmHook = new MythicMobsHook(getLogger());
        niHook = new NeigeItemsHook(getLogger());
        ecoHook = new EconomyHook(getLogger());
        ppHook = new PointsHook(getLogger());
        papiHook = new PlaceholderAPIHook(getLogger());
        initHooks();

        itemProvider = new ItemProvider(mmHook, niHook);
        rewardManager = new RewardManager(ecoHook, ppHook);

        groupManager = new ItemGroupManager();
        groupManager.load(configManager.getItemGroups());
        rodManager = new RodManager(mmHook, niHook);
        rodManager.load(configManager.getRods());
        loreBonusManager = new LoreBonusManager();
        loreBonusManager.load(configManager.getGeneral());
        customVariableManager = new CustomVariableManager();
        customVariableManager.load(configManager.getGeneral());

        listener = new FishingListener(configManager, rodManager, groupManager, itemProvider, rewardManager, loreBonusManager, customVariableManager, papiHook);
        getServer().getPluginManager().registerEvents(listener, this);

        PluginCommand cmd = getCommand("lonelyfishing");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    @Override
    public void onDisable() {
        // 关闭绿色提示
        printBanner(false);
        if (listener != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                listener.clearSession(p.getUniqueId());
            }
        }
    }

    /** 控制台分行绿色输出启动/关闭横幅 */
    private void printBanner(boolean enable) {
        String green = ChatColor.GREEN.toString();
        String[] lines = new String[] {
            "插件作者：" + Sec.author(),
            "感谢您的支持！",
            "如有问题请及时反馈！",
            enable ? "插件已启动！" : "插件已关闭！"
        };
        for (String line : lines) {
            Bukkit.getConsoleSender().sendMessage(green + line);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("lonelyfishing.reload")) {
                sender.sendMessage(configManager.msg("reload-no-perm"));
                return true;
            }
            reloadAll();
            sender.sendMessage(configManager.msg("reload-success"));
            return true;
        }
        sender.sendMessage(configManager.msg("unknown-subcommand"));
        return true;
    }

    /** 发送多行帮助信息 (作者名以加密形式注入) */
    private void sendHelp(CommandSender sender) {
        for (String line : configManager.msgList("help", "author", Sec.author())) {
            sender.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            String[] opts = new String[]{"reload", "help"};
            for (String o : opts) {
                if (o.startsWith(args[0].toLowerCase())) out.add(o);
            }
        }
        return out;
    }

    public void reloadAll() {
        // 热重载: 重新探测软依赖 (支持启动后才安装的 mm/ni/vault/playerpoints/papi), 再重载配置与缓存
        initHooks();
        configManager.reload();
        groupManager.load(configManager.getItemGroups());
        rodManager.load(configManager.getRods());
        if (loreBonusManager != null) loreBonusManager.load(configManager.getGeneral());
        if (customVariableManager != null) customVariableManager.load(configManager.getGeneral());
    }

    /** 重新初始化全部软依赖 hook (idempotent, 缺失插件则置为不可用) */
    private void initHooks() {
        if (mmHook != null) mmHook.init();
        if (niHook != null) niHook.init();
        if (ecoHook != null) ecoHook.init();
        if (ppHook != null) ppHook.init();
        if (papiHook != null) papiHook.init();
    }
}
