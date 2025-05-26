package plugin.treasureHunter.command;

import java.util.Map;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.treasureHunter.data.GameData;


public class BackCommand extends BaseCommand implements CommandExecutor {

  private final Map<String, GameData> playerGameDataMap;

  public BackCommand(Map<String, GameData> playerGameDataMap){
    this.playerGameDataMap = playerGameDataMap;
  }


  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label, String[] args) {
    backLocation(player);
    return true;
  }

  @Override
  public boolean onExecuteNPCPlayerCommand(CommandSender sender,Command command, String label, String[] args) {
    return false;
  }


  /**
   * ゲーム終了後、元の位置に戻ります。
   * @param player backコマンドを実行したプレイヤー
   */
  private void backLocation(Player player) {
    GameData data = playerGameDataMap.get(player.getName());
    if (Objects.isNull(data)){
      return;
    }
    Location location = data.getOriginalLocation();
    player.teleport(location);
    player.sendMessage("元の位置に戻りました!");
  }
}
