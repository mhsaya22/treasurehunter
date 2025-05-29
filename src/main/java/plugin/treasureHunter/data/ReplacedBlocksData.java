package plugin.treasureHunter.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 * ゲーム開始前の鉱石の情報を扱うオブジェクト。
 */

@Getter
@Setter
public class ReplacedBlocksData {
private Block block;
private BlockState originalState;

  public ReplacedBlocksData(Block block) {
    this.block = block;
    this.originalState = block.getState();
  }
}
