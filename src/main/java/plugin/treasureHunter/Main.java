package plugin.treasureHunter;


import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.treasureHunter.command.BackCommand;
import plugin.treasureHunter.command.TreasureHunterCommand;


public final class Main extends JavaPlugin {

  @Override
  public void onEnable() {
    TreasureHunterCommand treasureHunterCommand = new TreasureHunterCommand(this);
    Bukkit.getPluginManager().registerEvents(treasureHunterCommand, this);
    getCommand("treasureHunter").setExecutor(treasureHunterCommand);

    BackCommand backCommand = new BackCommand(treasureHunterCommand.getPlayerGameDataMap());
    getCommand("back").setExecutor(backCommand);

  }

}
