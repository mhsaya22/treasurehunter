package plugin.treasureHunter.command;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import lombok.Getter;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import plugin.treasureHunter.Main;
import plugin.treasureHunter.data.GameData;
import plugin.treasureHunter.data.GameDifficulty;
import plugin.treasureHunter.data.Player;
import plugin.treasureHunter.data.ReplacedBlocksData;
import plugin.treasureHunter.data.mapper.PlayerScoreMapper;
import plugin.treasureHunter.data.mapper.data.PlayerScore;

/**
 * 制限時間内にランダムで出現する鉱石ブロックを破壊して、スコア情報を獲得するゲームを起動するコマンドです。スコアは鉱石の種類によって変わり、破壊した鉱石の数によってスコアが変動します。
 * 結果はプレイヤー名、点数、日時などで保存されます。
 */

public class TreasureHunterCommand extends BaseCommand implements Listener {


  private final Main main;
  private final List<Player> playerList = new ArrayList<>();
  private final List<ReplacedBlocksData> replacedBlocksList = new ArrayList<>();
  private static final String LIST = "list";
  private SqlSessionFactory sqlSessionFactory;

  public TreasureHunterCommand(Main main) {
    this.main = main;

    try {
      InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
      this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Getter
  private Map<String, GameData>  playerGameDataMap = new HashMap<>();

  @Override
  public boolean onExecutePlayerCommand(org.bukkit.entity.Player player, Command command, String label, String[] args) {

    if (args.length == 1 && LIST.equals(args[0])){
      try (SqlSession session = sqlSessionFactory.openSession()) {

        PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
        List<PlayerScore> playerScoreList = mapper.selectList();

        for (PlayerScore playerScore : playerScoreList){

              player.sendMessage(playerScore.getId() + " | "
              + playerScore.getPlayerName() + " | "
              + playerScore.getScore() + " | "
              + playerScore.getDifficulty() + " | "
              + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
      }

     return false;
    }

   if (args.length != 1){
     player.sendMessage(ChatColor.RED + "実行できません。難易度の指定をしてください。[ easy / normal / hard ]");
     return false;
   }

    initPlayerStatus(player);

    gamePlay(player, getPlayerScore(player), GameDifficulty.fromDifficultyString(args[0]));
    return true;
  }



  @Override
  public boolean onExecuteNPCPlayerCommand(CommandSender sender,Command command, String label, String[] args) {
    return false;
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e){
    //ブロックを壊したプレイヤー
    org.bukkit.entity.Player player = e.getPlayer();
    Block block = e.getBlock();


    if (Objects.isNull(player) || replacedBlocksList.stream()
        .noneMatch(data -> data.getBlock().equals(block))) {
      return;
    }

    playerList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {int point = switch (block.getType()) {
          case IRON_ORE -> 5;
          case GOLD_ORE -> 10;
          case LAPIS_ORE, REDSTONE_ORE -> 20;
          case DIAMOND_ORE -> 30;
          case EMERALD_ORE -> 50;
          default -> 0;
        };

          GameData data = getGameData(player, block);

          p.setScore(p.getScore() + point + (data.getComboCount() -1)*2);
          if (data.getComboCount() > 1){
            player.sendMessage(ChatColor.YELLOW + "コンボ! × " + data.getComboCount());
          }

          player.sendMessage("鉱石を獲得！現在のスコアは" + p.getScore() + "点です！");
        });

  }


  /**
   * 現在実行しているプレイヤーのスコア情報を取得する
   * @param player コマンドを実行したプレイヤー
   * @return 現在実行しているプレイヤーのスコア情報
   */
  private Player getPlayerScore(org.bukkit.entity.Player player) {
    Player playerScore = new Player(player.getName());
    if (playerList.isEmpty()) {
     playerScore = addNewPlayer(player);
    } else {
      playerScore = playerList.stream().findFirst().map(ps
          -> ps.getPlayerName().equals(player.getName())
          ? ps
          : addNewPlayer(player)).orElse(playerScore);
    }
    playerScore.setScore(0);

    return playerScore;
  }

  /**
   * 新規のプレイヤー情報をリストに追加します。
   * @param player コマンドを実行したプレイヤー
   * @return 新規プレイヤー
   */
  private Player addNewPlayer(org.bukkit.entity.Player player) {
    Player newPlayer = new Player(player.getName());
    playerList.add(newPlayer);
    return newPlayer;
  }

  /**
   * ゲームを始める前にプレイヤーの状態を設定する。体力と空腹値を最大化して、装備は鉄のツルハシになる。
   *
   * @param player コマンドを実行したプレイヤー
   */
  private void initPlayerStatus(org.bukkit.entity.Player player) {
    player.setHealth(20);
    player.setFoodLevel(20);

    player.teleport(new Location(player.getWorld(), -259,11,-190));

    ItemStack originalItem = player.getInventory().getItemInMainHand();

    GameData data = new GameData();
    data.setOriginalLocation(player.getLocation());

    data.setOriginalItem(originalItem.getType() == Material.AIR
        ? new ItemStack(Material.AIR)
        : originalItem.clone());

    playerGameDataMap.put(player.getName(),data);

    player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));
  }

  /**
   * ゲームを実行します。
   * 制限時間内に鉱石を破壊するとスコアが加算されます。時間経過後に合計スコアを表示します。
   * @param player コマンドを実行したプレイヤー
   * @param nowPlayer プレイヤースコア情報
   */
  private void gamePlay(org.bukkit.entity.Player player, Player nowPlayer,GameDifficulty difficulty) {
    replacedBlocksList.clear();
    int[] gameTimer = {difficulty.getTimeLimit()};
    Bukkit.getScheduler().runTaskTimer(main,Runnable -> {
      gameTimer[0] -= 5 ;
      //Runnable.cancelの判断ロジック
      if (gameTimer[0] <= 0){
        Runnable.cancel();

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard emptyBord = manager.getNewScoreboard();
        player.setScoreboard(emptyBord);

        player.sendTitle("ゲームが終了しました！" , nowPlayer.getPlayerName() + " 合計は " + nowPlayer.getScore() + " 点！" ,
            0 , 60 , 0);


        replacedBlocksList.forEach(data ->
            data.getBlock().setType(data.getOriginalMaterial()));
        replacedBlocksList.clear();

        //スコア登録情報
        try (SqlSession session = sqlSessionFactory.openSession(true)){
          PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);

          mapper.insert(
              new PlayerScore(nowPlayer.getPlayerName()
                  ,nowPlayer.getScore()
                  ,difficulty.getName()));
        }

        player.sendMessage("元の位置に戻りますか？　戻る場合は　/back と入力してください ");
        GameData data = playerGameDataMap.get(player.getName());

        if (Objects.isNull(data)){
          player.getInventory().setItemInMainHand(data.getOriginalItem());
        }
        return;
      }

      for (int i = 0; i < 3; i++){
        Block spawnOre = player.getWorld().getBlockAt(getOreLocation(player));
        if (replacedBlocksList.stream().noneMatch(data -> data.getBlock().equals(spawnOre))){
          replacedBlocksList.add(new ReplacedBlocksData(spawnOre,spawnOre.getType()));
        }
        spawnOre.setType(getOre(difficulty));
      }

      scoreBoard(player, nowPlayer, gameTimer);

    },0,5 * 20);
  }

