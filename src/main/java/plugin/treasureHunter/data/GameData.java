package plugin.treasureHunter.data;


import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * ゲームデータを保存するオブジェクト
 *
 */
@Getter
@Setter

public class GameData {

  private Material lastBlockOre = Material.AIR;
  private int comboCount = 0;
  private ItemStack originalItem;
  private Location originalLocation;
  private final List<ReplacedBlocksData> replacedBlocksList = new ArrayList<>();


}
