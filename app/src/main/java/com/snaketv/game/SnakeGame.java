package com.snaketv.game;

import java.util.LinkedList;
import java.util.Random;

/**
 * 贪吃蛇核心逻辑（纯数据，不含View）
 * 坐标系：格子坐标，左上角为 (0,0)
 */
public class SnakeGame {

    public enum Direction { UP, DOWN, LEFT, RIGHT }
    public enum State { RUNNING, PAUSED, GAME_OVER }

    // 游戏格子大小
    public static final int COLS = 25;
    public static final int ROWS = 15;

    // 蛇身：LinkedList，头部为 index 0
    public final LinkedList<int[]> snake = new LinkedList<>();

    // 当前豆豆位置 [col, row]
    public int[] food = new int[2];

    public Direction direction = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT; // 下一帧方向（防止同帧多次转向）

    public int score = 0;
    public State state = State.PAUSED;

    private final Random rnd = new Random();

    // 每吃N个豆豆提速一次
    private static final int SPEED_UP_EVERY = 5;
    // 初始帧间隔（ms）
    public static final int INITIAL_INTERVAL_MS = 300;
    // 最小帧间隔（ms）
    public static final int MIN_INTERVAL_MS = 80;

    public SnakeGame() {
        reset();
    }

    public void reset() {
        snake.clear();
        score = 0;
        direction = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        state = State.PAUSED;

        // 初始蛇：3节，位于地图中央
        int startCol = COLS / 2;
        int startRow = ROWS / 2;
        snake.addLast(new int[]{startCol,     startRow});
        snake.addLast(new int[]{startCol - 1, startRow});
        snake.addLast(new int[]{startCol - 2, startRow});

        spawnFood();
    }

    /** 请求转向，忽略反向操作 */
    public void requestDirection(Direction d) {
        if (state != State.RUNNING) return;
        // 不允许反向
        if (d == Direction.UP    && direction == Direction.DOWN)  return;
        if (d == Direction.DOWN  && direction == Direction.UP)    return;
        if (d == Direction.LEFT  && direction == Direction.RIGHT) return;
        if (d == Direction.RIGHT && direction == Direction.LEFT)  return;
        nextDirection = d;
    }

    /** 每个游戏Tick调用一次，返回是否继续运行 */
    public boolean tick() {
        if (state != State.RUNNING) return false;

        direction = nextDirection;
        int[] head = snake.getFirst();
        int newCol = head[0];
        int newRow = head[1];

        switch (direction) {
            case UP:    newRow--; break;
            case DOWN:  newRow++; break;
            case LEFT:  newCol--; break;
            case RIGHT: newCol++; break;
        }

        // 撞墙检测
        if (newCol < 0 || newCol >= COLS || newRow < 0 || newRow >= ROWS) {
            state = State.GAME_OVER;
            return false;
        }

        // 撞自身检测
        for (int[] seg : snake) {
            if (seg[0] == newCol && seg[1] == newRow) {
                state = State.GAME_OVER;
                return false;
            }
        }

        // 移动：头部添加新格子
        snake.addFirst(new int[]{newCol, newRow});

        // 吃到豆豆
        if (newCol == food[0] && newRow == food[1]) {
            score++;
            spawnFood();
            // 不移除尾部 → 蛇变长
        } else {
            snake.removeLast(); // 移除尾部 → 长度不变
        }

        return true;
    }

    /** 计算当前帧间隔（随分数递减） */
    public int getIntervalMs() {
        int speedLevel = score / SPEED_UP_EVERY;
        int interval = INITIAL_INTERVAL_MS - speedLevel * 20;
        return Math.max(interval, MIN_INTERVAL_MS);
    }

    public void togglePause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
        } else if (state == State.PAUSED) {
            state = State.RUNNING;
        }
    }

    private void spawnFood() {
        // 随机生成豆豆，确保不在蛇身上
        int col, row;
        do {
            col = rnd.nextInt(COLS);
            row = rnd.nextInt(ROWS);
        } while (isOnSnake(col, row));
        food[0] = col;
        food[1] = row;
    }

    private boolean isOnSnake(int col, int row) {
        for (int[] seg : snake) {
            if (seg[0] == col && seg[1] == row) return true;
        }
        return false;
    }
}
