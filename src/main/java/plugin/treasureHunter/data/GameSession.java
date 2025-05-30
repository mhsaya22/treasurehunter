package plugin.treasureHunter.data;


import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * ゲーム中のセッションデータを保持するクラスです。
 * 各プレイヤーごとにゲームの進行状況(スコア、コンボ、元の位置、装備、残り時間)を管理します。
 */

@Getter
@Setter

public class GameSession {

  private Material lastBlockOre = Material.AIR;
  private int comboCount = 0;
  private ItemStack originalItem;
  private Location originalLocation;
  private final List<ReplacedBlocksData> replacedBlocksList = new ArrayList<>();
  private int gameTimer;

  public void startTimer(int timeLimit){
    this.gameTimer = timeLimit;
  }
  public void reduceTime(int seconds){
    this.gameTimer -= seconds;
  }
  public int getTimeLeft(){
    return this.gameTimer;
  }
  }
