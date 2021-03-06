package cn.flypigeon.springbootdemo.bombplane.service;

import cn.flypigeon.springbootdemo.bombplane.component.BombPlane;
import cn.flypigeon.springbootdemo.bombplane.component.Checkerboard;
import cn.flypigeon.springbootdemo.game.component.base.Player;
import cn.flypigeon.springbootdemo.bombplane.entity.BoomPoint;
import cn.flypigeon.springbootdemo.bombplane.entity.GameOver;
import cn.flypigeon.springbootdemo.game.exception.IllegalOperationException;
import cn.flypigeon.springbootdemo.game.server.Server;
import cn.flypigeon.springbootdemo.game.service.Service;
import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;

/**
 * 炸点
 * 1. 检查是否轮到此玩家
 * 2. 炸点
 * 3. 反馈给所有玩家被炸点的信息
 * Created by htf on 2020/10/23.
 */
public class BoomPointService extends Service {

    public BoomPointService(Service next) {
        super(next);
    }

    @Override
    protected int getCode() {
        return 5;
    }

    @Override
    protected void process0(Server server, JSONObject command) {
        BombPlane game = (BombPlane) server.getPlayer().getRoom().getGame();
        if (game.getStatus() == 1) {
            throw new IllegalOperationException("放置飞机阶段不能炸点");
        }
        if (game.getStatus() == 3) {
            throw new IllegalOperationException("游戏结束了");
        }
        Player player = game.currentPlayer();
        if (!server.getPlayer().equals(player)) {
            throw new IllegalOperationException("还没轮到你");
        }
        Integer x = command.getInteger("x");
        Integer y = command.getInteger("y");
        Checkerboard.CheckerPoint checkerPoint = game.anotherCheckerboard().attack(x, y);
        BoomPoint boomPoint = new BoomPoint();
        boomPoint.setX(x);
        boomPoint.setY(y);
        boomPoint.setType(checkerPoint.getType().code());
        boomPoint.setOwner(2);
        boomPoint.setEnd(game.isEnd());
        player.send(boomPoint);
        boomPoint.setOwner(1);
        player.getRoom().broadcast(boomPoint, player);
        if (boomPoint.isEnd()) {
            game.gameOver();
            GameOver gameOver = new GameOver();
            gameOver.setWinner(server.getPlayer().getId());
            Player[] players = game.getPlayers();
            for (Player p : players) {
                p.setReady(false);
            }
            Checkerboard[] checkerboards = game.getCheckerboards();
            gameOver.setPlanes(Arrays.asList(checkerboards[1].getPlanes()));
            players[0].send(gameOver);
            gameOver.setPlanes(Arrays.asList(checkerboards[0].getPlanes()));
            players[1].send(gameOver);
        }
        game.next();
    }
}
