package plugin.treasureHunter.command;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
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
import plugin.treasureHunter.data.GameSession;
import plugin.treasureHunter.data.GameDifficulty;
import plugin.treasureHunter.data.ExecutingPlayer;
import plugin.treasureHunter.data.ReplacedBlocksData;
import plugin.treasureHunter.data.mapper.data.PlayerScore;

/**
 * 制限時間内にランダムで出現する鉱石ブロックを破壊して、スコアを競うゲームを起動するコマンドです。鉱石の種類によって変わり、破壊した鉱石の数によってスコアが変動します。
 * 結果はプレイヤー名、点数、日時などで保存されます。
 */

public class TreasureHunterCommand extends BaseCommand implements Listener {

  private final PlayerScoreData playerScoreData = new PlayerScoreData();
  private final Main main;
  private final List<ExecutingPlayer> executingPlayerList = new ArrayList<>();
  private static final String LIST = "list";
  private static final double MAX_HEALTH = 20.0;
  private static final int MAX_FOOD_LEVEL = 20;

  public TreasureHunterCommand(Main main) {
    this.main = main;
  }
  @Getter
  private final Map<String, GameSession>  playerGameDataMap = new HashMap<>();

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
    GameSession gameSession = playerGameDataMap.computeIfAbsent(player.getName(), k -> new GameSession());
    gameSession.setOriginalLocation(player.getLocation());
    initPlayerStatus(player);
    gamePlay(player, getPlayerScore(player), GameDifficulty.fromDifficultyString(args[0]),
        gameSession);
    return true;
  }

  @Override
  public boolean onExecuteNPCPlayerCommand(CommandSender sender,Command command, String label, String[] args) {
    return false;
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e){
    Player player = e.getPlayer();
    Block block = e.getBlock();
    GameSession session = getGameData(player,block);

    e.setDropItems(false);
    e.setExpToDrop(0);


//ブロックが復元リストに存在しない場合は追加のみ
    if (session.getReplacedBlocksList().stream()
            .noneMatch(data -> data.getBlock().equals(block))) {
      session.getReplacedBlocksList().add(new ReplacedBlocksData(block));
    }
    executingPlayerList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {int point = switch (block.getType()) {
          case IRON_ORE -> 1;
          case GOLD_ORE -> 10;
          case LAPIS_ORE, REDSTONE_ORE -> 20;
          case DIAMOND_ORE -> 50;
          case EMERALD_ORE -> 80;
          default -> 0;
        };
          if (point == 0){
            return;
          }
          p.setScore(p.getScore() + point + (session.getComboCount() -1)*2);
          if (session.getComboCount() > 1){
            player.sendMessage(ChatColor.YELLOW + "コンボ! × " + session.getComboCount());
          }
          player.sendMessage("鉱石を獲得！現在のスコアは" + p.getScore() + "点です！");
        });
  }

  /**
   *データベースに登録されたスコア情報を表示します。
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
   *ゲーム開始前にプレイヤーの初期状態を設定します。
   *採掘場にテレポートし、体力と空腹値を最大化、ダイアモンドのピッケルと松明64個を装備します。
   *また、ゲーム開始前のアイテムと位置情報を保存します。
   * @param player コマンドを実行したプレイヤー
   */
  private void initPlayerStatus(Player player) {
    GameSession gameSession = playerGameDataMap.computeIfAbsent(player.getName(), k -> new GameSession());
    gameSession.setOriginalLocation(player.getLocation());
    gameSession.setOriginalItem(player.getInventory().getContents().clone());

    player.getInventory().clear();
    player.getInventory().addItem(new ItemStack(Material.TORCH,64));
    player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_PICKAXE));

    player.setHealth(MAX_HEALTH);
    player.setFoodLevel(MAX_FOOD_LEVEL);

    playerGameDataMap.put(player.getName(), gameSession);

    player.teleport(new Location(player.getWorld(), -259,11,-190));
    player.sendTitle(
        "採掘場にテレポートしました！", "装備したピッケルで鉱石を採掘し高得点を目指してください！" ,0 ,30 ,0);
  }

  /**
   * ゲームを開始します。
   * 一定間隔で鉱石を出現させ、プレイヤーが破壊することでスコアが加算されます。
   * 制限時間経過後は、スコアの表示・スコアボードの非表示・ブロックの復元・スコア記録を行います。
   * @param player コマンドを実行したプレイヤー
   * @param nowExecutingPlayer 現在ゲームを実行しているプレイヤー
   * @param difficulty 難易度
   * @param gameSession ゲーム進行情報
   */
  private void gamePlay(Player player, ExecutingPlayer nowExecutingPlayer,GameDifficulty difficulty,
      GameSession gameSession) {
    gameSession.getReplacedBlocksList().clear();
    gameSession.startTimer(difficulty.getTimeLimit());
    Bukkit.getScheduler().runTaskTimer(main,Runnable -> {
      gameSession.reduceTime(5);
      //Runnable.cancelの判断ロジック
      if (gameSession.getTimeLeft() <= 0){
        Runnable.cancel();

        endGame(player, nowExecutingPlayer, difficulty,gameSession);

        player.getInventory().setContents(gameSession.getOriginalItem());
        return;
      }
      for (int i = 0; i < 3; i++){
        Block spawnOre = player.getWorld().getBlockAt(getOreLocation(player));
        if (gameSession.getReplacedBlocksList().stream().noneMatch(data -> data.getBlock().equals(spawnOre))){
          gameSession.getReplacedBlocksList().add(new ReplacedBlocksData(spawnOre));
        }
        spawnOre.setType(getOre(difficulty));
      }
      scoreBoard(player, nowExecutingPlayer, gameSession);
    },0,5 * 20);
  }

  /**
   * ゲーム中のスコアボードを表示します。
   * 残り時間と現在のスコアをサイドバーに表示し、プレイヤーに進行状況を示します。
   * @param player スコアボードを表示する対象プレイヤー
   * @param nowExecutingPlayer 現在ゲームを実行しているプレイヤー
   */
  private  void scoreBoard(Player player, ExecutingPlayer nowExecutingPlayer, GameSession gameSession) {
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Scoreboard board = manager.getNewScoreboard();

    Objective objective = board.registerNewObjective("gameInfo","dummy",ChatColor.GOLD + "鉱石ゲーム");
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    Score nowTime = objective.getScore(ChatColor.GREEN + "残り : " + gameSession.getTimeLeft()
        + "秒");
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
  private GameSession getGameData(Player player, Block block) {
    GameSession data = playerGameDataMap.get(player.getName());
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
   * プレイヤーの周囲にランダムな鉱石出現位置を計算して返します。
   * 木材ブロックや、空気ブロックのみの場所は除外されます。
   * @param player 出現位置の基準となるプレイヤー
   * @return 出現位置
   */
  private Location getOreLocation(Player player) {
    Location playerLocation = player.getLocation();
    SplittableRandom random = new SplittableRandom();

    for (int i = 0; i < 15; i++) {
      int blockX = random.nextInt(11) - 5;
      int blockY = random.nextInt(3) - 1;
      int blockZ = random.nextInt(11) - 5;

      int x = playerLocation.getBlockX() + blockX;
      int y = playerLocation.getBlockY() + blockY;
      int z = playerLocation.getBlockZ() + blockZ;

      Location targetLoc = new Location(player.getWorld(), x, y, z);
      Block targetBlock = targetLoc.getBlock();
      Block groundBlock = targetBlock.getRelative(BlockFace.DOWN);
      Block airBlock = targetBlock.getRelative(BlockFace.UP);

      if (targetBlock.getType().isSolid() && airBlock.getType().isAir()
          || targetBlock.getType().isAir() && groundBlock.getType().isSolid()) {
        return targetLoc;
      }
    }
    return playerLocation;
  }

  /**
   * 指定された難易度によって鉱石リストからランダムで鉱石を抽選し、その結果の鉱石を取得します。
   * @param difficulty 難易度
   * @return 鉱石
   */
  private Material getOre(GameDifficulty difficulty) {
   List<Material>ores = difficulty.getOreList();
    return ores.get(new SplittableRandom().nextInt(ores.size()));
  }

  /**
   * ゲーム終了時の処理を実行します。
   * スコアボードの非表示・復元対象ブロックの復元・スコアの保存・アイテムの復元
   * プレイヤーへ、ゲーム終了の案内メッセージの表示を行います。
   * @param player コマンドを実行したプレイヤー
   * @param nowExecutingPlayer  ゲームを実行していたプレイヤー
   * @param difficulty 難易度
   */
  private void endGame(Player player, ExecutingPlayer nowExecutingPlayer, GameDifficulty difficulty,GameSession gameSession) {
    Location safeLocation = (new Location(player.getWorld(), -259,11,-190));
    player.teleport(safeLocation);

    //スコアボードの非表示
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Scoreboard emptyBord = manager.getNewScoreboard();
    player.setScoreboard(emptyBord);

    player.sendTitle("ゲームが終了しました！" , nowExecutingPlayer.getPlayerName() + " 合計は " + nowExecutingPlayer.getScore() + " 点！" ,
        0 , 60 , 0);

    //ブロックの復元
    gameSession.getReplacedBlocksList().forEach(data -> {
      Location location = data.getBlock().getLocation();
      location.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5).stream()
          .filter(entity -> entity instanceof Item)
          .forEach(Entity::remove);
      data.getOriginalState().update(true, false);
    });
    gameSession.getReplacedBlocksList().clear();

    playerScoreData.insert(
        new PlayerScore(nowExecutingPlayer.getPlayerName(), nowExecutingPlayer.getScore()
            , difficulty.getName()));

    //backCommandで元の位置へ
    player.sendMessage("元の位置に戻りますか？　戻る場合は　/back と入力してください ");
  }

}
