package plugin.treasureHunter.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * ゲーム開始前の鉱石の情報を扱うオブジェクト。
 */

@Getter
@Setter

public class ReplacedBlocksData {
private Block block;
private Material originalMaterial;

  public ReplacedBlocksData(Block block, Material originalMaterial) {
    this.block = block;
    this.originalMaterial = originalMaterial;
  }
}
