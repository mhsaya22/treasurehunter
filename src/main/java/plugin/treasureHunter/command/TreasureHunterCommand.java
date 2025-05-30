package plugin.treasureHunter.command;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
import plugin.treasureHunter.PlayerScoreData;
import plugin.treasureHunter.data.GameData;
import plugin.treasureHunter.data.GameDifficulty;
import plugin.treasureHunter.data.ExecutingPlayer;
import plugin.treasureHunter.data.ReplacedBlocksData;
import plugin.treasureHunter.data.mapper.data.PlayerScore;

/**
 * 制限時間内にランダムで出現する鉱石ブロックを破壊して、スコア情報を獲得するゲームを起動するコマンドです。スコアは鉱石の種類によって変わり、破壊した鉱石の数によってスコアが変動します。
 * 結果はプレイヤー名、点数、日時などで保存されます。
 */

public class TreasureHunterCommand extends BaseCommand implements Listener {

  private final PlayerScoreData playerScoreData = new PlayerScoreData();
  private final Main main;
  private final List<ExecutingPlayer> executingPlayerList = new ArrayList<>();
  private final List<ReplacedBlocksData> replacedBlocksList = new ArrayList<>();
  private static final String LIST = "list";

  public TreasureHunterCommand(Main main) {
    this.main = main;
  }

