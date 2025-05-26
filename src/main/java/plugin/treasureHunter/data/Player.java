package plugin.treasureHunter.data;

import lombok.Getter;
import lombok.Setter;

/**
 * TreasureHunterのゲームを実行する際のプレイヤー情報を扱うオブジェクト。
 *
 */

@Getter
@Setter

public class Player {
  private String playerName;
  private int score;

  public Player(String playerName) {
    this.playerName = playerName;
  }
}