  /**
   * スコアボードの表示
   * @param nowPlayer 現在実行しているプレイヤーのスコア情報
   * @param gameTimer ゲームの残り時間
   */
  private  void scoreBoard(org.bukkit.entity.Player player, Player nowPlayer, int[] gameTimer) {
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Scoreboard board = manager.getNewScoreboard();


    Objective objective = board.registerNewObjective("gameInfo","dummy",ChatColor.GOLD + "鉱石ゲーム");
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    Score nowTime = objective.getScore(ChatColor.GREEN + "残り : " + gameTimer[0] + "秒");
    nowTime.setScore(2);

    Score nowScore = objective.getScore(ChatColor.YELLOW + "スコア : " + nowPlayer.getScore() + "点");
    nowScore.setScore(1);

    player.setScoreboard(board);
  }

  /**
   * 同じ鉱石を連続で破壊した場合、コンボボーナスを加算します。
   * @param player 鉱石を破壊したプレイヤー
   * @param block 鉱石
   * @return
   */
  private GameData getGameData(org.bukkit.entity.Player player, Block block) {
    GameData data = playerGameDataMap.get(player.getName());
    Material brokenType = block.getType();

    if (brokenType.equals(data.getLastBlockOre())){
      data.setComboCount(data.getComboCount() + 1);
    }else {
      data.setComboCount(1);
      data.setLastBlockOre(brokenType);
    }
    return data;
  }

  /**
   * 鉱石の出現場所を取得します。
   *
   * @param player コマンドを実行したプレイヤー
   * @return ブロックの出現場所
   */

  private Location getOreLocation(org.bukkit.entity.Player player) {
    Location playerLocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(10) -5;
    int randomY = new SplittableRandom().nextInt(1) -2;
    int randomZ = new SplittableRandom().nextInt(10) -5;

    int blockX = playerLocation.getBlockX() + randomX;
    int blockY = playerLocation.getBlockY() + randomY;
    int blockZ = playerLocation.getBlockZ() + randomZ;

    return new Location(player.getWorld(), blockX , blockY ,blockZ );
  }


  /**
   * ランダムで鉱石を抽選して、その結果の鉱石を取得します。
   * @param difficulty 難易度
   * @return 鉱石
   */
  private Material getOre(GameDifficulty difficulty) {
   List<Material>ores = difficulty.getOreList();
    return ores.get(new SplittableRandom().nextInt(ores.size()));
  }

}
