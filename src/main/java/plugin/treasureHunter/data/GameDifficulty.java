package plugin.treasureHunter.data;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

/**
 * 難易度設定を管理するデータクラスです。
 */
@Setter
@Getter

public class GameDifficulty {

  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";

  private List<Material> oreList;
  private String name;
  private int timeLimit;
  private double spawnChance;

  public GameDifficulty(String name, int timeLimit ,List<Material> oreList, double spawnChance){
    this.name = name;
    this.timeLimit = timeLimit;
    this.oreList = oreList;
    this.spawnChance = spawnChance;
  }

  public static GameDifficulty fromDifficultyString(String difficultyName){
    return switch (difficultyName) {

      case NORMAL -> new GameDifficulty(NORMAL, 80,
          List.of(Material.IRON_ORE, Material.GOLD_ORE,Material.LAPIS_ORE, Material.REDSTONE_ORE),0.3);

      case HARD -> new GameDifficulty(HARD, 60,
          List.of(Material.IRON_ORE, Material.GOLD_ORE,Material.LAPIS_ORE, Material.REDSTONE_ORE,Material.DIAMOND_ORE,
              Material.EMERALD_ORE),0.15);

      default -> new GameDifficulty(EASY, 100,List.of(Material.IRON_ORE, Material.GOLD_ORE),0.5);
    };
  }

}
