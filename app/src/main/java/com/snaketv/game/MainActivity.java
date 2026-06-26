package com.snaketv.game;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.WindowManager;

/**
 * 主Activity
 * - 全屏横屏显示
 * - 捕获遥控器DPAD方向键 + OK键
 * - 使用Handler驱动游戏循环
 */
public class MainActivity extends Activity {

    private SnakeGame game;
    private SnakeView gameView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable gameLoop = this::tick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏，防止TV系统UI干扰
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        game = new SnakeGame();
        gameView = new SnakeView(this, game);
        setContentView(gameView);

        // 确保View可以接收焦点（遥控器输入）
        gameView.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 返回前台时暂停（防止玩家直接操作）
        if (game.state == SnakeGame.State.RUNNING) {
            game.state = SnakeGame.State.PAUSED;
            gameView.invalidate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopGameLoop();
    }

    /** 游戏主循环 Tick */
    private void tick() {
        if (game.state == SnakeGame.State.RUNNING) {
            game.tick();
            gameView.invalidate();
            if (game.state == SnakeGame.State.RUNNING) {
                // 根据当前速度安排下一帧
                handler.postDelayed(gameLoop, game.getIntervalMs());
            } else {
                // GAME_OVER — 刷新一次显示结束画面
                gameView.invalidate();
            }
        }
    }

    private void startGameLoop() {
        handler.removeCallbacks(gameLoop);
        handler.postDelayed(gameLoop, game.getIntervalMs());
    }

    private void stopGameLoop() {
        handler.removeCallbacks(gameLoop);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            // ── 方向键 ──
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                game.requestDirection(SnakeGame.Direction.UP);
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                game.requestDirection(SnakeGame.Direction.DOWN);
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                game.requestDirection(SnakeGame.Direction.LEFT);
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                game.requestDirection(SnakeGame.Direction.RIGHT);
                return true;

            // ── OK / 确认键 ──
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A: {
                if (game.state == SnakeGame.State.GAME_OVER) {
                    // 重新开始
                    game.reset();
                    game.state = SnakeGame.State.RUNNING;
                    startGameLoop();
                } else if (game.state == SnakeGame.State.PAUSED) {
                    game.state = SnakeGame.State.RUNNING;
                    startGameLoop();
                } else if (game.state == SnakeGame.State.RUNNING) {
                    // 暂停
                    stopGameLoop();
                    game.state = SnakeGame.State.PAUSED;
                }
                gameView.invalidate();
                return true;
            }

            // ── 返回键 ──
            case KeyEvent.KEYCODE_BACK:
                if (game.state == SnakeGame.State.RUNNING) {
                    // 先暂停，再按一次才退出
                    stopGameLoop();
                    game.state = SnakeGame.State.PAUSED;
                    gameView.invalidate();
                    return true;
                }
                // 已经暂停状态，允许正常返回退出
                return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }
}
