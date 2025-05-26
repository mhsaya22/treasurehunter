package plugin.treasureHunter.command;

import java.util.List;
import java.util.SplittableRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;


public class SpawnBlockCommand extends BaseCommand implements Listener {

  public SpawnBlockCommand() {
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label, String[] args) {
    player.getWorld().getBlockAt(getOreLocation(player)).setType(getOre());
    return true;
  }

  @Override
  public boolean onExecuteNPCPlayerCommand(CommandSender sender,Command command, String label, String[] args) {
    return false;
  }

  private Location getOreLocation(Player player) {
    Location playerLocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(10) -5;
    int randomY = new SplittableRandom().nextInt(0) -2;
    int randomZ = new SplittableRandom().nextInt(10) -5;

    int blockX = playerLocation.getBlockX() + randomX;
    int blockY = playerLocation.getBlockY() + randomY;
    int blockZ = playerLocation.getBlockZ() + randomZ;

    return new Location(player.getWorld(), blockX , blockY ,blockZ );
  }

  /**
   * ランダムで鉱石を抽選して、その結果の鉱石を取得します。
   * @return 鉱石
   */
  private Material getOre() {
    List<Material> oreList = List.of(
        Material.IRON_ORE, Material.GOLD_ORE,
        Material.LAPIS_ORE, Material.REDSTONE_ORE,Material.DIAMOND_ORE,
        Material.EMERALD_ORE);
    return oreList.get(new SplittableRandom().nextInt(oreList.size()));
  }

}
