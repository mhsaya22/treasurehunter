package plugin.treasureHunter.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * コマンドを実行して動かすプラグインの基底クラスです。
 */

public abstract class BaseCommand implements CommandExecutor {


  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player){
      return onExecutePlayerCommand(player,command,label,args);
    }else {
      return onExecuteNPCPlayerCommand(sender,command,label,args);
    }
  }

  /**
   * コマンド実行者がプレイヤーだった場合に実行します。
   * @param player　コマンドを実行したプレイヤー
   * @return 処理の実行有無
   */
  public abstract boolean onExecutePlayerCommand(Player player,Command command, String label, String[] args);

  /**
   * コマンド実行者がプレイヤー以外だった場合に実行します
   * @param sender　コマンド実行者
   * @return 処理の実行有無
   */
  public abstract boolean onExecuteNPCPlayerCommand(CommandSender sender,Command command, String label, String[] args);
}
