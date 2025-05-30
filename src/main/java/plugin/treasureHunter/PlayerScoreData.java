package plugin.treasureHunter;

import java.io.InputStream;
import java.util.List;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import plugin.treasureHunter.data.mapper.PlayerScoreMapper;
import plugin.treasureHunter.data.mapper.data.PlayerScore;

public class PlayerScoreData {

  private final SqlSessionFactory sqlSessionFactory;

  /**
   * DB接続やそれに付随する登録や更新処理を行うクラスです。
   */
  public PlayerScoreData() {
    try {
      InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
      this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * DBから一覧でスコア情報を取得する
   * @return スコア情報の一覧
   */
  public List<PlayerScore> selectList() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
      return mapper.selectList();
    }
  }

  /**
   * DBにスコア情報を登録する。
   * @param playerScore プレイヤースコア
   */
  public void insert(PlayerScore playerScore){
    try (SqlSession session = sqlSessionFactory.openSession(true)) {
      PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
      mapper.insert(playerScore);
    }
    }
  }
