package plugin.treasureHunter.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

/**
 * ゲーム中に置き換えられたブロックの元の情報を保持するデータクラスです。
 * 位置と状態を保存し、ゲーム終了後に復元されます。
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