  @Getter
  private final Map<String, GameData>  playerGameDataMap = new HashMap<>();

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label, String[] args) {


    if (args.length == 1 && LIST.equals(args[0])){
      sendPlayerScoreList(player);
      return false;
    }

   if (args.length != 1){
     player.sendMessage(ChatColor.RED + "実行できません。難易度の指定をしてください。[ easy / normal / hard ]");
     return false;
   }
    GameData gameData = playerGameDataMap.computeIfAbsent(player.getName(), k -> new GameData());
    gameData.setOriginalLocation(player.getLocation());
    initPlayerStatus(player);
    gamePlay(player, getPlayerScore(player), GameDifficulty.fromDifficultyString(args[0]),gameData);
    return true;
  }

  @Override
  public boolean onExecuteNPCPlayerCommand(CommandSender sender,Command command, String label, String[] args) {
    return false;
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e){
    //ブロックを壊したプレイヤー
    Player player = e.getPlayer();
    Block block = e.getBlock();

    if (replacedBlocksList.stream()
            .noneMatch(data -> data.getBlock().equals(block))) {
      replacedBlocksList.add(new ReplacedBlocksData(block));
      return;
    }

    executingPlayerList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {int point = switch (block.getType()) {
          case IRON_ORE -> 1;
          case GOLD_ORE, LAPIS_ORE, REDSTONE_ORE -> 20;
          case DIAMOND_ORE -> 50;
          case EMERALD_ORE -> 80;
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
   *現在登録されているスコアのお一覧をメッセージに送る
   * @param player プレイヤー
   */
  private void sendPlayerScoreList(Player player) {
    List<PlayerScore> playerScoreList = playerScoreData.selectList();
    for (PlayerScore playerScore : playerScoreList){
      player.sendMessage(playerScore.getId() + " | "
          + playerScore.getPlayerName() + " | "
          + playerScore.getScore() + " | "
          + playerScore.getDifficulty() + " | "
          + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
  }

  /**
   * 現在実行しているプレイヤーのスコア情報を取得する
   * @param player コマンドを実行したプレイヤー
   * @return 現在実行しているプレイヤーのスコア情報
   */
  private ExecutingPlayer getPlayerScore(Player player) {
    ExecutingPlayer executingPlayerScore = new ExecutingPlayer(player.getName());
    if (executingPlayerList.isEmpty()) {
     executingPlayerScore = addNewPlayer(player);
    } else {
      executingPlayerScore = executingPlayerList.stream().findFirst().map(ps
          -> ps.getPlayerName().equals(player.getName())
          ? ps
          : addNewPlayer(player)).orElse(executingPlayerScore);
    }
    executingPlayerScore.setScore(0);

    return executingPlayerScore;
  }

  /**
   * 新規のプレイヤー情報をリストに追加します。
   * @param player コマンドを実行したプレイヤー
   * @return 新規プレイヤー
   */
  private ExecutingPlayer addNewPlayer(Player player) {
    ExecutingPlayer newExecutingPlayer = new ExecutingPlayer(player.getName());
    executingPlayerList.add(newExecutingPlayer);
    return newExecutingPlayer;
  }

  /**
   * ゲームを始める前にプレイヤーの状態を設定する。
   * 採掘場にテレポートし、体力と空腹値を最大化して、装備はダイアモンドのピッケルになる。
   *
   * @param player コマンドを実行したプレイヤー
   */
  private void initPlayerStatus(Player player) {
    GameData gameData = playerGameDataMap.computeIfAbsent(player.getName(), k -> new GameData());
    gameData.setOriginalLocation(player.getLocation());

    ItemStack originalItem = player.getInventory().getItemInMainHand();

    player.setHealth(20);
    player.setFoodLevel(20);

    gameData.setOriginalItem(originalItem.getType() == Material.AIR
        ? new ItemStack(Material.AIR)
        : originalItem.clone());

    playerGameDataMap.put(player.getName(),gameData);
    player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));

    player.teleport(new Location(player.getWorld(), -259,11,-190));
    player.sendTitle(
        "採掘場にテレポートしました！", "装備したピッケルで鉱石を採掘し高得点を目指してください！" ,0 ,30 ,0);
  }

  /**
   * ゲームを実行します。
   * 制限時間内に鉱石を破壊するとスコアが加算されます。時間経過後に合計スコアを表示します。
   * @param player コマンドを実行したプレイヤー
   * @param nowExecutingPlayer プレイヤースコア情報
   */
  private void gamePlay(Player player, ExecutingPlayer nowExecutingPlayer,GameDifficulty difficulty,GameData gameData) {
    replacedBlocksList.clear();
    int[] gameTimer = {difficulty.getTimeLimit()};
    Bukkit.getScheduler().runTaskTimer(main,Runnable -> {
      gameTimer[0] -= 5 ;
      //Runnable.cancelの判断ロジック
      if (gameTimer[0] <= 0){
        Runnable.cancel();

        //スコアボードの非表示
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard emptyBord = manager.getNewScoreboard();
        player.setScoreboard(emptyBord);

        player.sendTitle("ゲームが終了しました！" , nowExecutingPlayer.getPlayerName() + " 合計は " + nowExecutingPlayer.getScore() + " 点！" ,
            0 , 60 , 0);

       //ブロックの復元
        replacedBlocksList.forEach(data ->
            data.getOriginalState().update(true,false));
        replacedBlocksList.clear();

        playerScoreData.insert(
            new PlayerScore(nowExecutingPlayer.getPlayerName(), nowExecutingPlayer.getScore()
            ,difficulty.getName()));

        //backCommandで元の位置へ
        player.sendMessage("元の位置に戻りますか？　戻る場合は　/back と入力してください ");

        player.getInventory().setItemInMainHand(gameData.getOriginalItem());
        return;
      }

      for (int i = 0; i < 3; i++){
        Block spawnOre = player.getWorld().getBlockAt(getOreLocation(player));
        if (replacedBlocksList.stream().noneMatch(data -> data.getBlock().equals(spawnOre))){
          replacedBlocksList.add(new ReplacedBlocksData(spawnOre));
        }
        spawnOre.setType(getOre(difficulty));
      }
      scoreBoard(player, nowExecutingPlayer, gameTimer);
    },0,5 * 20);
  }

  /**
   * スコアボードの表示
   * @param nowExecutingPlayer 現在実行しているプレイヤーのスコア情報
   * @param gameTimer ゲームの残り時間
   */
  private  void scoreBoard(Player player, ExecutingPlayer nowExecutingPlayer, int[] gameTimer) {
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Scoreboard board = manager.getNewScoreboard();


    Objective objective = board.registerNewObjective("gameInfo","dummy",ChatColor.GOLD + "鉱石ゲーム");
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    Score nowTime = objective.getScore(ChatColor.GREEN + "残り : " + gameTimer[0] + "秒");
    nowTime.setScore(2);

    Score nowScore = objective.getScore(ChatColor.YELLOW + "スコア : " + nowExecutingPlayer.getScore() + "点");
    nowScore.setScore(1);

    player.setScoreboard(board);
  }

  /**
   *プレイヤーの破壊したブロックの種類を記録し、コンボ状態を更新します。
   * 直前と同じ種類の鉱石であればコンボ数を増加させ、異なる場合はリセットされます。
   * @param player ブロックを破壊したプレイヤー
   * @param block プレイヤーが破壊したブロック
   * @return 更新後のGameData
   */
  private GameData getGameData(Player player, Block block) {
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
  private Location getOreLocation(Player player) {
    Location playerLocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(15) -5;
    int randomY = new SplittableRandom().nextInt(8) -2;
    int randomZ = new SplittableRandom().nextInt(15) -5;

    int blockX = playerLocation.getBlockX() + randomX;
    int blockY = playerLocation.getBlockY() + randomY;
    int blockZ = playerLocation.getBlockZ() + randomZ;

    Location targetLocation = new Location(player.getWorld(),blockX,blockY,blockZ);
    Block targetBlock = targetLocation.getBlock();
    Block below = player.getWorld().getBlockAt(blockX, blockY, blockZ);

    boolean hasAirAround = Arrays.stream(BlockFace.values())
        .filter(blockFace -> blockFace != BlockFace.SELF)
        .anyMatch(blockFace -> targetBlock.getRelative(blockFace).getType() == Material.AIR);
    if (!hasAirAround || targetBlock.getType().name().toLowerCase().contains("wood")) {
      return player.getLocation();
    }
    return targetLocation;
  }



  /**
   * 難易度によってランダムで鉱石を抽選し、その結果の鉱石を取得します。
   * @param difficulty 難易度
   * @return 鉱石
   */
  private Material getOre(GameDifficulty difficulty) {
   List<Material>ores = difficulty.getOreList();
    return ores.get(new SplittableRandom().nextInt(ores.size()));
  }

}
