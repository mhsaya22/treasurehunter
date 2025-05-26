package plugin.treasureHunter.data;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Setter
@Getter

public class GameDifficulty {


  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";

  private List<Material> oreList;

  private String name;
  private int timeLimit;

  public GameDifficulty(String name, int timeLimit ,List<Material> oreList){
    this.name = name;
    this.timeLimit = timeLimit;
    this.oreList = oreList;
  }

  public static GameDifficulty fromDifficultyString(String difficultyName){
    return switch (difficultyName) {
      case EASY -> new GameDifficulty(EASY, 180,List.of(Material.IRON_ORE, Material.GOLD_ORE));

      case NORMAL -> new GameDifficulty(NORMAL, 120,
          List.of(Material.IRON_ORE, Material.GOLD_ORE,Material.LAPIS_ORE, Material.REDSTONE_ORE));

      case HARD -> new GameDifficulty(HARD, 60,
          List.of(Material.IRON_ORE, Material.GOLD_ORE,Material.LAPIS_ORE, Material.REDSTONE_ORE,Material.DIAMOND_ORE,
              Material.EMERALD_ORE));

      default -> new GameDifficulty(EASY, 180,List.of(Material.IRON_ORE, Material.GOLD_ORE));
    };
  }

}
